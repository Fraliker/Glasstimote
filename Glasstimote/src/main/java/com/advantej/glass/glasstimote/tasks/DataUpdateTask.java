package com.advantej.glass.glasstimote.tasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

import com.advantej.glass.glasstimote.model.vo.LocationDataVO;
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
    private static final String JSON_NODE_IMAGE_REQUEST = "location_image_url";
    private static final String JSON_NODE_LOCATION_NAME = "location_name";
    private static final String JSON_NODE_LOCATION_INFO = "location_info";
    private static final String JSON_NODE_SUCCESS = "success";

    private ArrayList<NameValuePair> _requestParamsList;
    private JSONParser _jsonParser = new JSONParser();
    private LocationDataVO _locationDataVO = new LocationDataVO();
    private Message _dataUpdatedMessage;

    public void run (ArrayList<NameValuePair> requestParamsList, Message dataUpdatedMessage)
    {
        Log.i(TAG, "DataUpdateTask:: [run]");

        _dataUpdatedMessage = dataUpdatedMessage;
        _requestParamsList = requestParamsList;
        this.execute();
    }

    /**
     * Getting iBeacon details in background thread
     * */
    protected String doInBackground(String...strings)
    {
        // Making the http request using the php layer
        // TODO : add try/catch, with catch defining UnknownHostException.
        final JSONObject json = _jsonParser.makeHttpRequest(IBEACON_DATA_URL, IBEACON_DATA_REQUEST_METHOD, _requestParamsList);


        try
        {
            int success = json.getInt(JSON_NODE_SUCCESS);

            if (success == 1)
            {
                JSONArray iBeaconsArray = json.getJSONArray(IBEACON_DATA_REQUEST_NAME);
                JSONObject iBeaconData = iBeaconsArray.getJSONObject(0);

                if (!isCancelled()) {

                    Log.i(TAG, "location name: " + iBeaconData.getString(JSON_NODE_LOCATION_NAME));
                    Log.i(TAG, "location info: " + iBeaconData.getString(JSON_NODE_LOCATION_INFO));
                    Log.i(TAG, "location image: " + iBeaconData.getString(JSON_NODE_IMAGE_REQUEST));

                    _locationDataVO.locationName = iBeaconData.getString(JSON_NODE_LOCATION_NAME);
                    _locationDataVO.locationInfo = iBeaconData.getString(JSON_NODE_LOCATION_INFO);

                    // Run a separate Async task for downloading the image.
                    ImageDownloader imageDownloader = new ImageDownloader();
                    imageDownloader.execute(iBeaconData.getString(JSON_NODE_IMAGE_REQUEST));
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
        protected Bitmap doInBackground(String... imageURLArray)
        {
            String imageURL = imageURLArray[0];
            Bitmap bitmap = null;

            try
            {
                Log.i(TAG, "ImageDownloader:: [doInBackground] is cancelled? " + isCancelled());
                if (!isCancelled())
                {
                    InputStream input = new java.net.URL(imageURL).openStream();
                    bitmap = BitmapFactory.decodeStream(input);
                }
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

            if (!isCancelled())
            {
                _locationDataVO.locationImage = loadedImage;

                _dataUpdatedMessage.obj = _locationDataVO;
                _dataUpdatedMessage.sendToTarget();
            }
        }
    }

}