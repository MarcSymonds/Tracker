package me.marcsymonds.tracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.telephony.SmsManager;
import android.widget.Toast;

class SMSSender {
    // Intent action that will be passed to the SMS sender, who will then send it back once the
    // SMS message has been sent.
    static final String INTENT_SMS_SENT = "me.marcsymonds.Tracker.SMS_SENT";
    private static final String TAG = "SMSSender";

    /*boolean canSendSMSMessage(Activity activity) {
        boolean canSend = false;
        // Do we already have permission to send SMS messages>
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            canSend = true;
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
                    Tracker.PERMISSION_REQUEST.SEND_SMS.getValue());
        }

        return canSend;
    }*/

    boolean sendMessage(Activity activity, String recipient, String smsMessage, String toastMessage, int trackedItemID, int action) {
        boolean couldSend = false;

        if (PermissionChecker.acquireSMSSendPermissionForDevice(activity, trackedItemID)) {
            if (recipient == null || recipient.length() == 0) {
                TrackedItem trackedItem = TrackedItems.getItemByID(trackedItemID);

                Toast.makeText(
                        activity.getApplicationContext(),
                        String.format(
                                activity.getResources().getString(R.string.error_unable_to_send_no_recipient),
                                trackedItem == null ? "?" : trackedItem.getName()),
                        Toast.LENGTH_SHORT)
                        .show();
            } else {
                try {
                    SmsManager smsManager = SmsManager.getDefault();

                    Toast.makeText(
                            activity.getApplicationContext(),
                            toastMessage,
                            Toast.LENGTH_SHORT)
                            .show();

                    Intent intent = new Intent(SMSSender.INTENT_SMS_SENT);
                    intent.putExtra("TrackedItemID", trackedItemID);
                    intent.putExtra("Action", action);

                    PendingIntent sentPending = PendingIntent.getBroadcast(activity.getApplicationContext(), 1, intent, PendingIntent.FLAG_ONE_SHOT);

                    smsManager.sendTextMessage(
                            recipient,
                            null,
                            smsMessage,
                            sentPending,
                            null);

                    couldSend = true;
                } catch (Exception ex) {
                    Resources res = activity.getResources();
                    AlertDialog.Builder msg = new AlertDialog.Builder(activity);

                    msg
                            .setTitle("Error sending SMS message")
                            .setMessage(ex.getMessage())
                            .setNegativeButton(res.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // Do nothing.
                                }
                            })
                            .setCancelable(true);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        msg.setIcon(res.getDrawable(android.R.drawable.ic_menu_info_details, null));
                    } else {
                        msg.setIcon(res.getDrawable(android.R.drawable.ic_menu_info_details));
                    }

                    msg.show();
                }
            }
        }

        return couldSend;
    }
}
