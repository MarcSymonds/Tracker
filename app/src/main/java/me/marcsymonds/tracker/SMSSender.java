package me.marcsymonds.tracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.widget.Toast;

import java.util.Locale;

class SMSSender {
    private static final String TAG = "SMSSender";

    // Intent action that will be passed to the SMS sender, who will then send it back once the
    // SMS message has been sent.
    static final String INTENT_SMS_SENT = "SMS_SENT";

    //private static Activity mActivity;

    static void setActivity() {//Activity activity) {
        //mActivity = activity;
    }

    static void tearDown() {
        //mActivity = null;
    }

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
        } else if (trackedItem.getTelephoneNumber().length() == 0) {
            Toast.makeText(
                    activity.getApplicationContext(),
                    String.format("Telephone number not set for %s", trackedItem.getName()),
                    Toast.LENGTH_LONG)
                    .show();
        } else {
            int awaiting = trackedItem.getNumberOfResponses() - trackedItem.getNumberOfResponsesReceived();
            if (trackedItem.getSentPingRequest() && awaiting > 0) {
                new AlertDialog.Builder(activity)
                        .setTitle("Awaiting Responses")
                        .setMessage(String.format(Locale.getDefault(), "Still awaiting %d response(s). Are you sure you want to send another request?", awaiting))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                doSendPingMessage(activity, trackedItem);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
            else {
                doSendPingMessage(activity, trackedItem);
            }
        }
    }

    private void doSendPingMessage(Activity activity, TrackedItem trackedItem) {
        SmsManager smsManager = SmsManager.getDefault();

        Toast.makeText(
                activity.getApplicationContext(),
                String.format("Sending location request to %s", trackedItem.getName()),
                Toast.LENGTH_SHORT)
                .show();

        Intent intent = new Intent(INTENT_SMS_SENT);
        intent.putExtra("TrackedItemID", trackedItem.getID());
        PendingIntent sentPending = PendingIntent.getBroadcast(activity.getApplicationContext(), 1, intent, PendingIntent.FLAG_ONE_SHOT);

        smsManager.sendTextMessage(
                trackedItem.getTelephoneNumber(),
                null,
                trackedItem.getLocationCommand(),
                sentPending,
                null);
    }
}
