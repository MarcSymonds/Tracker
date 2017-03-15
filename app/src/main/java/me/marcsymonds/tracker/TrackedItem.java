package me.marcsymonds.tracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrackedItem {
    final static private String TAG = "TrackedItem";

    // These values must match the android:key values in the pref_tracked_item_entry.xml file.
    // These values are also used when saving the record to file.

    final static private String TI_ID = "id";
    final static private String TI_NAME = "tracked_item_name";
    final static private String TI_ENABLED = "tracked_item_enabled";
    final static private String TI_TELEPHONE_COUNTRY_CODE = "tracked_item_telephone_country_code";
    final static private String TI_TELEPHONE_NUMBER = "tracked_item_telephone_number";
    final static private String TI_LOCATION_COMMAND = "tracked_item_ping_command";
    final static private String TI_NUMBER_OF_RESPONSES = "tracked_item_expected_responses";
    final static private String TI_SECONDS_BETWEEN_RESPONSES = "tracked_item_seconds_between_responses";
    final static private String TI_AUTO_RESEND_COMMAND_SECONDS = "tracked_item_ping_interval";
    final static private String TI_COLOUR = "tracked_item_colour";
    final static private String TI_TYPE = "tracked_item_device_type";
    final static private String TI_PING = "tracked_item_auto_ping";

    enum TrackedItemTypes {
        UNKNOWN (0),
        TK103A (1);

        final int value;

        TrackedItemTypes(int id) {
            this.value = id;
        }
    }

    //private Activity mActivity;

    private int mID = 0;
    private boolean mEnabled = false;
    private String mName = "";
    private String mTelephoneCountryCode = "44";
    private String mTelephoneNumber = "";
    private String mLocationCommand = "";
    private int mNumberOfResponses = 1;
    private int mSecondsBetweenResponses = 30;
    private int mAutoResendSeconds = 0;
    private int mColour = 0; // Colour is used for the map marker. The map uses hues for colouring, so this value is the hue (0-359) and is converted to an actual colour where needed.
    private TrackedItemTypes mTrackerType = TrackedItemTypes.UNKNOWN;
    private boolean mPing = false;

    private File mSaveFile = null;

    private File mHistoryDirectory;
    private HistoryManager mHistory;

    private TrackedItemButton mButton = null;
    private Marker mMapMarker = null;

    // Control data for tracked item.

    private boolean mSentPingRequest = false;
    private int mNumberOfResponsesReceived = 0;

    TrackedItem() {
        mID = TrackedItems.getNextID();
        setHistory();
    }

    /**
     * Construct a TrackedItem object from a file.
     *
     * @param file a File object specifying the file to load.
     * @throws FileNotFoundException Exception if the file does not exist.
     */
    TrackedItem(File file) throws IOException {
        String line, name, value;
        int idx;

        mSaveFile = file;

        Log.d(TAG, String.format("Loading TrackedItem from %s", file.getAbsolutePath()));

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            line = reader.readLine();
            while (line != null) {
                idx = line.indexOf(":");
                name = line.substring(0, idx);
                value = line.substring(idx + 1);

                switch (name) {
                    case TI_ID:
                        // Use setID because it will set the history directory.
                        mID = Integer.parseInt(value);
                        break;

                    case TI_NAME:
                        mName = value;
                        break;

                    case TI_ENABLED:
                        mEnabled = (value.equals("1"));
                        break;

                    case TI_TELEPHONE_NUMBER:
                        mTelephoneNumber = value;
                        break;

                    case TI_TELEPHONE_COUNTRY_CODE:
                        mTelephoneCountryCode = value;
                        break;

                    case TI_LOCATION_COMMAND:
                        mLocationCommand = value;
                        break;

                    case TI_NUMBER_OF_RESPONSES:
                        mNumberOfResponses = Integer.parseInt(value);
                        break;

                    case TI_SECONDS_BETWEEN_RESPONSES:
                        mSecondsBetweenResponses = Integer.parseInt(value);
                        break;

                    case TI_AUTO_RESEND_COMMAND_SECONDS:
                        mAutoResendSeconds = Integer.parseInt(value);
                        break;

                    case TI_COLOUR:
                        mColour = Integer.parseInt(value);
                        break;

                    case TI_TYPE:
                        try {
                            mTrackerType = TrackedItemTypes.valueOf(value);
                        } catch (IllegalArgumentException ae) {
                            Log.w(TAG, String.format("Unknown tracker type read from %s: %s", file.getAbsolutePath(), line));
                            mTrackerType = TrackedItemTypes.UNKNOWN;
                        }
                        break;

                    case TI_PING:
                        mPing = (value.equals("1"));
                        break;

                    default:
                        Log.w(TAG, String.format("Unexpected value read from %s: %s", file.getAbsolutePath(), line));
                        break;
                }

                line = reader.readLine();
            }

            reader.close();

            setHistory();
        }
        catch (IOException ex) {
            Log.e(TAG, String.format("IOException reading file %s - %s", file.getAbsolutePath(), ex.toString()));
            throw ex;
        }
    }

    private void setHistory() {
        mHistoryDirectory = new File(TrackedItems.getTrackedItemsHistoryDir(), String.valueOf(mID));
        mHistory = new HistoryManager(mHistoryDirectory);
    }

    void saveToFile() {
        if (mSaveFile == null) {
            mSaveFile = new File(TrackedItems.getTrackedItemsSaveDir(), String.format(Locale.getDefault(), "%s.%d", TrackedItems.TRACKED_ITEM_FILE_PREFIX, mID));
        }

        Log.d(TAG, String.format("Saving TrackedItem %s (%d) to %s", mName, mID, mSaveFile.getAbsolutePath()));

        try {
            if (mSaveFile.exists()) {
                mSaveFile.delete();
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(mSaveFile));

            writer.write(TI_ID + ":" + mID);
            writer.newLine();
            writer.write(TI_NAME + ":" + mName);
            writer.newLine();
            writer.write(TI_ENABLED + ":" + (mEnabled ? "1" : "0"));
            writer.newLine();
            writer.write(TI_TELEPHONE_NUMBER + ":" + mTelephoneNumber);
            writer.newLine();
            writer.write(TI_TELEPHONE_COUNTRY_CODE + ":" + mTelephoneCountryCode);
            writer.newLine();
            writer.write(TI_LOCATION_COMMAND + ":" + mLocationCommand);
            writer.newLine();
            writer.write(TI_NUMBER_OF_RESPONSES + ":" + mNumberOfResponses);
            writer.newLine();
            writer.write(TI_SECONDS_BETWEEN_RESPONSES + ":" + mSecondsBetweenResponses);
            writer.newLine();
            writer.write(TI_AUTO_RESEND_COMMAND_SECONDS + ":" + mAutoResendSeconds);
            writer.newLine();
            writer.write(TI_COLOUR + ":" + mColour);
            writer.newLine();
            writer.write(TI_TYPE + ":" + mTrackerType);
            writer.newLine();
            writer.write(TI_PING + ":" + (mPing ? "1" : "0"));
            writer.newLine();

            writer.flush();
            writer.close();

            /*BufferedReader r = new BufferedReader(new FileReader(file));
            String t = "";
            String l;
            l = r.readLine();
            while (l != null) {
                t = t + l;
                l = r.readLine();
            }
            r.close();
            Log.v(TAG, String.format("Saved data for %s : %s", file.getAbsolutePath(), t));*/
        }
        catch (IOException io) {
            Log.e(TAG, String.format("IOException writing file %s - %s", mSaveFile.getAbsolutePath(), io.toString()));
        }
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    TrackedItemButton getButton(Activity activity) {
        if (mButton == null) {
            mButton = new TrackedItemButton(activity, this);
        }
        else {
            mButton.setButtonAppearance();
        }

        return mButton;
    }

    HistoryManager getHistory() {
        return mHistory;
    }

    Location getLastLocation() {
        if (mHistory != null) {
            return mHistory.getLastLocation();
        }
        else {
            return null;
        }
    }

    Marker createMapMarker(GoogleMap map) {
        return createMapMarker(map, getLastLocation());
    }

    Marker createMapMarker(GoogleMap map, Location location) {
        if (mMapMarker == null && location != null) {
            MarkerOptions options = new MarkerOptions()
                    .position(location.getLatLng())
                    .icon(BitmapDescriptorFactory.defaultMarker(mColour))
                    .title(mName)
                    .snippet(location.snippetText());

            mMapMarker = map.addMarker(options);
        }

        return mMapMarker;
    }

    void updateMapMarkerLocation(Location location) {
        if (mMapMarker != null && location != null) {
            mMapMarker.setPosition(location.getLatLng());
            mMapMarker.setSnippet(location.snippetText());

            refreshMapMarkerInfoWindow();
        }
    }

    void updateMapMarkerInfo() {
        if (mMapMarker != null) {
            Log.d(TAG, String.format("Setting marking info %s %d", mName, mColour));

            mMapMarker.setTitle(mName);
            mMapMarker.setIcon(BitmapDescriptorFactory.defaultMarker(mColour));

            refreshMapMarkerInfoWindow();
        }
    }

    private void refreshMapMarkerInfoWindow() {
        if (mMapMarker.isInfoWindowShown()) {
            mMapMarker.hideInfoWindow();
            mMapMarker.showInfoWindow();
        }
    }

    boolean removeMapMarker() {
        boolean removed = false;

        if (mMapMarker != null) {
            mMapMarker.remove();
            mMapMarker = null;

            removed = true;
        }

        return removed;
    }

    boolean hasMapMarker() {
        return (mMapMarker != null);
    }

    void setFollowingButtonState(boolean on) {
        if (mButton != null) {
            mButton.setFollowingImage(on);
        }
    }

    private void setPingingButtonState(boolean on) {
        if (mButton != null) {
            mButton.setPingingImage(on);
        }
    }

    //region STANDARD GETTERS AND SETTERS
    int getID() {
        return mID;
    }

    //void setID(int id) {
        //mID = id;
        //setHistory();
    //}

    void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    String getName() {
        return mName;
    }

    void setName(String name) {
        mName = name;
    }

    String getTelephoneCountryCode() {
        return mTelephoneCountryCode;
    }

    public void setTelephoneCountryCode(String code) {
        mTelephoneNumber = code;
    }

    public String getTelephoneNumber() {
        return mTelephoneNumber;
    }

    public void setTelephoneNumber(String telephoneNumber) {
        mTelephoneNumber = telephoneNumber;
    }

    public String getLocationCommand() {
        return mLocationCommand;
    }

    public void setLocationCommand(String locationCommand) {
        mLocationCommand = locationCommand;
    }

    public int getNumberOfResponses() {
        return mNumberOfResponses;
    }

    public void setNumberOfResponses(int numberOfResponses) {
        mNumberOfResponses = numberOfResponses;
    }

    public int getSecondsBetweenResponses() {
        return mSecondsBetweenResponses;
    }

    public void setSecondsBetweenResponses(int secondsBetweenResponses) {
        mSecondsBetweenResponses = secondsBetweenResponses;
    }

    public int getAutoResendSeconds() {
        return mAutoResendSeconds;
    }

    public void setAutoResendSeconds(int autoResendSeconds) {
        mAutoResendSeconds = autoResendSeconds;
    }

    public int getColour() {
        return mColour;
    }

    public void setColour(int colour) {
        mColour = colour;
    }

    public TrackedItemTypes getType() {
        return mTrackerType;
    }

    public void setType(TrackedItemTypes type) {
        mTrackerType = type;
    }

    public boolean getAutoResendCommand() {
        return mPing;
    }

    public void setAutoResendCommand(boolean ping) {
        mPing = ping;
    }

    void putToSharedPreferences(SharedPreferences sp) {
        SharedPreferences.Editor edit = sp.edit();

        //edit.clear();
        edit.putString(TI_NAME, mName);
        edit.putBoolean(TI_ENABLED, mEnabled);
        edit.putInt(TI_COLOUR, mColour);
        edit.putString(TI_TYPE, mTrackerType.toString());
        edit.putString(TI_TELEPHONE_COUNTRY_CODE, mTelephoneCountryCode);
        edit.putString(TI_TELEPHONE_NUMBER, mTelephoneNumber);
        edit.putString(TI_LOCATION_COMMAND, mLocationCommand);
        edit.putString(TI_NUMBER_OF_RESPONSES, String.valueOf(mNumberOfResponses));
        edit.putString(TI_SECONDS_BETWEEN_RESPONSES, String.valueOf(mSecondsBetweenResponses));
        edit.putBoolean(TI_PING, mPing);
        edit.putString(TI_AUTO_RESEND_COMMAND_SECONDS, String.valueOf(mAutoResendSeconds));
        edit.commit();
        //edit.apply();
    }

    static void clearSharedPreferences(SharedPreferences sp) {
        SharedPreferences.Editor edit = sp.edit();

        edit.clear();

        /*edit.putString(TI_NAME, "");
        edit.putBoolean(TI_ENABLED, true);
        edit.putInt(TI_COLOUR, 0);
        edit.putString(TI_TYPE, TrackedItemTypes.TK103A.toString());
        edit.putString(TI_TELEPHONE_COUNTRY_CODE, "");
        edit.putString(TI_TELEPHONE_NUMBER, "");
        edit.putString(TI_LOCATION_COMMAND, "");
        edit.putString(TI_NUMBER_OF_RESPONSES, "1");
        edit.putString(TI_SECONDS_BETWEEN_RESPONSES, "30");
        edit.putBoolean(TI_PING, false);
        edit.putString(TI_AUTO_RESEND_COMMAND_SECONDS, "60");*/
        edit.commit();
        //edit.apply();
    }

    void getFromSharedPreferences(SharedPreferences sp) {
        mName = sp.getString(TI_NAME, mName);
        mEnabled = sp.getBoolean(TI_ENABLED, mEnabled);
        mColour = sp.getInt(TI_COLOUR, mColour);
        mTrackerType = TrackedItemTypes.valueOf(sp.getString(TI_TYPE, mTrackerType.toString()));
        mTelephoneCountryCode = sp.getString(TI_TELEPHONE_COUNTRY_CODE, mTelephoneCountryCode);
        mTelephoneNumber = sp.getString(TI_TELEPHONE_NUMBER, mTelephoneNumber);
        mLocationCommand = sp.getString(TI_LOCATION_COMMAND, mLocationCommand);
        mNumberOfResponses = Integer.parseInt(sp.getString(TI_NUMBER_OF_RESPONSES, String.valueOf(mNumberOfResponses)));
        mSecondsBetweenResponses = Integer.parseInt(sp.getString(TI_SECONDS_BETWEEN_RESPONSES, String.valueOf(mSecondsBetweenResponses)));
        mPing = sp.getBoolean(TI_PING, mPing);
        mAutoResendSeconds = Integer.parseInt((sp.getString(TI_AUTO_RESEND_COMMAND_SECONDS, String.valueOf(mAutoResendSeconds))));
    }

    public int getNumberOfResponsesReceived() {
        return mNumberOfResponsesReceived;
    }

    public void setNumberOfResponsesReceived(int responses) {
        mNumberOfResponsesReceived = responses;
    }

    public boolean getSentPingRequest() {
        return mSentPingRequest;
    }

    public void setSentPingRequest(boolean sent) {
        mSentPingRequest = sent;
    }
//endregion

    public void sendPing(Activity activity) {
        SMSSender sender = new SMSSender();
        sender.sendPingMessage(activity, this);
    }

    public void pingSent() {
        mSentPingRequest = true;
        mNumberOfResponsesReceived = 0;

        setPingingButtonState(true);

        //TODO: Set up alarm to resend the ping after specified time.
    }

    void pingFailed(int code, String message) {
        setPingingButtonState(false);
    }

    public void messageReceived(Context context, String message) {
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

            TODO: Possible lookup LAC/CID somehow.
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
        }
        else {
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
            newLocationReceived(context, new Location(lat, lng, gps));
        }
        //switch(mTrackerType) {
//            case TK103A:
//        }
    }

    private void newLocationReceived(Context context, Location location) {
        Log.d(TAG, String.format("New location for %s - %s", mName, location.toString()));

        mHistory.recordLocation(location);

        ++mNumberOfResponsesReceived;
        if (mNumberOfResponsesReceived >= mNumberOfResponses) {
            setPingingButtonState(false);
        }

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
        Intent event = new Intent("TRACKER-EVENT");
        event.putExtra("EVENT", "TRACKED-ITEM-LOCATION-UPDATE");
        event.putExtra("TRACKED-ITEM", mID);
        event.putExtra("LOCATION", location.toString());
        lbm.sendBroadcast(event);
    }

    void deleteHistory() {
        if (mHistory != null) {
            mHistory.deleteAllHistory();
        }
    }

    void saveHistory() {
        if (mHistory != null) {
            mHistory.saveHistory();
        }
    }
}
