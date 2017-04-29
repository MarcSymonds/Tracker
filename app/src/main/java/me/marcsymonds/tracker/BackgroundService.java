package me.marcsymonds.tracker;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;


/**
 * This class is instantiated by the BackgroundServiceStarter class, and sets up a receiver to
 * listen for new SMS messages.
 */
public class BackgroundService extends Service {
    private final static String TAG = "BackgroundService";

    private static boolean mServiceRunning = false;

    private SMSReceiver mSMSReceiver = null;
    private BackgroundLocationUpdateManager mBackgrounLocationUpdateManager = null;

    static boolean isServiceRunning() {
        return mServiceRunning;
    }

    static void runIfNotStarted(Context context) {
        if (!mServiceRunning) {
            Intent serviceIntent = new Intent(context, BackgroundService.class);
            context.startService(serviceIntent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * Called by the system every time a client explicitly starts the service by calling
     * {@link Context#startService}, providing the arguments it supplied and a
     * unique integer token representing the start request.  Do not call this method directly.
     * <p>
     * <p>For backwards compatibility, the default implementation calls
     * {@link #onStart} and returns either {@link #START_STICKY}
     * or {@link #START_STICKY_COMPATIBILITY}.
     * <p>
     * <p>If you need your application to run on platform versions prior to API
     * level 5, you can use the following model to handle the older {@link #onStart}
     * callback in that case.  The <code>handleCommand</code> method is implemented by
     * you as appropriate:
     * <p>
     * {@sample development/samples/ApiDemos/src/com/example/android/apis/app/ForegroundService.java
     * start_compatibility}
     * <p>
     * <p class="caution">Note that the system calls this on your
     * service's main thread.  A service's main thread is the same
     * thread where UI operations take place for Activities running in the
     * same process.  You should always avoid stalling the main
     * thread's event loop.  When doing long-running operations,
     * network calls, or heavy disk I/O, you should kick off a new
     * thread, or use {@link AsyncTask}.</p>
     *
     * @param intent  The Intent supplied to {@link Context#startService},
     *                as given.  This may be null if the service is being restarted after
     *                its process has gone away, and it had previously returned anything
     *                except {@link #START_STICKY_COMPATIBILITY}.
     * @param flags   Additional data about this start request.  Currently either
     *                0, {@link #START_FLAG_REDELIVERY}, or {@link #START_FLAG_RETRY}.
     * @param startId A unique integer representing this specific request to
     *                start.  Use with {@link #stopSelfResult(int)}.
     * @return The return value indicates what semantics the system should
     * use for the service's current started state.  It may be one of the
     * constants associated with the {@link #START_CONTINUATION_MASK} bits.
     * @see #stopSelfResult(int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        TrackedItems.initialise(this.getApplicationContext());

        Log.d(TAG, "onStartCommand " + (intent == null ? "NO INTENT" : intent.toString()) + " " + Boolean.toString(mServiceRunning));
        if (PermissionChecker.hasPermissions(this.getApplicationContext(), Manifest.permission.RECEIVE_SMS)) {
            mSMSReceiver = new SMSReceiver();
            Log.d(TAG, "REGISTERING SMS");
            IntentFilter filt = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
            registerReceiver(mSMSReceiver, filt);
        } else {
            Log.d(TAG, "NO PERMS to RECEIVE SMS");
            Toast.makeText(this.getApplicationContext(),
                    "Need permission to receive SMS messages",
                    Toast.LENGTH_LONG).show();
        }

        mBackgrounLocationUpdateManager = BackgroundLocationUpdateManager.getInstance();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BackgroundLocationUpdateManager.EVENT_START_LOCATION_UPDATES);
        filter.addAction(BackgroundLocationUpdateManager.EVENT_STOP_LOCATION_UPDATES);
        filter.addAction(BackgroundLocationUpdateManager.EVENT_START_LOCATION_UPDATES_ISNS);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getApplicationContext());

        lbm.registerReceiver(mBackgrounLocationUpdateManager, filter);

        // Could just call the function mBackgrounLocationUpdateManager.onReceive(.,.) - not sure if that's a good idea.
        lbm.sendBroadcast(new Intent(BackgroundLocationUpdateManager.EVENT_START_LOCATION_UPDATES_ISNS));

        Log.d(TAG, "onStartCommand: Registered background location update manager and started it");

        mServiceRunning = true;

        return START_STICKY;// super.onStartCommand(intent, flags, startId);
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.  The
     * service should clean up any resources it holds (threads, registered
     * receivers, etc) at this point.  Upon return, there will be no more calls
     * in to this Service object and it is effectively dead.  Do not call this method directly.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mSMSReceiver != null) {
            unregisterReceiver(mSMSReceiver);
            mSMSReceiver = null;
        }

        if (mBackgrounLocationUpdateManager != null) {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mBackgrounLocationUpdateManager);
        }

        mServiceRunning = false;
    }

    /**
     * Return the communication channel to the service.  May return null if
     * clients can not bind to the service.  The returned
     * {@link IBinder} is usually for a complex interface
     * that has been <a href="{@docRoot}guide/components/aidl.html">described using
     * aidl</a>.
     * <p>
     * <p><em>Note that unlike other application components, calls on to the
     * IBinder interface returned here may not happen on the main thread
     * of the process</em>.  More information about the main thread can be found in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html">Processes and
     * Threads</a>.</p>
     *
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     * @return Return an IBinder through which clients can call on to the
     * service.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
