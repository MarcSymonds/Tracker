package me.marcsymonds.tracker;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Marc on 27/02/2017.
 */

public class SMSReceiver {
    private final static String TAG = "SMSReceiver";

    private static Activity mActivity;
    private static BroadcastReceiver mReceiver = null;

    public static void setupBroadcastReceiver(Activity activity) {
        IntentFilter filt = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");

        mActivity = activity;

        if (hasPermission()) {
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, String.format("Received SMS message - %s", intent.toString()));

                    Bundle bundle = intent.getExtras();
                    SmsMessage msg = null;
                    String msgFormat = bundle.getString("format");

                    Object[] pdus = (Object[]) bundle.get("pdus");
                    for (int i = 0; i < pdus.length; i++) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            msg = SmsMessage.createFromPdu((byte[]) pdus[i], msgFormat);
                        } else {
                            msg = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        }

                        String source = msg.getOriginatingAddress();
                        String message = msg.getMessageBody();

                        Log.d(TAG, String.format("From:%s, Message:%s", msg.getOriginatingAddress(), msg.getMessageBody()));

                        for (TrackedItem ti : TrackedItems.getTrackedItemsList()) {
                            if (Telephony.sameNumbers(source, ti.getTelephoneNumber(), ti.getTelephoneCountryCode())) {
                                ti.messageReceived(message);
                            }
                        }
                    }
                }
            };

            activity.registerReceiver(mReceiver, filt);
        }
    }

    private static boolean hasPermission() {
        boolean havePermission = false;

        // Do we already have permission to read SMS messages?
        if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            // No. Have we been denied permission previously?
            if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.READ_SMS)) {
                // Yes. Tell the user to enable SMS permissions.
                Toast.makeText(
                        mActivity.getApplicationContext(),
                        "Insufficient permission to read SMS message. Enable 'SMS' permission for application.",
                        Toast.LENGTH_LONG)
                        .show();
            }
            // No, so ask permission to send SMS messages.
            else {
                // Use first 4 bits of request ID as the request ID, and put the ID of the object
                // to be acted on in the upper bits.
                ActivityCompat.requestPermissions(
                        mActivity,
                        new String[]{Manifest.permission.SEND_SMS},
                        Tracker.PERMISSION_REQUEST.READ_SMS.getValue());
            }
        }
        // Do we have permission to receive SMS messages?
        else if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            // Yes.
            havePermission = true;
        }
        // No. Have we been denied permission previously?
        else if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.RECEIVE_SMS)) {
            // Yes. Tell the user to enable SMS permissions.
            Toast.makeText(
                    mActivity.getApplicationContext(),
                    "Insufficient permission to receive SMS messages. Enable 'SMS' permission for application.",
                    Toast.LENGTH_LONG)
                    .show();
        }
        // No, so ask permission to receive SMS messages.
        else {
            // Use first 4 bits of request ID as the request ID, and put the ID of the object
            // to be acted on in the upper bits.
            ActivityCompat.requestPermissions(
                    mActivity,
                    new String[]{Manifest.permission.RECEIVE_SMS},
                    Tracker.PERMISSION_REQUEST.RECEIVE_SMS.getValue());
        }

        return havePermission;
    }

    public static void teadDown() {
        if (mReceiver != null) {
            mActivity.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }
}
