package com.bignerdranch.photogallery;

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
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

// SOS: Like an AsyncTask, IntentService stops its thread when there are no more intents to process.
public class PollService extends IntentService {

    private static final String LOG_TAG = "PollService";

    private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(15);

    private static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    // SOS: the alarm restarts PollService every 15 minutes, even though app may be in background.
    // For battery reasons, the lowest rate I can set is 1 minute. Moreover, alarms are not exact,
    // ie if 10 apps decide to set their alarm every 1 minute, Android will move around the times a
    // bit so that it wakes up fewer times and serves many apps at once. If I need exact alarms, I
    // can use alarm.setWindow or alarm.setExact. Note: if the phone is sleeping (screen turned off),
    // the alarms WON'T actually wake up the device, for that use ELAPSED_REALTIME_WAKEUP.
    static void setServiceAlarm(Context context, boolean isOn) {
        Intent intent = PollService.newIntent(context);
        // SOS: essentially getService says I want this intent to be sent w startService(intent)
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (isOn) {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME,
                    // SOS: this actually means start now (now = elapsedRealTime since boot)
                    SystemClock.elapsedRealtime(),
                    POLL_INTERVAL_MS, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    static boolean isServiceAlarmOn(Context context) {
        //  SOS: Passing the intent to getService w FLAG_NO_CREATE means 'give me the existing
        // pendingIntent'. If it's null due to called cancel above, we'll know that the alarm is off
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
        // SOS: user may have disabled internet for background applications
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
        Resources resources = getResources();
        Intent i = PhotoGalleryActivity.newIntent(this);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, i, 0);

        // SOS: clicking on this will open PhotoGalleryActivity
        Notification notification = new NotificationCompat.Builder(this, "SOME_CHANNEL")
                .setTicker(resources.getString(R.string.new_pictures_title))
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle(resources.getString(R.string.new_pictures_title))
                .setContentText(resources.getString(R.string.new_pictures_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        // SOS: if I notify again w same id, the new notification replaces the old. This is how dynamic
        // stuff in notifications, eg progress bars, is implemented
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(0, notification);
    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        return isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();
    }
}
