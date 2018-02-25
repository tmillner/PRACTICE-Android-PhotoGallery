package com.trevor.showcase.photogallery;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by trevormillner on 2/4/18.
 */

public class ThumbnailDownloader <T> extends HandlerThread {
    private final static String TAG = ThumbnailDownloader.class.getName();

    private final static int MESSAGE_DOWNLOAD = 0;

    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();;
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    // Using this listener allows delegation of responsibility of what to do
    //   to a class other than this HandlerThread
    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for url: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a url: " + url);

        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            // The message itself does not contain url, it is better here
            //   to pass in the source object (ViewHolder
            // Get a new message and pass to handler (sendToTarget). The handler
            // (mRequestHandler) will put the msg in the Loopers message queue.
            // And then handler will execute this message via Handler.handleMessage
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                    .sendToTarget();
        }
    }

    // Helper method
    private void handleRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);

            if (url == null) return;

            byte[] bitmapBytes = new FlickrFetcher().getURLBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    // This is necessary because the viewHolder may have been updated
                    // with another URL (scroll), so cancel the old one and always use the most
                    // recent Where the url is a match for the target ViewHolder
                    if (mRequestMap.get(target) != url) {
                        return;
                    }

                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error downloading image:" + e);
        }
    }
}
