package me.marcsymonds.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.text.ParseException;

public class Tracker extends AppCompatActivity implements IMapFragmentActions, ITrackedItemActions, IHistoryUploaderController {
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

    private CameraPosition mInitialCamera = null;

    private ImageButton mButMyLocation = null;
    private int mItemBeingFollowed = IBF_NONE; // -1=None, 0=My Location, n=Tracked Item ID.

    private boolean mSetInitialMapLocation = false;
    private boolean mMarkersAdded = false;
    private boolean mKeepScreenOn = false;

    private HistoryUploader mHistoryUploader = null;

    private MyLocation mMyLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        Telephony.initialise(this);
        TrackedItemButtonHelper.initialise(this);
        TrackedItems.initialise(this.getApplicationContext());

        SMSSenderReceiver.setupBroadcastReceiver(this);
        //SMSReceiver.setupBroadcastReceiver(this);

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
                    MyLocation.getInstance().setLastLocation(new Location(temp));
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
                            int id = intent.getIntExtra("TRACKED-ITEM", 0);
                            TrackedItem trackedItem = TrackedItems.getItemByID(id);
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

        BackgroundService.runIfNotStarted(getApplicationContext());
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart");

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BackgroundLocationUpdateManager.EVENT_STOP_LOCATION_UPDATES));

        mButMyLocation.setImageBitmap(TrackedItemButtonHelper.getMyLocationImage(mItemBeingFollowed == IBF_MY_LOCATION));
        drawTrackedItemButtons();

        // Markers may not be added at this point if the map is not ready yet.
        // When the map becomes ready, the markers will be added then.
        addTrackedItemMarkers();

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mKeepScreenOn = Pref.getKeepScreenOn(this);
        if (mKeepScreenOn) {
            keepScreenOn(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState ) {
        super.onSaveInstanceState(outState);

        Log.d(TAG, "onSaveInstanceState");

        outState.putInt(SAVE_ITEM_BEING_FOLLOWED, mItemBeingFollowed);

        Location lastLocation = MyLocation.getInstance().getLastLocation();
        if (lastLocation != null) {
            outState.putString(SAVE_MY_LOCATION, lastLocation.toString());
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

        Pref.setKeepScreenOn(this, mKeepScreenOn);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

        if (mHistoryUploader != null) {
            mHistoryUploader.stop();
        }

        removeTrackedItemButtonsAndMarkers();
        mMarkersAdded = false;

        LocalBroadcastManager
                .getInstance(getApplicationContext())
                .sendBroadcast(
                        new Intent(BackgroundLocationUpdateManager.EVENT_START_LOCATION_UPDATES));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy");
        //SMSReceiver.tearDown(this);
        SMSSenderReceiver.tearDown(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // Turn on the icons in the overflow menu. Cos, why not!
        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }

        return true;
    }

    /**
     * Prepare the Screen's standard options menu to be displayed.  This is
     * called right before the menu is shown, every time it is shown.  You can
     * use this method to efficiently enable/disable items or otherwise
     * dynamically modify the contents.
     * <p>
     * <p>The default implementation updates the system menu items based on the
     * activity's state.  Deriving classes should always call through to the
     * base class implementation.
     *
     * @param menu The options menu as last shown or first initialized by
     *             onCreateOptionsMenu().
     * @return You must return true for the menu to be displayed;
     * if you return false it will not be shown.
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Set the state and icon of the Keep Screen On menu option.
        MenuItem mi = menu.findItem(R.id.main_menu_keep_screen_on);
        if (mi != null) {
            mi.setChecked(mKeepScreenOn);
            mi.setIcon(mKeepScreenOn ? R.drawable.ic_phone_awake_android_black_24px : R.drawable.ic_phone_sleep_android_black_24px);
        }

        return super.onPrepareOptionsMenu(menu);
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

            case R.id.mmSettings:
                intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, ACTIVITY_REQUEST.UNKNOWN.getValue());
                break;

            case R.id.main_menu_upload_history_now:
                if (mHistoryUploader == null || mHistoryUploader.isCompleted()) {
                    mHistoryUploader = new HistoryUploader(this);
                    String[] files = HistoryRecorder.getInstance(getApplicationContext()).getHistoryManager().getListOfFilesForUpload();
                    mHistoryUploader.execute(files);
                }
                break;

            case R.id.main_menu_keep_screen_on:
                mKeepScreenOn = !mKeepScreenOn;
                option.setChecked(mKeepScreenOn);
                keepScreenOn(mKeepScreenOn);
                break;
        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, String.format("onActivityResult - req:%d, res:%d", requestCode, resultCode));
    }

    @Override
    public void onMapReady(MapFragment mapFragment, GoogleMap map) {
        Location initialCameraLocation = null;

        mMapFragment = mapFragment;
        mMap = map;

        Log.d(TAG, "The map is ready. Adding markers for tracked items.");

        initialCameraLocation = addTrackedItemMarkers();

        Location lastLocation = MyLocation.getInstance().getLastLocation();

        if (mItemBeingFollowed == IBF_MY_LOCATION && lastLocation != null) {
            initialCameraLocation = lastLocation;
            Log.d(TAG, String.format("Last position of my location: %s", lastLocation.toString()));
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
            mSetInitialMapLocation = true; // When we get the first location message, move the map to that location.
        }
    }

    @Override
    public void updateMyLocation(Location location) {
        MyLocation.getInstance().recordLocation(this.getApplicationContext(), location);

        if (mItemBeingFollowed == IBF_MY_LOCATION || mSetInitialMapLocation) {
            mMapFragment.centerMap(location, mSetInitialMapLocation ? 16 : -1);
            mSetInitialMapLocation = false;
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        PermissionChecker.RequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void drawTrackedItemButtons() {
        boolean foundFollowedItem = false;

        Log.d(TAG, "Drawing buttons");

        LinearLayout container = (LinearLayout)findViewById(R.id.layTrackedItemButtons);

        container.removeAllViews();

        for (TrackedItem ti : TrackedItems.getTrackedItemsList()) {
            if (ti.isEnabled()) {
                View buttonView = ti.getButton(this, true).getButtonContainerView();
                ViewGroup parent = (ViewGroup) buttonView.getParent();
                if (parent != null) {
                    parent.removeView(buttonView);
                }
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

    private Location addTrackedItemMarkers() {
        Location initialCameraLocation = null;

        if (mMap != null && !mMarkersAdded) {
            Log.d(TAG, "Adding map markers");

            for (TrackedItem trackedItem : TrackedItems.getTrackedItemsList()) {
                if (trackedItem.isEnabled()) {
                    trackedItem.createMapMarker(mMap);

                    if (mItemBeingFollowed == trackedItem.getID()) {
                        initialCameraLocation = trackedItem.getLastLocation();
                        Log.d(TAG, String.format("Last position of tracked item %s: %s", trackedItem.getName(), trackedItem.getLastLocation().toString()));
                    }
                }
            }

            mMarkersAdded = true;
        }

        return initialCameraLocation;
    }

    private void removeTrackedItemButtonsAndMarkers() {
        Log.d(TAG, "Removing buttons and markers");

        for (TrackedItem ti : TrackedItems.getTrackedItemsList()) {
            ti.removeButton();
            ti.removeMapMarker();
        }

        mMarkersAdded = false;
    }

    public void myLocationButtonClick(View view) {
        setItemBeingFollowed(IBF_MY_LOCATION);
    }

    @Override
    public void trackedItemButtonClick(TrackedItem trackedItem) {
        setItemBeingFollowed(trackedItem.getID());
    }

    @Override
    public void trackedItemButtonLongClick(TrackedItem trackedItem) {
        trackedItem.sendPing(this);
    }

    @Override
    public void trackedItemButtonDoubleClick(TrackedItem trackedItem) {
        trackedItem.getTrackerDevice().openContextMenu(this, trackedItem);
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

                Location lastLocation = MyLocation.getInstance().getLastLocation();
                if (lastLocation != null) {
                    mMapFragment.centerMap(lastLocation);
                }
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

    @Override
    public void HistoryUploadComplete(HistoryUploader uploader, HistoryUploaderState result) {
        Exception exception = uploader.getException();

        switch (result) {
            case SUCCESS:
                toastMessage("History uploaded");
                break;

            case NO_URLS:
                toastMessage("No URLs defined to upload history");
                break;

            case UNABLE_TO_CONNECT:
                if (exception == null) {
                    toastMessage("Unable to connect to upload server");
                } else {
                    alertMessage("Failed to connect", exception.getMessage());
                }
                break;

            case CANCELLED:
                toastMessage("Upload cancelled");
                break;

            case STOPPED:
                break;

            case FAILED:
                if (exception == null) {
                    toastMessage("Upload failed - Reason unknown");
                } else {
                    alertMessage("Upload failed", exception.getMessage());
                }
                break;

            default:
                alertMessage("Unknown result - " + result.toString(), exception == null ? "" : exception.getMessage());
                break;
        }

        mHistoryUploader = null;
    }

    private void toastMessage(String message) {
        Toast.makeText(this.getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void alertMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Resources res = this.getResources();

        builder
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(res.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setCancelable(true)
                .show();
    }

    private void keepScreenOn(boolean keepOn) {
        if (keepOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
