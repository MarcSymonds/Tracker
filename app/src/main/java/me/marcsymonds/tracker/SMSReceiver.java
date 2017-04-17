package me.marcsymonds.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

class SMSReceiver extends BroadcastReceiver {
    private final static String TAG = "SMSReceiver";

    SMSReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, String.format("Received SMS message - %s", intent.toString()));

        Bundle bundle = intent.getExtras();
        SmsMessage msg;
        String msgFormat = bundle.getString("format");

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus != null) {
            for (Object pdu : pdus) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    msg = SmsMessage.createFromPdu((byte[]) pdu, msgFormat);
                } else {
                    msg = SmsMessage.createFromPdu((byte[]) pdu);
                }

                String source = msg.getOriginatingAddress();
                String message = msg.getMessageBody();

                Log.d(TAG, String.format("From:%s, Message:%s", msg.getOriginatingAddress(), msg.getMessageBody()));

                for (TrackedItem ti : TrackedItems.getTrackedItemsList()) {
                    TrackerDevice td = ti.getTrackerDevice();
                    if (td != null) {
                        if (td.messageReceived(context, ti, source, message)) {
                            Log.i(TAG, String.format("Message handled by Tracked Item %s", ti.getName()));
                        }
                    }
                }
            }
        }
    }
}
