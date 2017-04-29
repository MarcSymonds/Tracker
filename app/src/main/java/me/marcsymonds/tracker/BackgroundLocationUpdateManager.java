package me.marcsymonds.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

/**
 * Created by Marc on 27/04/2017.
 */

public class BackgroundLocationUpdateManager extends BroadcastReceiver implements GoogleApiClient.ConnectionCallbacks {
    public static final String EVENT_START_LOCATION_UPDATES = "BGLU-START";
    public static final String EVENT_START_LOCATION_UPDATES_ISNS = "BGLU-START-ISNS"; // If state not set.
    public static final String EVENT_STOP_LOCATION_UPDATES = "BGLU_STOP";
    private static final String TAG = "BGLocUpdManager";
    private static BackgroundLocationUpdateManager mBackgroundLocationUpdateManager = null;

    private boolean mStateSet = false;
    private boolean mStarted = false;
    private GoogleApiClient mGoogleApiClient = null;

    private Handler mHandler = null;
    private Runnable mRunnable = null;

    // Private constructor so it can't be created elsewhere.
    private BackgroundLocationUpdateManager() {
    }

    public synchronized static BackgroundLocationUpdateManager getInstance() {
        // Singleton.

        if (mBackgroundLocationUpdateManager == null) {
            mBackgroundLocationUpdateManager = new BackgroundLocationUpdateManager();
        }

        return mBackgroundLocationUpdateManager;
    }

    /**
     * This method is called when the BroadcastReceiver is receiving an Intent
     * broadcast.  During this time you can use the other methods on
     * BroadcastReceiver to view/modify the current result values.  This method
     * is always called within the main thread of its process, unless you
     * explicitly asked for it to be scheduled on a different thread using
     * {@link Context# registerReceiver(BroadcastReceiver, * IntentFilter, String, Handler)}. When it runs on the main
     * thread you should
     * never perform long-running operations in it (there is a timeout of
     * 10 seconds that the system allows before considering the receiver to
     * be blocked and a candidate to be killed). You cannot launch a popup dialog
     * in your implementation of onReceive().
     * <p>
     * <p><b>If this BroadcastReceiver was launched through a &lt;receiver&gt; tag,
     * then the object is no longer alive after returning from this
     * function.</b>  This means you should not perform any operations that
     * return a result to you asynchronously -- in particular, for interacting
     * with services, you should use
     * {@link Context#startService(Intent)} instead of
     * {@link Context# bindService(Intent, ServiceConnection, int)}.  If you wish
     * to interact with a service that is already running, you can use
     * {@link #peekService}.
     * <p>
     * <p>The Intent filters used in {@link Context#registerReceiver}
     * and in application manifests are <em>not</em> guaranteed to be exclusive. They
     * are hints to the operating system about how to find suitable recipients. It is
     * possible for senders to force delivery to specific recipients, bypassing filter
     * resolution.  For this reason, {@link #onReceive(Context, Intent) onReceive()}
     * implementations should respond only to known actions, ignoring any unexpected
     * Intents that they may receive.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent.toString());
        switch (intent.getAction()) {
            case EVENT_START_LOCATION_UPDATES:
                mStateSet = true;
                startLocationUpdates(context);
                break;

            case EVENT_STOP_LOCATION_UPDATES:
                mStateSet = true;
                stopLocationUpdates();
                break;

            // Used by the service starter. If the application is updated while the application is
            // running, there is a race condition between the service and the Tracker activity to
            // initialise this manager; the activity could set the state to stop updates, and then
            // the service could set the state to started. So we end up with the activity getting
            // location updates, and this manager getting the location updates, which is not needed.
            // So this event is used by the service, so that if the activity has already set the
            // state, then we won't change it.
            case EVENT_START_LOCATION_UPDATES_ISNS:
                if (!mStateSet) {
                    mStateSet = true;
                    startLocationUpdates(context);
                }

        }
    }

    private void startLocationUpdates(Context context) {
        Log.d(TAG, "startLocationUpdates: " + context.toString());
        if (!mStarted) {
            GoogleApiClient.Builder apiBuilder = new GoogleApiClient.Builder(context);
            apiBuilder.addApi(LocationServices.API);
            apiBuilder.addConnectionCallbacks(this);

            mGoogleApiClient = apiBuilder.build();

            mGoogleApiClient.connect();

            Log.d(TAG, "startLocationUpdates: Connecting to location services");

            mStarted = true;
        }
    }

    private void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates: ");

        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
            mRunnable = null;
            mHandler = null;
        }

        if (mStarted) {
            mGoogleApiClient.disconnect();

            mStarted = false;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: ");

        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
            mRunnable = null;
            mHandler = null;
        }

        final Context context = mGoogleApiClient.getContext().getApplicationContext();

        mHandler = new Handler();

        mRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBackgroundUpdateEnabled(context)) {
                    updateLocation();
                }

                mHandler.postDelayed(this, getLocationUpdateInterval(context));
            }
        };

        mHandler.postDelayed(mRunnable, getLocationUpdateInterval(context));
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private boolean isBackgroundUpdateEnabled(Context context) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(Pref.PREF_MY_LOCATION_BACKGROUND_UPDATES_ENABLED, false);
    }

    private long getLocationUpdateInterval(Context context) {
        if (isBackgroundUpdateEnabled(context)) {
            return PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getLong(Pref.PREF_MY_LOCATION_BACKGROUND_UPDATE_INTERVAL, 5) * 60 * 1000; // Minutes -> milliseconds
        } else {
            return 2 * 60 * 1000; // 2 minutes intervals if recording is currently disabled.
        }
    }

    private void updateLocation() {
        try {
            // Note: android.location.Location
            android.location.Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (location != null) {
                Log.d(TAG, "updateLocation: " + location.toString());

                MyLocation
                        .getInstance()
                        .recordLocation(
                                mGoogleApiClient.getContext().getApplicationContext(),
                                new Location(0, location) // 0 = My Location
                        );

            }
        } catch (SecurityException sec) {
            Log.e(TAG, "updateLocation: Failed getting last location", sec);
        }
    }
}
