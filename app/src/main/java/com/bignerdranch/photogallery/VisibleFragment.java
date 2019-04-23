package com.bignerdranch.photogallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;

abstract class VisibleFragment extends Fragment {

    private static final String LOG_TAG = "VisibleFragment";

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
        assert getActivity() != null;
        // SOS: note that we have to specify the permission for this receiver, unlike receivers
        // declared in the manifest which implicitly use the permission that the app uses
        getActivity().registerReceiver(mOnShowNotification, filter, PollService.PERM_PRIVATE, null);
    }

    @Override
    public void onStop() {
        super.onStop();
        assert getActivity() != null;
        getActivity().unregisterReceiver(mOnShowNotification);
    }

    // SOS: This receiver will only be registered when the app is visible, therefore in those cases it
    // will receive the broadcast before NotificationManager and it will set the result to CANCELLED.
    // NotificationReceiver will see this result and not send the notification.
    private final BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(LOG_TAG, "Cancelling notification");
            setResultCode(Activity.RESULT_CANCELED);
        }
    };
}
