package me.marcsymonds.tracker;

import android.content.Context;
import android.util.Log;

import static android.content.Context.MODE_PRIVATE;

class MyLocation {
    private final static String TAG = "MyLocation";

    private final static String MY_HISTORY_DIR = "MyHistory";

    private static MyLocation mMyLocation = null; // Reference to singleton object.

    private Location mMyLastLocation = null;
    private HistoryManager mMyLocationHistory = null;

    // Private constructor so object cannot be created externally.
    private MyLocation() {
    }

    static synchronized MyLocation getInstance() {
        if (mMyLocation == null) {
            mMyLocation = new MyLocation();
        }

        return mMyLocation;
    }

    synchronized void recordLocation(Context context, Location location) {
        Log.d(TAG, "recordLocation: " + location.toString());

        if (mMyLocationHistory == null) {
            mMyLocationHistory = new HistoryManager(context.getDir(MY_HISTORY_DIR, MODE_PRIVATE));
        }

        if (mMyLastLocation == null
                || (mMyLastLocation.isGPS() && location.isGPS() && mMyLastLocation.distanceTo(location) > 4.0)
                || (mMyLastLocation.distanceTo(location) > 9.0)) {

            mMyLocationHistory.recordLocation(location);
            HistoryRecorder.getInstance(context).recordHistory(location);

            mMyLastLocation = location;
        } else {
            Log.d(TAG, "recordLocation: Not recording new location due to small distance change: " + String.valueOf(mMyLastLocation.distanceTo(location)));
        }
    }

    Location getLastLocation() {
        return mMyLastLocation;
    }

    void setLastLocation(Location location) {
        mMyLastLocation = location;
    }
}
