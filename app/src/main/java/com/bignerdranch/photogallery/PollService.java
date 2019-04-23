package com.bignerdranch.photogallery;

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
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PollService extends IntentService {

    private static final String LOG_TAG = "PollService";

    private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(15);

    static final String ACTION_SHOW_NOTIFICATION = "action_show_notification";
    static final String PERM_PRIVATE = "com.bignerdranch.photogallery.PRIVATE";
    static final String EXTRA_NOTIFICATION_CODE = "extra_notification_code";
    static final String EXTRA_NOTIFICATION = "extra_notification";

    private static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    static void setServiceAlarm(Context context, boolean isOn) {
        Intent intent = PollService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (isOn) {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(),
                    POLL_INTERVAL_MS, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }

        QueryPreferences.setAlarmOn(context, isOn);
    }

    static boolean isServiceAlarmOn(Context context) {
        Intent intent = PollService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context,
                0, intent, PendingIntent.FLAG_NO_CREATE);

        return pendingIntent != null;
    }

    public PollService() {
        super(LOG_TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!isNetworkAvailableAndConnected()) {
            return;
        }

        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);

        List<GalleryItem> items;
        if (query == null) {
            items = new FlickrFetchr().fetchRecentPhotos();
        } else {
            items = new FlickrFetchr().searchPhotos(query);
        }

        if (items.isEmpty()) {
            return;
        }

        String resultId = items.get(0).getId();
        if (resultId.equals(lastResultId)) {
            Log.i(LOG_TAG, "Got an old result: " + resultId);
        } else {
            Log.i(LOG_TAG, "Got a new result: " + resultId);
            sendNotification();
        }

        QueryPreferences.setLastResultId(this, resultId);
    }

    private void sendNotification() {
        Notification notification = buildNotification();

        Intent broadcastIntent = new Intent(ACTION_SHOW_NOTIFICATION);
        broadcastIntent.putExtra(EXTRA_NOTIFICATION_CODE, 0);
        broadcastIntent.putExtra(EXTRA_NOTIFICATION, notification);
        sendOrderedBroadcast(broadcastIntent, PERM_PRIVATE, null, null,
                Activity.RESULT_OK, null, null);
    }

    private Notification buildNotification() {
        Resources resources = getResources();
        Intent intent = PhotoGalleryActivity.newIntent(this);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        return new NotificationCompat.Builder(this, "SOME_CHANNEL")
                .setTicker(resources.getString(R.string.new_pictures_title))
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle(resources.getString(R.string.new_pictures_title))
                .setContentText(resources.getString(R.string.new_pictures_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        return isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();
    }
}
