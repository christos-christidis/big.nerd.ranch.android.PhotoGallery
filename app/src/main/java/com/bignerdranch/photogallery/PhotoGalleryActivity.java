package com.bignerdranch.photogallery;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    // SOS: will be used to start activity when we click on the notification posted by the service,
    // ie if I've exited the app, when I click on the notification, the app will open again.
    static Intent newIntent(Context context) {
        return new Intent(context, PhotoGalleryActivity.class);
    }

    @Override
    Fragment createFragment() {
        return PhotoGalleryFragment.newInstance();
    }
}
