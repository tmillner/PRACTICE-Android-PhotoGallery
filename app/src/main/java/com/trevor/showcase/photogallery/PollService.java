package com.trevor.showcase.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

/**
 * Created by trevormillner on 2/25/18.
 */

public class PollService extends IntentService {

    private static final String TAG = PollService.class.getName();
    public static final String ACTION_SHOW_NOTIFICATION =
            "com.trevor.showcase.photogallery.SHOW_NOTIFICATION";

    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";

    public static final String PERM_PRIVATE = "com.trevor.showcase.photogallery.PRIVATE";

    private static final int POLL_INTERVAL = 30 * 1000; // 30s
    // private static final long POLL_INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES;

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    public PollService() {
        super(TAG);
    }

    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent i = newIntent(context);
        // This pending intent will trigger the message.send() thus invoking
        // onHandleIntent eventually (all done behind the scenes)
        // alarm manager knows how to do this
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, i, 0);

        AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(ALARM_SERVICE);

        if(isOn) {
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(), POLL_INTERVAL, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }

        QueryPreferences.setPrefIsAlarmOn(context, isOn);
    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent i = newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, i,
                PendingIntent.FLAG_NO_CREATE);
        return pendingIntent != null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(!isNetworkAvailableAndConnected()) return;

        Log.i(TAG, "Handling intent (Doing nothing): " + intent);

        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getPrefLastResultId(this);

        List<GalleryItem> galleryItemList = null;
        if (query == null) {
            galleryItemList = new FlickrFetcher().fetchRecentItems();
        } else{
            galleryItemList = new FlickrFetcher().searchPhotos(query);
        }
        if(galleryItemList.size() == 0) return;

        String firstGalleryItemId = galleryItemList.get(0).getId();
        if(firstGalleryItemId == lastResultId) {
            Log.d(TAG, "Current most recent gallery item matches old: " + lastResultId);
        } else {
            Log.d(TAG, "Current most recent gallery item DOES NOT matches old " + firstGalleryItemId);

            Resources resources = getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();

            /* This could exist without broadcast recievers as such:
            But since we use brodcast recievers to with orderedBroadcast, need to
            run in static Broadcast Receiver (else this it service would die before full execution)

            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(this);
            notificationManager.notify(0, notification);
            */
            showBackgroundNotification(0, notification);

            // See VisibleFragment for FG broadcast receiver
            sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION), PERM_PRIVATE);
        }
        QueryPreferences.setPrefLastResultId(this, firstGalleryItemId);

    }

    private void showBackgroundNotification(int requestCode, Notification notification) {
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE, requestCode);
        i.putExtra(NOTIFICATION, notification);
        // result receiver typically is used but since this could die early
        // outsourced to static Broadcast receiver
        sendOrderedBroadcast(i, PERM_PRIVATE, null, null,
                Activity.RESULT_OK, null, null);
    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable &&
                cm.getActiveNetworkInfo().isConnected();
        return isNetworkConnected;
    }
}
