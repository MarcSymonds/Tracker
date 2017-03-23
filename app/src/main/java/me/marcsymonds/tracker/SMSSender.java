package me.marcsymonds.tracker;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

class SMSSender {
    // Intent action that will be passed to the SMS sender, who will then send it back once the
    // SMS message has been sent.
    static final String INTENT_SMS_SENT = "SMS_SENT";
    private static final String TAG = "SMSSender";

    void sendPingMessage(Activity activity, TrackedItem trackedItem) {
        // Do we already have permission to send SMS messages>
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            checkTrackedItem(activity, trackedItem);
        }
        // No. Have we been denied permission previously?
        else if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.SEND_SMS)) {
            // Yes. Tell the user to enable SMS permissions.
            Toast.makeText(
                    activity.getApplicationContext(),
                    "Insufficient permission to send SMS message. Enable 'SMS' permission for application.",
                    Toast.LENGTH_LONG)
                    .show();
        }
        // No, so ask permission to send SMS messages.
        else {
            // Use first 4 bits of request ID as the request ID, and put the ID of the object
            // to be acted on in the upper bits.
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.SEND_SMS},
                    Tracker.PERMISSION_REQUEST.SEND_SMS.getValue() + (trackedItem.getID() << 4));
        }
    }

    private void checkTrackedItem(final Activity activity, final TrackedItem trackedItem) {
        if (!trackedItem.isEnabled()) {
            Toast.makeText(
                    activity.getApplicationContext(),
                    String.format("%s is disabled.", trackedItem.getName()),
                    Toast.LENGTH_LONG)
                    .show();
        } else {
            TrackerDevice td = trackedItem.getTrackerDevice();
            if (td != null) {
                td.pingDevice(activity, trackedItem);
            }
        }
    }
}
