package com.bignerdranch.photogallery;

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

class ThumbnailDownloader<T> extends HandlerThread {

    private static final String LOG_TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private final ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();

    private final Handler mMainThreadHandler;

    interface Listener<T> {
        void onThumbnailDownloaded(T target, Bitmap bitmap);
    }

    private Listener<T> mListener;

    void setListener(Listener<T> listener) {
        mListener = listener;
    }

    ThumbnailDownloader(Handler mainThreadHandler) {
        super(LOG_TAG);
        mMainThreadHandler = mainThreadHandler;
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    void queueThumbnail(T target, String url) {
        Log.i(LOG_TAG, "Got a URL: " + url);

        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    @SuppressWarnings("unchecked")
                    T target = (T) msg.obj;
                    Log.i(LOG_TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }

    private void handleRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);
            if (url == null) return;

            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(LOG_TAG, "Bitmap created");

            mMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    String storedUrl = mRequestMap.get(target);
                    if (storedUrl == null || !storedUrl.equals(url) || mHasQuit) {
                        return;
                    }

                    mRequestMap.remove(target);
                    mListener.onThumbnailDownloaded(target, bitmap);
                }
            });
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error downloading image", e);
        }
    }
}
