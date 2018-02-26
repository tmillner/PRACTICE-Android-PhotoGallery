package com.trevor.showcase.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by trevormillner on 1/24/18.
 */

public class FlickrFetcher {

    private static final String TAG = FlickrFetcher.class.getName();

    private static final String API_KEY = "7da8cff0093d73af52a9f7c652562a7e";

    private static final String RECENT_PHOTOS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_PHOTOS_METHOD = "flickr.photos.search";

    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    public byte[] getURLBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        // This allows us a clean interface to operate on HTTP resources
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // At this point connects to the endpoint
            InputStream inputStream = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " + urlSpec);
            }

            // Read in the bytes from in, into the out ByteArrayOutputStream
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            return outputStream.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getURLString(String urlSpec)  throws IOException  {
        return new String(getURLBytes(urlSpec));
    }

    public List<GalleryItem> downloadGalleryItems(String url) {

        List<GalleryItem> items = new ArrayList<>();

        try {
           String jsonResponse = getURLString(url);
           Log.i(TAG, "Fetch flickr recent items result: " + jsonResponse);
           JSONObject jsonObject = new JSONObject(jsonResponse);
           parseRecentItems(items, jsonObject);
        } catch (IOException e) {
            Log.e(TAG, "Failed to Fetch flickr recent items: " + e);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse json for Fetch flickr recent items: " + e);
        }

        return items;
    }

    public List<GalleryItem> fetchRecentItems() {
        String url =  buildUrl(RECENT_PHOTOS_METHOD, null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query) {
        String url = buildUrl(SEARCH_PHOTOS_METHOD, query);
        return downloadGalleryItems(url);
    }

    private String buildUrl(String method, String query) {
       Uri.Builder uriBuilder = ENDPOINT.buildUpon()
               .appendQueryParameter("method", method);

       if (method == SEARCH_PHOTOS_METHOD) {
           uriBuilder.appendQueryParameter("text", query);
       }

       return uriBuilder.toString();
    }

    public void parseRecentItems(List<GalleryItem> items, JSONObject jsonObject)
            throws JSONException{
        JSONObject photosObject = jsonObject.getJSONObject("photos");
        JSONArray photosArray = photosObject.getJSONArray("photo");

        for (int i = 0; i < photosArray.length(); i++) {
            JSONObject photo = photosArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setCaption(photo.getString("id"));
            item.setCaption(photo.getString("title"));

            if(!photo.has("url_s")) {
                continue;
            }

            item.setUrl(photo.getString("url_s"));
            items.add(item);
        }
    }
}
