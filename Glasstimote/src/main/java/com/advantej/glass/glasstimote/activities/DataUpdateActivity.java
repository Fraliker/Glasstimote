package com.advantej.glass.glasstimote.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.advantej.glass.glasstimote.services.GlasstimoteService;
import com.advantej.glass.glasstimote.utils.JSONParser;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by ihayes on 16/09/2014.
 */
public class DataUpdateActivity extends Activity
{
    private static final String TAG = "munky";

    private GlasstimoteService _glasstimoteService;
    private DataUpdateTask _dataUpdateTask;


    private ServiceConnection _glasstimoteServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {

            _glasstimoteService = ((GlasstimoteService.GlassAppBinder)binder).getService();

            if (_glasstimoteService != null) {

                // run task...
                Log.i(TAG, "DataUpdateActivity connected to GlasstimoteService.");

                //_dataUpdateTask = new DataUpdateTask();
                //_dataUpdateTask.run(_glasstimoteService.getRequestParamsList)
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, GlasstimoteService.class), _glasstimoteServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(_glasstimoteServiceConnection);
    }


    private class DataUpdateTask extends AsyncTask<String, String, String> {

        private static final String IBEACON_DATA_URL = "http://mayfly.tmwtest.co.uk/glasstimote/android_connect/get_ibeacon_details.php";
        private static final String IBEACON_DATA_REQUEST_NAME = "ibeacon";
        private static final String IBEACON_DATA_REQUEST_METHOD = "GET";
        private static final String JSON_NODE_IMAGE_REQUEST = "image";
        private static final String JSON_NODE_SUCCESS = "success";

        private ArrayList<NameValuePair> _requestParamsList;
        private JSONParser _jsonParser = new JSONParser();
        private ImageDownloader _imageDownloader = new ImageDownloader();

        public void run (ArrayList<NameValuePair> requestParamsList)
        {
            _requestParamsList = requestParamsList;
            this.execute();
        }

        /**
         * Getting iBeacon details in background thread
         * */
        protected String doInBackground(String...strings) {

            // Making the http request using the php layer
            final JSONObject json = _jsonParser.makeHttpRequest(IBEACON_DATA_URL, IBEACON_DATA_REQUEST_METHOD, _requestParamsList);

            try {
                int success = json.getInt(JSON_NODE_SUCCESS);

                if (success == 1) {

                    JSONArray iBeaconsArray = json.getJSONArray(IBEACON_DATA_REQUEST_NAME);
                    JSONObject iBeaconData = iBeaconsArray.getJSONObject(0);

                    // Running separate Async task for image
                    _imageDownloader.execute(iBeaconData.getString(JSON_NODE_IMAGE_REQUEST));

                    //TextView locationName = (TextView) findViewById(R.id.location_name);

                    // Finding Fields
                    // txtName = (TextView) findViewById(R.id.name);

                    // Displaying
                    // txtName.setText(iBeaconData.getString(NAME));

                } else {
                    Log.i(TAG, "ibeacon with minor not found");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * Task Completion
         * **/
        protected void onPostExecute(String file_url) {
            // dismiss the dialog once got all details
        }
        private class ImageDownloader extends AsyncTask<String, Void, Bitmap> {

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

                //_glasstimoteService
            }
        }

    }
}
