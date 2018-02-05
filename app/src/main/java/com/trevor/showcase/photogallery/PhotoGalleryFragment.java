package com.trevor.showcase.photogallery;


import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PhotoGalleryFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private List<GalleryItem> mGalleryItems = new ArrayList<>();

    private final String TAG = PhotoGalleryFragment.class.getName();

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    public PhotoGalleryFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemsTask().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_photo_gallery, container, false);
        mRecyclerView = view.findViewById(R.id.photo_gallery_recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        setupAdapter();

        return view;
    }

    private void setupAdapter() {
        if(isAdded()) {
            mRecyclerView.setAdapter(new PhotoAdapter(mGalleryItems));
        }
    }

    public class PhotoHolder extends RecyclerView.ViewHolder {

        private TextView mTitleTextView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mTitleTextView = (TextView) itemView;
        }

        public void bindGalleryItem(GalleryItem item) {
            mTitleTextView.setText(item.toString());
        }
    }

    public class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> items) {
            mGalleryItems = items;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(getActivity());
            return new PhotoHolder(textView);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            holder.bindGalleryItem(galleryItem);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Void,Void, List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {

            /** Test if main get Raw response works
            try {
                String result = new FlickrFetcher().getURLString("https://www.bignerdranch.com");
                Log.i(TAG, "Fetched result: " + result);
            } catch (IOException e) {
                Log.e(TAG, "Failed to fetch result: " + e);
                e.printStackTrace();
            }
             **/
            return new FlickrFetcher().fetchRecentItems();
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mGalleryItems = items;
            setupAdapter();
        }
    }
}
