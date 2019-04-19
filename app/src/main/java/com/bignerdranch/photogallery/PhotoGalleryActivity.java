package com.bignerdranch.photogallery;

import android.support.v4.app.Fragment;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    @Override
    Fragment createFragment() {
        return PhotoGalleryFragment.newInstance();
    }
}
