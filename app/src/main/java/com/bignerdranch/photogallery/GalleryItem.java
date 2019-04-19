package com.bignerdranch.photogallery;

class GalleryItem {

    private String mCaption;
    private String mId;
    private String mUrl;

    String getCaption() {
        return mCaption;
    }

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

    @Override
    public String toString() {
        return mCaption;
    }
}
