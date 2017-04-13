package me.marcsymonds.tracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for managing TK103A/B tracker devices.
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
    private static final String mDisarmCommand = "disarm$p";
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
        /*if (mTelephoneNumber.length() == 0) {
            Toast.makeText(
                    activity.getApplicationContext(),
                    String.format("Telephone number not set for %s", trackedItem.getName()),
                    Toast.LENGTH_LONG)
                    .show();
        } else {*/
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
        //}
    }

    private void doSendPingMessage(Activity activity, TrackedItem trackedItem) {
        SMSSender sender = new SMSSender();

        sender.sendMessage(
                activity,
                mTelephoneNumber,
                addPasswordToCommand(mPingCommand),
                String.format("Sending location request to %s", trackedItem.getName()),
                trackedItem.getID(),
                ACTION_SENT_PING);
    }

    @Override
    public boolean messageSent(TrackedItem trackedItem, int action) {
        switch (action) {
            case TrackerDevice.ACTION_SENT_PING:
                mPinged = true;
                mPingResponsesReceived = 0;

                trackedItem.setPingingButtonState(true);
                break;
        }

        //TODO: Set up alarm to resend the ping after specified time.

        return true;
    }

    @Override
    public boolean messageFailed(TrackedItem trackedItem, int action, int resultCode, String message) {
        switch (action) {
            case TrackerDevice.ACTION_SENT_PING:
                trackedItem.setPingingButtonState(false);
                break;
        }

        return true;
    }

    @Override
    public boolean messageReceived(Context context, TrackedItem trackedItem, String source, String message) {
        boolean handled = false;

        if (isMessageFor(source)) {
            /*
            Response from TK103A may be one of two message formats, depending on whether the device
            has got GPS service:-

            [message]?
            [[Lac: 12ab 34cd]?
            [lat:12.3456
            long:21.6543]
            *
            http://maps.google.com/maps*&q=12.3456,21.6543&z=<zoom>
            ]?

            an optional message,
            possibly followed by a "Lac:", or a "lat:" and "long:"
            and possibly further down a URL containing the last known GPS coords

                sensor alarm!
                Lac:234c 12b5  <-- Current GSM/Cellular coordinates.
                T:14/01/01 01:13
                Last:
                T:03:47
                http://maps.google.com/maps?f=q&q=48.405690,1.506240&z=16  <-- Last known GPS coordinates.

            or

                sensor alarm!
                lat:48.404748
                long:1.506225
                speed:0.0
                T:17/02/26 19:12
                http://maps.google.com/maps?f=q&q=48.404748,1.506225&z=16
                Pwr: ON Door: OFF ACC: OFF

            When "Lac:" is sent, This format means the tracker does not have GPS coordinates, so it
            is reporting the Location Area Code (LAC) and Cell ID (CID) of the tower it is connected
            to. The message also contains the last known GPS coordinates as part of a HTML link.
            So we will use the GPS coordinates from the HTML link as the location in this instance.

            TODO: Possible lookup LAC/CID. Somehow.

            DOTALL means the dot (.) will match new-line characters.
            */

            Location location = new Location(trackedItem.getID());

            Pattern pat;
            Matcher mtch;
            boolean found = false;
            boolean possibleMsg = true;

            pat = Pattern.compile("Lac:(\\p{XDigit}+) (\\p{XDigit}+)", Pattern.CASE_INSENSITIVE);
            mtch = pat.matcher(message);
            if (mtch.find()) {
                location.setLACCID(mtch.group(1), mtch.group(2));

                if (mtch.start() == 0) {
                    possibleMsg = false;
                }

                found = true;
            }

            pat = Pattern.compile("lat:(-?[0-9\\.]+)\\nlong:(-?[0-9\\.]+)", Pattern.CASE_INSENSITIVE);
            mtch = pat.matcher(message);
            if (mtch.find()) {
                location.setLocation(Double.parseDouble(mtch.group(1)), Double.parseDouble(mtch.group(2)));
                location.setGPS(true);

                if (mtch.start() == 0) {
                    possibleMsg = false;
                }

                found = true;
            }

            // Don't bother getting the location from the link in the message, if we already have
            // the coordinates.
            if (!location.hasLocation()) {
                pat = Pattern.compile("http://.*q=(-?[0-9\\\\.]+),(-?[0-9\\\\.]+)", Pattern.CASE_INSENSITIVE);
                mtch = pat.matcher(message);
                if (mtch.find()) {
                    location.setLastKnownLocation(Double.parseDouble(mtch.group(1)), Double.parseDouble(mtch.group(2)));

                    found = true;
                }
            }

            // There is possibly a message at the start of the SMS message, so extract that.
            if (possibleMsg) {
                pat = Pattern.compile("^([^\\n]+)");
                mtch = pat.matcher(message);
                if (mtch.find()) {
                    location.setMessage(mtch.group(1));
                }

                found = true;
            }

            if (found) {
                trackedItem.newLocationReceived(context, location);

                if (mPinged) {
                    mPingResponsesReceived++;
                    if (mPingResponsesReceived >= mPingResponsesExpected) {
                        mPinged = false;
                        trackedItem.setPingingButtonState(false);
                    }
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

    @Override
    public String getTelephoneNumber() {
        return mTelephoneNumber;
    }

    public void setTelephoneNumber(String telephoneNumber) {
        mTelephoneNumber = telephoneNumber;
    }

    public void setTelephoneCountryCode(String telephoneCountryCode) {
        mTelephoneCountryCode = telephoneCountryCode;
    }

    private String addPasswordToCommand(String command) {
        return command.replaceAll("\\$p", mDevicePassword == null ? "" : mDevicePassword);
    }

    @Override
    public boolean openContextMenu(final Activity activity, final TrackedItem trackedItem) {
        PopupMenu pm = new PopupMenu(activity, trackedItem.getButton(activity, false).getButtonView());
        pm.inflate(R.menu.menu_tk103ab);
        pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.arm_device:
                        armDevice(activity, trackedItem);
                        break;

                    case R.id.disarm_device:
                        disarmDevice(activity, trackedItem);
                        break;
                }

                return true;
            }
        });
        pm.show();

        return true;
    }

    private void armDevice(Activity activity, TrackedItem trackedItem) {
        SMSSender sender = new SMSSender();

        sender.sendMessage(
                activity,
                mTelephoneNumber,
                addPasswordToCommand(mArmCommand),
                String.format("Sending arm command to %s", trackedItem.getName()),
                trackedItem.getID(),
                ACTION_SENT_ARM);

    }

    private void disarmDevice(Activity activity, TrackedItem trackedItem) {
        SMSSender sender = new SMSSender();

        sender.sendMessage(
                activity,
                mTelephoneNumber,
                addPasswordToCommand(mDisarmCommand),
                String.format("Sending disarm command to %s", trackedItem.getName()),
                trackedItem.getID(),
                ACTION_SENT_DISARM);
    }
}
