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
    private static final double BEACON_REGION_ENTRY_DISTANCE = 1.5;
    private static final double BEACON_REGION_EXIT_DISTANCE = 2.5;
    private static final Region CREATIVE_BEACON_REGION = new Region("creative", null, TMW_BEACONS_MAJOR, CREATIVE_BEACON_MINOR);
    private static final Region KITCHEN_BEACON_REGION = new Region("kitchen", null, TMW_BEACONS_MAJOR, KITCHEN_BEACON_MINOR);
    private static final Region TECH_BEACON_REGION = new Region("tech", null, TMW_BEACONS_MAJOR, TECH_BEACON_MINOR);
    
    private LiveCard _beaconsLiveCard;
    private RemoteViews _beaconsLiveCardView;
    private Beacon _kitchenBeacon;
    private Beacon _creativeBeacon;
    private Beacon _techBeacon;

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
        _beaconManager.setRangingListener(mRangingListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //publishOrUpdateLiveCard(0);
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

    private BeaconManager.RangingListener mRangingListener = new BeaconManager.RangingListener() {
        @Override
        public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {

            _beacons = beacons;

            int count = beacons.size();

            if (count > 0) {

                // publishOrUpdateLiveCard(count);

                checkBeaconPositions();

            } else if (count == 0) {
                unpublishLiveCard();
            }
        }
    };

    private void checkBeaconPositions() {

        // loop through beacons.
        for (Beacon beacon : _beacons) {

            // ensure these are the TMW estimotes.
            if (beacon.getMajor() == TMW_BEACONS_MAJOR) {

                int beaconMinor = beacon.getMinor();

                // check which estimote this is and set variables accordingly.
                if (beaconMinor == CREATIVE_BEACON_MINOR) {

                    _creativeBeacon = beacon;
                }
                else if (beaconMinor == KITCHEN_BEACON_MINOR) {

                    _kitchenBeacon = beacon;
                }
                else if (beaconMinor == TECH_BEACON_MINOR) {

                    _techBeacon = beacon;
                }
            }
        }

        if (_creativeBeacon != null) {

            // get the distance between the beacon and the device.
            double creativeBeaconDistance = Utils.computeAccuracy(_creativeBeacon);

            // if we are close to the beacon, and not already in a state inside the beacon's range...
            if (creativeBeaconDistance < BEACON_REGION_ENTRY_DISTANCE) {

                if (_creativeRegionState == Region.State.OUTSIDE) {

                    // switch to a state inside the range of this beacon.
                    _creativeRegionState = Region.State.INSIDE;

                    // stop looking for the other beacons, as we are in range of this one.
                    try {
                        _beaconManager.stopRanging(KITCHEN_BEACON_REGION);
                        _beaconManager.stopRanging(TECH_BEACON_REGION);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    showLiveCard("creative");

                    // return early to stop other beacons getting mixed up.
                    return;
                }
            } else if (creativeBeaconDistance >= BEACON_REGION_EXIT_DISTANCE) {

                if (_creativeRegionState == Region.State.INSIDE) {

                    // switch to a state outside the range of this beacon.
                    _creativeRegionState = Region.State.OUTSIDE;

                    Log.i(TAG, "hiding creative live card text...");
                    // unpublishLiveCard();
                    updateCardLocationText(getString(R.string.discovering));


                    // restart looking for the other beacons, as we are no longer in range of this one.
                    try {
                        _beaconManager.startRanging(KITCHEN_BEACON_REGION);
                        _beaconManager.startRanging(TECH_BEACON_REGION);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (_kitchenBeacon != null) {

            // get the distance between the beacon and the device.
            double kitchenBeaconDistance = Utils.computeAccuracy(_kitchenBeacon);

            // if we are close to the beacon, and not already in a state inside the beacon's range...
            if (kitchenBeaconDistance < BEACON_REGION_ENTRY_DISTANCE) {

                if (_kitchenRegionState == Region.State.OUTSIDE) {

                    // switch to a state inside the range of this beacon.
                    _kitchenRegionState = Region.State.INSIDE;

                    // stop looking for the other beacons, as we are in range of this one.
                    try {
                        _beaconManager.stopRanging(CREATIVE_BEACON_REGION);
                        _beaconManager.stopRanging(TECH_BEACON_REGION);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    showLiveCard("kitchen");

                    // return early to stop other beacons getting mixed up.
                    return;
                }

            } else if (kitchenBeaconDistance >= BEACON_REGION_EXIT_DISTANCE) {

                if (_kitchenRegionState == Region.State.INSIDE) {

                    // switch to a state outside the range of this beacon.
                    _kitchenRegionState = Region.State.OUTSIDE;

                    Log.i(TAG, "left the kitchen location...");
                    updateCardLocationText(getString(R.string.discovering));

                    // restart looking for the other beacons, as we are no longer in range of this one.
                    try {
                        _beaconManager.startRanging(CREATIVE_BEACON_REGION);
                        _beaconManager.startRanging(TECH_BEACON_REGION);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (_techBeacon != null) {
        
            // get the distance between the beacon and the device.
            double techBeaconDistance = Utils.computeAccuracy(_techBeacon);

            // if we are close to the beacon, and not already in a state inside the beacon's range...
            if (techBeaconDistance < BEACON_REGION_ENTRY_DISTANCE) {

                if (_techRegionState == Region.State.OUTSIDE) {

                    // switch to a state inside the range of this beacon.
                    _techRegionState = Region.State.INSIDE;

                    // stop looking for the other beacons, as we are in range of this one.
                    try {
                        _beaconManager.stopRanging(CREATIVE_BEACON_REGION);
                        _beaconManager.stopRanging(KITCHEN_BEACON_REGION);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    showLiveCard("tech");

                    // return early to stop other beacons getting mixed up.
                    // currently commented out since this is the last statement within the containing method.
                    // return;
                }

            } else if (techBeaconDistance >= BEACON_REGION_EXIT_DISTANCE) {

                if (_techRegionState == Region.State.INSIDE) {
                    // switch to a state outside the range of this beacon.
                    _techRegionState = Region.State.OUTSIDE;

                    // unpublishLiveCard();
                    updateCardLocationText(getString(R.string.discovering));

                    Log.i(TAG, "left the tech location...");

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

    private void showLiveCard (String beaconName) {

        Log.i(TAG, "showing live card: " + beaconName);

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

        _beaconsLiveCardView.setTextViewText(R.id.location_name, beaconName);
        _beaconsLiveCard.setViews(_beaconsLiveCardView);
    }

    private void updateCardLocationText (String locationText) {

        if (_beaconsLiveCard != null) {

            _beaconsLiveCardView.setTextViewText(R.id.location_name, locationText);
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

    public List<Beacon> getBeacons() {
        return _beacons;
    }
}
