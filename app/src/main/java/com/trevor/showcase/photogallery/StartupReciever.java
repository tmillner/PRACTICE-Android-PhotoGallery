package com.trevor.showcase.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by trevormillner on 2/27/18.
 */

public class StartupReciever extends BroadcastReceiver {

    private static final String TAG = StartupReciever.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received broadcast for intent: " + intent);

        boolean isAlarmOn = QueryPreferences.getPrefIsAlarmOn(context);
        PollService.setServiceAlarm(context, isAlarmOn);
    }
}
