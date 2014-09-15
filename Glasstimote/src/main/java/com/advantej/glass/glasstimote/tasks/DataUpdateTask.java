package com.advantej.glass.glasstimote.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.advantej.glass.glasstimote.utils.JSONParser;

import com.advantej.glass.glasstimote.tasks.ImageDownloader;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Background Async Task to Get complete iBeacon data
 * */
public class DataUpdateTask extends AsyncTask<String, String, String> {

    private static final String TAG = "munky";

    private static final String IBEACON_DATA_URL = "http://mayfly.tmwtest.co.uk/glasstimote/android_connect/get_ibeacon_details.php";
    private static final String IBEACON_DATA_REQUEST_NAME = "ibeacon";
    private static final String IBEACON_DATA_REQUEST_METHOD = "GET";
    private static final String JSON_NODE_IMAGE_REQUEST = "image";
    private static final String JSON_NODE_SUCCESS = "success";

    private WeakReference<Activity> _mainActivityWeakReference;
    private ArrayList<NameValuePair> _requestParamsList;
    private JSONParser _jsonParser = new JSONParser();
    private ImageDownloader _imageDownloader = new ImageDownloader();

    public void DataUpdateTask (Activity mainActivity)
    {
        _mainActivityWeakReference = new WeakReference<Activity>(mainActivity);
    }

    public void start (ArrayList<NameValuePair> requestParamsList)
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
        final Activity mainActivity = _mainActivityWeakReference.get();

        if (mainActivity != null) {

            // updating UI from Background Thread
            mainActivity.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        int success = json.getInt(JSON_NODE_SUCCESS);

                        if (success == 1) {

                            JSONArray iBeaconsArray = json.getJSONArray(IBEACON_DATA_REQUEST_NAME);
                            JSONObject iBeaconData = iBeaconsArray.getJSONObject(0);

                            // Running separate Async task for image
                            _imageDownloader.execute(iBeaconData.getString(JSON_NODE_IMAGE_REQUEST));

                            /*
                            // Finding Fields
                            txtName = (TextView) findViewById(R.id.name);
                            txtColour = (TextView) findViewById(R.id.colour);
                            txtAddress = (TextView) findViewById(R.id.address);
                            txtMajor = (TextView) findViewById(R.id.major);
                            txtMinor = (TextView) findViewById(R.id.minor);

                            // Displaying
                            txtName.setText(iBeaconData.getString(NAME));
                            txtColour.setText(iBeaconData.getString(COLOUR));
                            txtAddress.setText(iBeaconData.getString(ADDRESS));
                            txtMajor.setText(iBeaconData.getString(MAJOR));
                            txtMinor.setText(iBeaconData.getString(REQUEST_STRING_MINOR));
                            */
                        } else {
                            Log.i(TAG, "ibeacon with minor not found");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        return null;
    }

    /**
     * Task Completion
     * **/
    protected void onPostExecute(String file_url) {
        // dismiss the dialog once got all details
    }
}