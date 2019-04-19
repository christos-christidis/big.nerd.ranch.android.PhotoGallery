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

// SOS: Holy shit. The exact same work is done in 4 lines using a 3rd party library called Picasso
// (see p542). If I want to display GIFs -> Google's Glide or Facebook's Fresco library
class ThumbnailDownloader<T> extends HandlerThread {

    private static final String LOG_TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    // SOS: this is a thread-safe implementation of hash map
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();

    // SOS: this will hold the main (UI) thread's handler so I can return the bitmap to him. Remember,
    // only the main thread can update the UI
    private Handler mMainThreadHandler;

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

    // SOS: T will be something to identify our messages. The most clever choice is to use the
    // PhotoHolder on which the resulting image will be placed!
    void queueThumbnail(T target, String url) {
        Log.i(LOG_TAG, "Got a URL: " + url);

        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            // SOS: obtainMessage associates the msg w mRequestHandler. Note that the msg itself does
            // not contain the url. We'll get the url from the map later when the handler actually
            // handles the msg, so that we get the newest url associated with this photoholder (remember
            // that holders are recycled and reused).
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }

    // SOS: called right before the 1st time looper checks the message-queue
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

    // SOS: if the user rotates the screen, this thread may be hanging on to invalid photoholders.
    // Bad things will happen when the user clicks again on the new ImageViews.
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
                    // SOS: by the time this runs on the main thread, recyclerview may have recycled
                    // the photoholder and requested a different url for it! Moreover, this thread
                    // may have quit for some reason...
                    if (!mRequestMap.get(target).equals(url) || mHasQuit) {
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
