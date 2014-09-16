package com.advantej.glass.glasstimote.tasks;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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

public class DataUpdateTask extends AsyncTask<String, String, String>
{
    private static final String TAG = "munky";
    private static final String IBEACON_DATA_URL = "http://mayfly.tmwtest.co.uk/glasstimote/android_connect/get_ibeacon_details.php";
    private static final String IBEACON_DATA_REQUEST_NAME = "ibeacon";
    private static final String IBEACON_DATA_REQUEST_METHOD = "GET";
    private static final String JSON_NODE_IMAGE_REQUEST = "image";
    private static final String JSON_NODE_LOCATION_NAME = "location_name";
    private static final String JSON_NODE_LOCATION_INFO = "location_info";
    private static final String JSON_NODE_SUCCESS = "success";

    private GlasstimoteService _glasstimoteService;
    private ArrayList<NameValuePair> _requestParamsList;
    private JSONParser _jsonParser = new JSONParser();
    private JSONObject _iBeaconData;

    private ServiceConnection _glasstimoteServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder)
        {
            _glasstimoteService = ((GlasstimoteService.GlassAppBinder)binder).getService();

            if (_glasstimoteService != null)
            {
                // run task...
                Log.i(TAG, "DataUpdateActivity connected to GlasstimoteService.");

                //_dataUpdateTask = new DataUpdateTask();
                //_dataUpdateTask.run(_glasstimoteService.getRequestParamsList)
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    public void run (ArrayList<NameValuePair> requestParamsList)
    {
        _requestParamsList = requestParamsList;
        this.execute();
    }

    /**
     * Getting iBeacon details in background thread
     * */
    protected String doInBackground(String...strings)
    {
        // Making the http request using the php layer
        final JSONObject json = _jsonParser.makeHttpRequest(IBEACON_DATA_URL, IBEACON_DATA_REQUEST_METHOD, _requestParamsList);

        try
        {
            int success = json.getInt(JSON_NODE_SUCCESS);

            if (success == 1)
            {
                JSONArray iBeaconsArray = json.getJSONArray(IBEACON_DATA_REQUEST_NAME);
                _iBeaconData = iBeaconsArray.getJSONObject(0);

                if (!isCancelled()) {
                    Log.i(TAG, "loading image: " + _iBeaconData.getString(JSON_NODE_IMAGE_REQUEST));

                    // Running separate Async task for image
                    ImageDownloader imageDownloader = new ImageDownloader();
                    imageDownloader.execute(_iBeaconData.getString(JSON_NODE_IMAGE_REQUEST));

                    //TextView locationName = (TextView) findViewById(R.id.location_name);

                    // Finding Fields
                    // txtName = (TextView) findViewById(R.id.name);

                    // Displaying
                    // txtName.setText(iBeaconData.getString("location_name"));
                }

            } else {
                Log.i(TAG, "ibeacon with minor not found");
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Task Completion
     * **/
    protected void onPostExecute(String file_url)
    {
        Log.i(TAG, "data task post execute handler.");
    }

    private class ImageDownloader extends AsyncTask<String, Void, Bitmap>
    {
        protected Bitmap doInBackground(String... URL)
        {
            String imageURL = URL[0];
            Bitmap bitmap = null;

            try
            {
                InputStream input = new java.net.URL(imageURL).openStream();
                bitmap = BitmapFactory.decodeStream(input);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return bitmap;
        }

        protected void onPostExecute(Bitmap loadedImage)
        {
            Log.i(TAG, "image task post execute handler.");

            // imgMain = (ImageView) findViewById(R.id.image);
            // imgMain.setImageBitmap(loadedImage);

            if (!isCancelled()) {
                try {
                    _glasstimoteService.showLiveCard(loadedImage, _iBeaconData.getString(JSON_NODE_LOCATION_NAME), _iBeaconData.getString(JSON_NODE_LOCATION_INFO));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}