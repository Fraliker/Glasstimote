package com.advantej.glass.glasstimote.tasks;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;


/**
 * Background Async Task to get ibeacon image
 */
public class ImageDownloader extends AsyncTask<String, Void, Bitmap> {

    protected Bitmap doInBackground(String... URL) {
        String imageURL = URL[0];
        Bitmap bitmap = null;

        try {
            InputStream input = new java.net.URL(imageURL).openStream();
            bitmap = BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    protected void onPostExecute(Bitmap loadedImage) {
        // imgMain = (ImageView) findViewById(R.id.image);
        // imgMain.setImageBitmap(loadedImage);
    }
}