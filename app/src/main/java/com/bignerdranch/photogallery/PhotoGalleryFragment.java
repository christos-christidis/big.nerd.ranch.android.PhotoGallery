package com.bignerdranch.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();

    static Fragment newInstance() {
        return new PhotoGalleryFragment();
    }

    // SOS: we retain the fragment for simple task handling (otherwise we'd have to decide what to do
    // w the task when the fragment is destroyed & recreated). Alternatively, we could store a handle
    // to the task and call cancel on it in onDestroy; calling cancel(false) will set a flag that can
    // be checked inside doInBackground w isCancelled() to exit prematurely, whereas cancel(true)
    // kills the task immediately. Read p520 on how to publish the progress of the task. Also read
    // p521 for why AsyncTaskLoader is a better choice to AsyncTask (deals w a lot of problems!)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemsTask().execute();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = view.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        setUpAdapter();

        return view;
    }

    private void setUpAdapter() {
        // SOS: check that fragment is attached to activity so that getActivity() != null. Before this
        // app, we only had fragment methods called by the framework, which meant that there was def
        // an activity there (no activity = no callbacks). Now however, this method can be called
        // from our background thread, so we must make sure!!!
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {

        private final TextView mTitleTextView;

        PhotoHolder(@NonNull View itemView) {
            super(itemView);
            mTitleTextView = (TextView) itemView;
        }

        void bind(GalleryItem item) {
            mTitleTextView.setText(item.toString());
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private final List<GalleryItem> mGalleryItems;

        PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @NonNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            TextView textView = new TextView(getActivity());
            return new PhotoHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            photoHolder.bind(galleryItem);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            return new FlickrFetchr().fetchItems();
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            setUpAdapter();
        }
    }
}
