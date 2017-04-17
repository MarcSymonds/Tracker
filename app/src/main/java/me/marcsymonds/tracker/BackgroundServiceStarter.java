package me.marcsymonds.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * Created by Marc on 14/04/2017.
 */

/**
 * The class is used to receive a notification when the device is booted, at which point it will
 * start the background service process "BackgroundService".
 */
public class BackgroundServiceStarter extends BroadcastReceiver {
    private final static String TAG = "BGServiceStarter";

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
        boolean start = false;
        String action = intent.getAction();

        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
                start = true;
                break;

            case Intent.ACTION_PACKAGE_ADDED:
            case Intent.ACTION_PACKAGE_REPLACED:
                Uri uri = intent.getData();

                // There is a "Scheme Specific Part" filter in the manifest, but that option was
                // only added in API 19 (KITKAT), so we'll double check the intent identifies this
                // package.

                if (uri.getScheme().equals("package") && uri.getSchemeSpecificPart().equals(context.getPackageName())) {
                    start = true;
                }
                break;
        }

        if (start) {
            if (!BackgroundService.isServiceRunning()) {
                Log.d(TAG, "onReceive: Starting service");
                Intent serviceIntent = new Intent(context, BackgroundService.class);
                context.startService(serviceIntent);
            } else {
                Log.d(TAG, "onReceive: Service is already running");
            }
        }
    }
}
