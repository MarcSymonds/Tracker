package me.marcsymonds.tracker;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.widget.Toast;

import java.util.Locale;

/**
 * This class is used to receive messages from the SMS service once it has sent an SMS to a tracker
 * device. This is just looking for success/failure of the SMS being sent, it is not looking for
 * and response from the tracker. That is dealt with by the TrackerDevice for the tracked item.
 */
class SMSSenderReceiver {
    private static final String TAG = "SMSSenderReceiver";

    private static BroadcastReceiver mBroadcastReceiver;

    static void setupBroadcastReceiver(final Activity activity) {
        // Filter for messages to receive.
        IntentFilter filt = new IntentFilter(SMSSender.INTENT_SMS_SENT);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String msg;
                boolean showMsg = true;
                int trackedItemID = intent.getIntExtra("TrackedItemID", 0);
                int action = intent.getIntExtra("Action", 0);

                TrackedItem trackedItem = TrackedItems.getItemByID(trackedItemID);
                TrackerDevice trackerDevice;

                if (getResultCode() == Activity.RESULT_OK) {
                    msg = "SMS sent";

                    if (trackedItem != null) {
                        trackerDevice = trackedItem.getTrackerDevice();
                        if (action == TrackerDevice.ACTION_SENT_PING && trackerDevice != null) {
                            showMsg = trackerDevice.messageSent(trackedItem, action);
                        }
                    }
                }
                else {
                    switch (getResultCode()) {
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            msg = "Generic failure";
                            break;

                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            msg = "No service";
                            break;

                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            msg = "Null PDU";
                            break;

                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            msg = "Radio off";
                            break;

                        default:
                            msg = String.format(Locale.getDefault(), "Unknown SMS result: %d", getResultCode());
                            break;
                    }

                    if (trackedItem != null) {
                        trackerDevice = trackedItem.getTrackerDevice();
                        if (trackerDevice != null) {
                            showMsg = trackerDevice.messageFailed(trackedItem, action, getResultCode(), msg);
                        }
                    }
                }

                if (showMsg) {
                    Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }
        };

        activity.registerReceiver(mBroadcastReceiver, filt);
    }

    public static void tearDown(Activity activity) {
        activity.unregisterReceiver(mBroadcastReceiver);
        mBroadcastReceiver = null;
    }
}
