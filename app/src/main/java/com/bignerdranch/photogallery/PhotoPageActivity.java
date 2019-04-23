package com.bignerdranch.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.webkit.WebView;

public class PhotoPageActivity extends SingleFragmentActivity {

    static Intent newIntent(Context context, Uri photoPageUri) {
        Intent intent = new Intent(context, PhotoPageActivity.class);
        intent.setData(photoPageUri);
        return intent;
    }

    @Override
    Fragment createFragment() {
        return PhotoPageFragment.newInstance(getIntent().getData());
    }

    @Override
    public void onBackPressed() {
        WebView webView = findViewById(R.id.web_view);
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
