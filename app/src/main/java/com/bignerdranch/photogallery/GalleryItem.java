package com.bignerdranch.photogallery;

import android.net.Uri;

class GalleryItem {

    private String mCaption;
    private String mId;
    private String mUrl;

    private String mOwner;

    void setCaption(String caption) {
        mCaption = caption;
    }

    String getId() {
        return mId;
    }

    void setId(String id) {
        mId = id;
    }

    String getUrl() {
        return mUrl;
    }

    void setUrl(String url) {
        mUrl = url;
    }

    void setOwner(String owner) {
        mOwner = owner;
    }

    Uri getPhotoPageUri() {
        return Uri.parse("https://www.flickr.com/photos/")
                .buildUpon()
                .appendPath(mOwner)
                .appendPath(mId)
                .build();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        return mCaption;
    }
}
