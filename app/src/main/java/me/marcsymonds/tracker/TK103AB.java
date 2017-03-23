package me.marcsymonds.tracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Marc on 15/03/2017.
 */

class TK103AB extends TrackerDevice implements ITrackerDevice {
    private static final String TAG = "TK103AB";

    private static final String TD_TELEPHONE_COUNTRY_CODE = "tracker_device_telephone_country_code";
    private static final String TD_TELEPHONE_NUMBER = "tracker_device_telephone_number";
    private static final String TD_DEVICE_PASSWORD = "tracker_device_password";
    private static final String TD_PING_COMMAND = "tracker_device_ping_command";
    private static final String TD_PING_RESPONSES = "tracker_device_ping_responses";
    private static final String TD_PING_RESPONSE_DELAY = "tracker_device_ping_response_delay";
    private static final String TD_AUTO_RESEND_PING = "tracker_device_auto_resend_ping";
    private static final String TD_AUTO_RESEND_PING_DELAY = "tracker_device_auto_resend_ping_delay";
    private static final String mArmCommand = "arm$p";
    private static final String mDisarmCommand = "arm$p";
    private String mTelephoneCountryCode = "44";
    private String mTelephoneNumber = "";
    private String mDevicePassword = "";
    private String mPingCommand = "fix001s001n$p";
    private int mPingResponsesExpected = 1;
    private int mPingResponseDelay = 30;
    private boolean mAutoResendPing = false;
    private int mAutoResendPingDelay = 60;
    private boolean mPinged = false;
    private int mPingResponsesReceived = 0;

    TK103AB() {
        super();
    }

    @Override
    public void putToSharedPreferences(SharedPreferences sp) {
        SharedPreferences.Editor editor = sp.edit();

        editor.putString(TD_TELEPHONE_COUNTRY_CODE, mTelephoneCountryCode);
        editor.putString(TD_TELEPHONE_NUMBER, mTelephoneNumber);
        editor.putString(TD_DEVICE_PASSWORD, mDevicePassword);
        editor.putString(TD_PING_COMMAND, mPingCommand);
        editor.putString(TD_PING_RESPONSES, String.valueOf(mPingResponsesExpected));
        editor.putString(TD_PING_RESPONSE_DELAY, String.valueOf(mPingResponseDelay));
        editor.putBoolean(TD_AUTO_RESEND_PING, mAutoResendPing);
        editor.putString(TD_AUTO_RESEND_PING_DELAY, String.valueOf(mAutoResendPingDelay));

        editor.commit();

        Log.d(TAG, String.format("putToSharedPreferences: %s", sp.getAll().toString()));
    }

    @Override
    public void getFromSharedPreferences(SharedPreferences sp) {
        mTelephoneCountryCode = sp.getString(TD_TELEPHONE_COUNTRY_CODE, mTelephoneCountryCode);
        mTelephoneNumber = sp.getString(TD_TELEPHONE_NUMBER, mTelephoneNumber);
        mDevicePassword = sp.getString(TD_DEVICE_PASSWORD, mDevicePassword);
        mPingCommand = sp.getString(TD_PING_COMMAND, mPingCommand);
        mPingResponsesExpected = Integer.parseInt(sp.getString(TD_PING_RESPONSES, String.valueOf(mPingResponsesExpected)));
        mPingResponseDelay = Integer.parseInt(sp.getString(TD_PING_RESPONSE_DELAY, String.valueOf(mPingResponseDelay)));
        mAutoResendPing = sp.getBoolean(TD_AUTO_RESEND_PING, mAutoResendPing);
        mAutoResendPingDelay = Integer.parseInt(sp.getString(TD_AUTO_RESEND_PING_DELAY, String.valueOf(mAutoResendPingDelay)));

        Log.d(TAG, String.format("getFromSharedPreferences: %s", sp.getAll().toString()));
    }

    @Override
    public void clearSharedPreferences(SharedPreferences sp) {
        SharedPreferences.Editor editor = sp.edit();

        editor.putString(TD_TELEPHONE_COUNTRY_CODE, "44");
        editor.putString(TD_TELEPHONE_NUMBER, "");
        editor.putString(TD_DEVICE_PASSWORD, "");
        editor.putString(TD_PING_COMMAND, "fix001s001n$p");
        editor.putString(TD_PING_RESPONSES, "1");
        editor.putString(TD_PING_RESPONSE_DELAY, "30");
        editor.putBoolean(TD_AUTO_RESEND_PING, false);
        editor.putString(TD_AUTO_RESEND_PING_DELAY, "60");

        editor.commit();
    }

    @Override
    public boolean loadFromSave(String name, String value) {
        boolean loaded = true;

        Log.d(TAG, String.format("loadFromSave %s = %s", name, value));

        switch (name) {
            case TD_TELEPHONE_COUNTRY_CODE:
                mTelephoneCountryCode = value;
                break;

            case TD_TELEPHONE_NUMBER:
                mTelephoneNumber = value;
                break;

            case TD_PING_COMMAND:
                mPingCommand = value;
                break;

            case TD_PING_RESPONSES:
                mPingResponsesExpected = Integer.parseInt(value);
                break;

            case TD_PING_RESPONSE_DELAY:
                mPingResponseDelay = Integer.parseInt(value);
                break;

            case TD_AUTO_RESEND_PING:
                mAutoResendPing = Boolean.parseBoolean(value);
                break;

            case TD_AUTO_RESEND_PING_DELAY:
                mAutoResendPingDelay = Integer.parseInt(value);
                break;

            case TD_DEVICE_PASSWORD:
                mDevicePassword = value;
                break;

            default:
                loaded = false;
                break;
        }
        return loaded;
    }

    @Override
    public void saveValuesToFile(BufferedWriter writer) throws IOException {
        Log.d(TAG, String.format("Saving values"));

        writer.write(TD_TELEPHONE_COUNTRY_CODE + ":" + mTelephoneCountryCode);
        writer.newLine();
        writer.write(TD_TELEPHONE_NUMBER + ":" + mTelephoneNumber);
        writer.newLine();
        writer.write(TD_PING_COMMAND + ":" + mPingCommand);
        writer.newLine();
        writer.write(TD_PING_RESPONSES + ":" + String.valueOf(mPingResponsesExpected));
        writer.newLine();
        writer.write(TD_PING_RESPONSE_DELAY + ":" + String.valueOf(mPingResponseDelay));
        writer.newLine();
        writer.write(TD_AUTO_RESEND_PING + ":" + String.valueOf(mAutoResendPing));
        writer.newLine();
        writer.write(TD_AUTO_RESEND_PING_DELAY + ":" + mAutoResendPingDelay);
        writer.newLine();
        writer.write(TD_DEVICE_PASSWORD + ":" + mDevicePassword);
        writer.newLine();
    }

    @Override
    public void pingDevice(final Activity activity, final TrackedItem trackedItem) {
        if (mTelephoneNumber.length() == 0) {
            Toast.makeText(
                    activity.getApplicationContext(),
                    String.format("Telephone number not set for %s", trackedItem.getName()),
                    Toast.LENGTH_LONG)
                    .show();
        } else {
            int awaiting = mPingResponsesExpected - mPingResponsesReceived;
            if (mPinged && awaiting > 0) {
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
            } else {
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

        Intent intent = new Intent(SMSSender.INTENT_SMS_SENT);
        intent.putExtra("TrackedItemID", trackedItem.getID());
        PendingIntent sentPending = PendingIntent.getBroadcast(activity.getApplicationContext(), 1, intent, PendingIntent.FLAG_ONE_SHOT);

        smsManager.sendTextMessage(
                mTelephoneNumber,
                null,
                addPasswordToCommand(mPingCommand),
                sentPending,
                null);
    }

    @Override
    public void pingSent(TrackedItem trackedItem) {
        mPinged = true;
        mPingResponsesReceived = 0;

        trackedItem.setPingingButtonState(true);

        //TODO: Set up alarm to resend the ping after specified time.
    }

    @Override
    public void pingFailed(TrackedItem trackedItem, int resultCode, String message) {
        trackedItem.setPingingButtonState(false);
    }

    @Override
    public boolean messageReceived(Context context, TrackedItem trackedItem, String source, String message) {
        boolean handled = false;

        if (isMessageFor(source)) {
                    /*
        Response from TK103A may be one of two message formats, depending on whether the device
        has got GPS service:-

            Lac:544c 54b5  <-- Current GSM/Cellular coordinates.
            T:14/01/01 01:13
            Last:
            T:03:47
            http://maps.google.com/maps?f=q&q=51.405690,0.906240&z=16  <-- Last known GPS coordinates.

        or

            lat:51.504748
            long:0.906225
            speed:0.0
            T:17/02/26 19:12
            http://maps.google.com/maps?f=q&q=51.405690,0.906240&z=16
            Pwr: ON Door: OFF ACC: OFF
        */

        /*
            This format means the tracker does not have GPS coordinates, so it is reporting the
            Location Area Code (LAC) and Cell ID (CID) of the tower it is connected to. The message
            also contains the last known GPS coordinates as part of a HTML link. So we will use the
            GPS coordinates from the HTML link as the location in this instance.

            TODO: Possible lookup LAC/CID. Somehow.
         */

            // DOTALL means the dot (.) will match new-line characters.
            Pattern pat = Pattern.compile("Lac:.*http://.*q=(-?[0-9\\.]*),(-?[0-9\\.]*)", Pattern.MULTILINE + Pattern.CASE_INSENSITIVE + Pattern.DOTALL);
            Matcher mtch = pat.matcher(message);

            boolean found = false;
            double lat = 0, lng = 0;
            boolean gps = false;

            if (mtch.find()) {
                lat = Double.parseDouble(mtch.group(1));
                lng = Double.parseDouble(mtch.group(2));

                found = true;
            } else {
                pat = Pattern.compile("lat:\\s*(-?[0-9\\.]*)\\s*long:\\s*(-?[0-9\\.]*)", Pattern.MULTILINE + Pattern.CASE_INSENSITIVE + Pattern.DOTALL);
                mtch = pat.matcher(message);

                if (mtch.find()) {
                    lat = Double.parseDouble(mtch.group(1));
                    lng = Double.parseDouble(mtch.group(2));
                    gps = true;
                    found = true;
                }
            }

            if (found) {
                trackedItem.newLocationReceived(context, new Location(lat, lng, gps));

                mPingResponsesReceived++;
                if (mPingResponsesReceived >= mPingResponsesExpected) {
                    mPinged = false;
                    trackedItem.setPingingButtonState(false);
                }

                handled = true;
            }
        }

        return handled;
    }

    @Override
    public boolean isMessageFor(String source) {
        return Telephony.sameNumbers(source, mTelephoneNumber, mTelephoneCountryCode);
    }

    private String addPasswordToCommand(String command) {
        return command.replaceAll("\\$p", mDevicePassword == null ? "" : mDevicePassword);
    }
}
