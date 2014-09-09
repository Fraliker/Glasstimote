package com.advantej.glass.glasstimote;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.google.android.glass.timeline.LiveCard;

import java.util.List;

public class GlasstimoteService extends Service {


    private static final String TAG = "munky";
    private static final String LIVE_CARD_TAG = "BEACONS_CARD";
    private static final int TMW_BEACONS_MAJOR = 200;
    private static final int CREATIVE_BEACON_MINOR = 1;
    private static final int KITCHEN_BEACON_MINOR = 2;
    private static final int TECH_BEACON_MINOR = 3;
    private static final double BEACON_REGION_ENTRY_DISTANCE = 0.4;
    private static final double BEACON_REGION_EXIT_DISTANCE = 0.8;
    private static final Region CREATIVE_BEACON_REGION = new Region("creative", null, TMW_BEACONS_MAJOR, CREATIVE_BEACON_MINOR);
    private static final Region KITCHEN_BEACON_REGION = new Region("kitchen", null, TMW_BEACONS_MAJOR, KITCHEN_BEACON_MINOR);
    private static final Region TECH_BEACON_REGION = new Region("tech", null, TMW_BEACONS_MAJOR, TECH_BEACON_MINOR);
    
    private LiveCard _beaconsLiveCard;
    private RemoteViews _beaconsLiveCardView;

    private Region.State _creativeRegionState = Region.State.OUTSIDE;
    private Region.State _kitchenRegionState = Region.State.OUTSIDE;
    private Region.State _techRegionState = Region.State.OUTSIDE;

    private BeaconManager _beaconManager;
    private List<Beacon> _beacons = null;
    private final IBinder _binder = new GlassAppBinder();

    public class GlassAppBinder extends Binder {
        public GlasstimoteService getService() {
            return GlasstimoteService.this;
        }
    }

    public GlasstimoteService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _beaconManager = new BeaconManager(this);
        _beaconManager.setRangingListener(_rangingListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        showLiveCard(R.drawable.discovering_icon, getString(R.string.discovering_title), getString(R.string.discovering_info));
        checkBTStatusAndStartRanging();
        return START_STICKY;
    }

    private void checkBTStatusAndStartRanging() {

        // Check if device supports Bluetooth Low Energy.
        if (!_beaconManager.hasBluetooth()) {
            Toast.makeText(this, getString(R.string.error_bluetooth_le_unsupported), Toast.LENGTH_LONG).show();
            return;
        }

        // If Bluetooth is not enabled, let user enable it.
        if (!_beaconManager.isBluetoothEnabled()) {
            Toast.makeText(this, getString(R.string.error_bluetooth_not_enabled), Toast.LENGTH_LONG).show();

            // currently commented out since this is the last statement within the containing method.
            //return;
        } else {
            connectToService();
        }
    }

    private void connectToService() {
        _beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    _beaconManager.startRanging(CREATIVE_BEACON_REGION);
                    _beaconManager.startRanging(KITCHEN_BEACON_REGION);
                    _beaconManager.startRanging(TECH_BEACON_REGION);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        _beaconManager.disconnect();
        unpublishLiveCard();
        super.onDestroy();
    }

    private BeaconManager.RangingListener _rangingListener = new BeaconManager.RangingListener() {
        @Override
        public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {

            _beacons = beacons;

            if (beacons.size() > 0) {

                checkBeaconPositions();
            }
        }
    };

    private void checkBeaconPositions() {

        // loop through beacons.
        for (Beacon beacon : _beacons) {

            // ensure these are the TMW estimotes.
            if (beacon.getMajor() != TMW_BEACONS_MAJOR) continue;

            // get the distance between the beacon and the device.
            double beaconDistance = Utils.computeAccuracy(beacon);
            int beaconMinor = beacon.getMinor();


            if (beaconDistance < BEACON_REGION_ENTRY_DISTANCE) {

                // if we are close to a specific beacon, and not already in
                // a state inside the beacon's region range...
                if (beaconMinor == CREATIVE_BEACON_MINOR && _creativeRegionState == Region.State.OUTSIDE) {

                    Log.i(TAG, "entering creative... distance: " + beaconDistance);

                    // stop looking for the other beacons, as we are in range of this one.
                    try {
                        _beaconManager.stopRanging(KITCHEN_BEACON_REGION);
                        _beaconManager.stopRanging(TECH_BEACON_REGION);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    // switch to a state inside the range of this beacon.
                    _creativeRegionState = Region.State.INSIDE;
                    showLiveCard(R.drawable.creative_location_icon, getString(R.string.creative_location_title), getString(R.string.creative_location_info));

                } else if (beaconMinor == KITCHEN_BEACON_MINOR && _kitchenRegionState == Region.State.OUTSIDE) {

                    Log.i(TAG, "entering kitchen... distance: " + beaconDistance);

                    // stop looking for the other beacons, as we are in range of this one.
                    try {
                        _beaconManager.stopRanging(CREATIVE_BEACON_REGION);
                        _beaconManager.stopRanging(TECH_BEACON_REGION);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    // switch to a state inside the range of this beacon.
                    _kitchenRegionState = Region.State.INSIDE;
                    showLiveCard(R.drawable.kitchen_location_icon, getString(R.string.kitchen_location_title), getString(R.string.kitchen_location_info));

                } else if (beaconMinor == TECH_BEACON_MINOR && _techRegionState == Region.State.OUTSIDE) {

                    Log.i(TAG, "entering tech... distance: " + beaconDistance);

                    // stop looking for the other beacons, as we are in range of this one.
                    try {
                        _beaconManager.stopRanging(CREATIVE_BEACON_REGION);
                        _beaconManager.stopRanging(KITCHEN_BEACON_REGION);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    // switch to a state inside the range of this beacon.
                    _techRegionState = Region.State.INSIDE;
                    showLiveCard(R.drawable.tech_location_icon, getString(R.string.tech_location_title), getString(R.string.tech_location_info));
                }
            } else if (beaconDistance >= BEACON_REGION_EXIT_DISTANCE) {

                // if we are far enough away from a specific beacon region,
                // and not already in a state outside the beacon's region range...
                if (beaconMinor == CREATIVE_BEACON_MINOR && _creativeRegionState == Region.State.INSIDE) {

                    Log.i(TAG, "exiting creative... distance: " + beaconDistance);

                    // switch to a state outside the range of this beacon.
                    _creativeRegionState = Region.State.OUTSIDE;
                    showLiveCard(R.drawable.discovering_icon, getString(R.string.discovering_title), getString(R.string.discovering_info));

                    // restart looking for the other beacons, as we are no longer in range of this one.
                    try {
                        _beaconManager.startRanging(KITCHEN_BEACON_REGION);
                        _beaconManager.startRanging(TECH_BEACON_REGION);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                } else if (beaconMinor == KITCHEN_BEACON_MINOR && _kitchenRegionState == Region.State.INSIDE) {

                    Log.i(TAG, "exiting kitchen... distance: " + beaconDistance);

                    // switch to a state outside the range of this beacon.
                    _kitchenRegionState = Region.State.OUTSIDE;
                    showLiveCard(R.drawable.discovering_icon, getString(R.string.discovering_title), getString(R.string.discovering_info));

                    // restart looking for the other beacons, as we are no longer in range of this one.
                    try {
                        _beaconManager.startRanging(CREATIVE_BEACON_REGION);
                        _beaconManager.startRanging(TECH_BEACON_REGION);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                } else if (beaconMinor == TECH_BEACON_MINOR && _techRegionState == Region.State.INSIDE) {

                    Log.i(TAG, "exiting tech... distance: " + beaconDistance);

                    // switch to a state outside the range of this beacon.
                    _techRegionState = Region.State.OUTSIDE;
                    showLiveCard(R.drawable.discovering_icon, getString(R.string.discovering_title), getString(R.string.discovering_info));

                    // restart looking for the other beacons, as we are no longer in range of this one.
                    try {
                        _beaconManager.startRanging(CREATIVE_BEACON_REGION);
                        _beaconManager.startRanging(KITCHEN_BEACON_REGION);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void showLiveCard (int locationImage, String locationName, String locationInfo) {

        Log.i(TAG, "showing live card: " + locationName);

        if (_beaconsLiveCard == null) {
            // create a new live card.
            _beaconsLiveCard = new LiveCard(this, LIVE_CARD_TAG);

            _beaconsLiveCardView = new RemoteViews(getPackageName(), R.layout.beacons_live_card);

            Intent menuIntent = new Intent(this, LiveCardMenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            _beaconsLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

            //Publish card
            _beaconsLiveCard.publish(LiveCard.PublishMode.REVEAL);
        }

        updateCardLocation(locationImage, locationName, locationInfo);
    }

    private void updateCardLocation(int locationImage, String locationTitle, String locationInfo) {

        if (_beaconsLiveCard != null) {

            _beaconsLiveCardView.setTextViewCompoundDrawables(R.id.location_name, locationImage, 0, 0, 0);
            _beaconsLiveCardView.setTextViewText(R.id.location_name, locationTitle);
            _beaconsLiveCardView.setTextViewText(R.id.location_info, locationInfo);
            _beaconsLiveCard.setViews(_beaconsLiveCardView);
        }
    }

    private void unpublishLiveCard() {
        if (_beaconsLiveCard != null && _beaconsLiveCard.isPublished()) {
            _beaconsLiveCard.unpublish();
            _beaconsLiveCard = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }
}
