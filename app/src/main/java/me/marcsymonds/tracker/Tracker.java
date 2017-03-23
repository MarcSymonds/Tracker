package me.marcsymonds.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.text.ParseException;

public class Tracker extends AppCompatActivity implements IMapFragmentActions, ITrackedItemActions {
    private final String TAG = "Tracker";

    private final int IBF_NONE = -1;
    private final int IBF_MY_LOCATION = 0;

    private final String SAVE_ITEM_BEING_FOLLOWED = "IBF";
    private final String SAVE_MY_LOCATION = "MYLOC";
    private final String SAVE_MAP_LATITUDE = "MAPLAT";
    private final String SAVE_MAP_LONGITUDE = "MAPLONG";
    private final String SAVE_MAP_ZOOM = "MAPZOOM";
    private final String SAVE_MAP_BEARING = "MAPBEARING";

    private MapFragment mMapFragment = null;
    private GoogleMap mMap = null;
    private Location mMyLastLocation = null;

    private CameraPosition mInitialCamera = null;

    private ImageButton mButMyLocation = null;
    private int mItemBeingFollowed = IBF_NONE; // -1=None, 0=My Location, n=Tracked Item ID.

    private boolean mInitialiseMapLocation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        Telephony.initialise(this);
        TrackedItemButtonHelper.initialise(this);
        TrackedItems.initialise(this);

        SMSSenderReceiver.setupBroadcastReceiver(this);
        SMSReceiver.setupBroadcastReceiver(this);

        if (savedInstanceState != null) {
            for (String key : savedInstanceState.keySet()) {
                Object o = savedInstanceState.get(key);
                if (o == null)
                    o = "NULL";

                Log.d(TAG, String.format("+++ %s=%s", key, o.toString()));
            }

            mItemBeingFollowed = savedInstanceState.getInt(SAVE_ITEM_BEING_FOLLOWED, IBF_NONE);

            String temp = savedInstanceState.getString(SAVE_MY_LOCATION, null);
            if (temp != null) {
                try {
                    mMyLastLocation = new Location(temp);
                }
                catch (ParseException pe) {
                    Log.e(TAG, String.format("Failed to parse saved current location: %s - %s", temp, pe.toString()));
                }
            }

            Float f = savedInstanceState.getFloat(SAVE_MAP_BEARING, -1);
            if (f >= 0) {
                mInitialCamera = new CameraPosition(
                        new LatLng(savedInstanceState.getDouble(SAVE_MAP_LATITUDE), savedInstanceState.getDouble(SAVE_MAP_LONGITUDE)),
                        savedInstanceState.getFloat(SAVE_MAP_ZOOM),
                        0,
                        f);
            }
        }

        LocalBroadcastManager eventListener = LocalBroadcastManager.getInstance(this);
        eventListener.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String event;
                String locationString = "";

                Log.d(TAG, String.format("Event: %s - %s", context.toString(), intent.toString()));

                event = intent.getStringExtra("EVENT");
                switch(event) {
                    case "TRACKED-ITEM-LOCATION-UPDATE":
                        try {
                            TrackedItem trackedItem = TrackedItems.getItemByID(intent.getIntExtra("TRACKED-ITEM", 0));
                            locationString = intent.getStringExtra("LOCATION");
                            Location loc = new Location(locationString);
                            trackedItemLocationUpdate(trackedItem, loc);
                        }
                        catch (ParseException pe) {
                            Log.e(TAG, String.format("Exception parsing Location %s - %s", locationString, pe.toString()));
                        }
                        break;
                }
            }
        },
        new IntentFilter("TRACKER-EVENT"));

        setContentView(R.layout.activity_tracker);

        Toolbar toolbar = (Toolbar) findViewById(R.id.appBarToolbar);
        setSupportActionBar(toolbar);

        mButMyLocation = (ImageButton) findViewById(R.id.butMyLocation);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mButMyLocation.setImageBitmap(TrackedItemButtonHelper.getMyLocationImage(mItemBeingFollowed == IBF_MY_LOCATION));
        drawTrackedItemButtons();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState ) {
        super.onSaveInstanceState(outState);

        Log.d(TAG, "onSaveInstanceState");

        outState.putInt(SAVE_ITEM_BEING_FOLLOWED, mItemBeingFollowed);

        if (mMyLastLocation != null) {
            outState.putString(SAVE_MY_LOCATION, mMyLastLocation.toString());
        }

        if (mMap != null) {
            CameraPosition cam = mMap.getCameraPosition();

            outState.putFloat(SAVE_MAP_BEARING, cam.bearing);
            outState.putFloat(SAVE_MAP_ZOOM, cam.zoom);
            outState.putDouble(SAVE_MAP_LATITUDE, cam.target.latitude);
            outState.putDouble(SAVE_MAP_LONGITUDE, cam.target.longitude);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "onPause");

        for (TrackedItem ti : TrackedItems.getTrackedItemsList()) {
            ti.saveHistory();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SMSReceiver.tearDown(this);
        SMSSenderReceiver.tearDown(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem option) {
        Intent intent;

        switch(option.getItemId()) {
            case R.id.mmAbout:
                AboutDialogFragment about = new AboutDialogFragment();
                about.show(getSupportFragmentManager(), "Here");
                break;

            case R.id.mmTrackedItems:
                intent = new Intent(this, TrackedItemListActivity.class);
                startActivityForResult(intent, ACTIVITY_REQUEST.MANAGE_TRACKED_ITEMS.getValue());
                break;
        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, String.format("onActivityResult - req:%d, res:%d", requestCode, resultCode));

        if (requestCode == ACTIVITY_REQUEST.MANAGE_TRACKED_ITEMS.getValue()) {
            for (TrackedItem trackedItem : TrackedItems.getTrackedItemsList()) {
                if (trackedItem.isEnabled()) {
                    if (!trackedItem.hasMapMarker()) {
                        trackedItem.createMapMarker(mMap);
                    } else {
                        trackedItem.updateMapMarkerInfo();
                    }
                }
                else {
                    trackedItem.removeMapMarker();
                }
            }

            drawTrackedItemButtons();
        }
    }

    @Override
    public void onMapReady(MapFragment mapFragment, GoogleMap map) {
        Location initialCameraLocation = null;

        mMapFragment = mapFragment;
        mMap = map;

        Log.d(TAG, "The map is ready. Adding markers for tracked items.");

        for (TrackedItem trackedItem : TrackedItems.getTrackedItemsList()) {
            if (trackedItem.isEnabled()) {
                trackedItem.createMapMarker(map);

                if (mItemBeingFollowed == trackedItem.getID()) {
                    initialCameraLocation = trackedItem.getLastLocation();
                    Log.d(TAG, String.format("Last position of tracked item %s: %s", trackedItem.getName(), trackedItem.getLastLocation().toString()));
                }
            }
        }

        if (mItemBeingFollowed == IBF_MY_LOCATION && mMyLastLocation != null) {
            initialCameraLocation = mMyLastLocation;
            Log.d(TAG, String.format("Last position of my location: %s", mMyLastLocation.toString()));
        }

        if (initialCameraLocation != null) {
            if (mInitialCamera == null) {
                mInitialCamera = new CameraPosition(initialCameraLocation.getLatLng(), 16, 0, 0); // LatLng, Zoom, Tilt, Bearing.
            }
            //else {
                //mInitialCamera = new CameraPosition(initialCameraLocation.getLatLng(), mInitialCamera.zoom, mInitialCamera.tilt, mInitialCamera.bearing);
            //}
        }

        if (mInitialCamera != null) {
            CameraUpdate cu = CameraUpdateFactory.newCameraPosition(mInitialCamera);
            mMap.moveCamera(cu);
        }
        else {
            mInitialiseMapLocation = true; // When we get the first location message, move the map to that location.
        }
    }

    @Override
    public void updateMyLocation(Location location) {
        mMyLastLocation = location;

        if (mItemBeingFollowed == IBF_MY_LOCATION || mInitialiseMapLocation) {
            mMapFragment.centerMap(location, mInitialiseMapLocation ? 16 : -1);
            mInitialiseMapLocation = false;
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        // Extract the ID of the object to be acted on from the request ID.
        int id = requestCode >> 4;
        TrackedItem trackedItem;
        requestCode &= 7;

        switch (Tracker.PERMISSION_REQUEST.valueOf(requestCode)) {
            case SEND_SMS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    trackedItem = TrackedItems.getItemByID(id);
                    if (trackedItem != null) {
                        trackedItem.sendPing(this);
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(), "Failed to acquire permission to send SMS message.", Toast.LENGTH_LONG).show();
                    return;
                }
                break;

            case READ_SMS:
            case RECEIVE_SMS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SMSReceiver.setupBroadcastReceiver(this);
                }
                break;
        }
    }

    private void drawTrackedItemButtons() {
        boolean foundFollowedItem = false;

        LinearLayout container = (LinearLayout)findViewById(R.id.layTrackedItemButtons);

        container.removeAllViews();

        for (TrackedItem ti : TrackedItems.getTrackedItemsList()) {
            if (ti.isEnabled()) {
                View buttonView = ti.getButton(this).getButtonView();
                container.addView(buttonView);

            }
            else {
                ti.removeMapMarker();
            }

            if (mItemBeingFollowed == ti.getID()) {
                if (ti.isEnabled()) {
                    ti.setFollowingButtonState(true);
                }
                else {
                    ti.setFollowingButtonState(false);
                    mItemBeingFollowed = IBF_NONE;
                }

                foundFollowedItem = true;
            }

        }

        if (!foundFollowedItem && mItemBeingFollowed != IBF_MY_LOCATION) {
            mItemBeingFollowed = IBF_NONE;
        }
    }

    public void myLocationButtonClick(View view) {
        setItemBeingFollowed(IBF_MY_LOCATION);
    }

    @Override
    public void trackedItemButtonClick(TrackedItem trackedItem, boolean longClick) {
        if (longClick) {
            trackedItem.sendPing(this);
        }
        else {
            setItemBeingFollowed(trackedItem.getID());
        }
    }

    private void trackedItemLocationUpdate(TrackedItem trackedItem, Location location) {
        Log.d(TAG, String.format("Tracked item location changed - %s (%d/%d): %s", trackedItem.getName(), trackedItem.getID(), mItemBeingFollowed, location.toString()));

        if (trackedItem.hasMapMarker()) {
            trackedItem.updateMapMarkerLocation(location);
        }
        else {
            trackedItem.createMapMarker(mMap, location);
        }

        if (trackedItem.getID() == mItemBeingFollowed) {
            mMapFragment.centerMap(location);
        }
    }

    private void setItemBeingFollowed(int itemBeingFollowed) {
        TrackedItem trackedItem;

        // Unhilite the current tracked item.
        if (mItemBeingFollowed == IBF_MY_LOCATION) {
            mButMyLocation.setImageBitmap(TrackedItemButtonHelper.getMyLocationImage(false));
        }
        else if (mItemBeingFollowed > 0) {
            trackedItem = TrackedItems.getItemByID(mItemBeingFollowed);
            if (trackedItem != null) {
                trackedItem.setFollowingButtonState(false);
            }
        }

        if (mItemBeingFollowed == itemBeingFollowed) {
            mItemBeingFollowed = IBF_NONE;
        }
        else {
            mItemBeingFollowed = itemBeingFollowed;

            if (mItemBeingFollowed == IBF_MY_LOCATION) {
                mButMyLocation.setImageBitmap(TrackedItemButtonHelper.getMyLocationImage(true));
                mMapFragment.centerMap(mMyLastLocation);
            }
            else if (mItemBeingFollowed > 0) {
                trackedItem = TrackedItems.getItemByID(mItemBeingFollowed);
                if (trackedItem != null) {
                    trackedItem.setFollowingButtonState(true);
                    mMapFragment.centerMap(trackedItem.getLastLocation());
                }
            }
        }
    }

    enum PERMISSION_REQUEST {
        UNKNOWN(0),
        LOCATION(1),
        SEND_SMS(2),
        RECEIVE_SMS(3),
        READ_SMS(4);

        private final int mValue;

        PERMISSION_REQUEST(int value) {
            mValue = value;
        }

        static PERMISSION_REQUEST valueOf(int v) {
            for (PERMISSION_REQUEST p : PERMISSION_REQUEST.values()) {
                if (p.getValue() == v) {
                    return p;
                }
            }

            return UNKNOWN;
        }

        int getValue() {
            return mValue;
        }
    }

    enum ACTIVITY_REQUEST {
        UNKNOWN(0),
        MANAGE_TRACKED_ITEMS(1);

        private final int mValue;

        ACTIVITY_REQUEST(int value) {
            mValue = value;
        }

        int getValue() {
            return mValue;
        }
    }
}
