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
import java.util.Locale;

public class TrackedItem {
    final static private String TI_ID = "id";

    // These values must match the android:key values in the pref_tracked_item_entry.xml file.
    // These values are also used when saving the record to file.
    final static private String TI_NAME = "tracked_item_name";
    final static private String TI_ENABLED = "tracked_item_enabled";
    //final static private String TI_TELEPHONE_COUNTRY_CODE = "tracked_item_telephone_country_code";
    //final static private String TI_TELEPHONE_NUMBER = "tracked_item_telephone_number";
    //final static private String TI_LOCATION_COMMAND = "tracked_item_ping_command";
    //final static private String TI_NUMBER_OF_RESPONSES = "tracked_item_expected_responses";
    //final static private String TI_SECONDS_BETWEEN_RESPONSES = "tracked_item_seconds_between_responses";
    //final static private String TI_AUTO_RESEND_COMMAND_SECONDS = "tracked_item_ping_interval";
    final static private String TI_COLOUR = "tracked_item_colour";
    final static private String TI_TYPE = "tracked_item_device_type";
    final private String TAG = "TrackedItem";
    //final static private String TI_PING = "tracked_item_auto_ping";
    private int mID = 0;

    //private Activity mActivity;
    private boolean mEnabled = false;
    private String mName = "";
    //private String mTelephoneCountryCode = "44";
    //private String mTelephoneNumber = "";
    //private String mLocationCommand = "";
    //private int mNumberOfResponses = 1;
    //private int mSecondsBetweenResponses = 30;
    //private int mAutoResendSeconds = 0;
    private int mColour = 0; // Colour is used for the map marker. The map uses hues for colouring, so this value is the hue (0-359) and is converted to an actual colour where needed.
    private String mTrackerDeviceType = "";
    private TrackerDevice mTrackerDevice = null;
    private File mSaveFile = null;
    //private boolean mPing = false;
    private File mHistoryDirectory;
    private HistoryManager mHistory;
    private TrackedItemButton mButton = null;
    private Marker mMapMarker = null;
    private boolean mSentPingRequest = false;

    // Control data for tracked item.
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
                        mID = Integer.parseInt(value);
                        break;

                    case TI_NAME:
                        mName = value;
                        break;

                    case TI_ENABLED:
                        mEnabled = (value.equals("1"));
                        break;

                    case TI_COLOUR:
                        mColour = Integer.parseInt(value);
                        break;

                    case TI_TYPE:
                        try {
                            setTrackerType(value);
                            /*mTrackerType = TrackedItemTypes.valueOf(value);

                            switch(mTrackerType) {
                                case TK103A:
                                    mTrackerDevice = new TK103AB();
                                    break;

                                default:
                                    Log.e(TAG, String.format("Code Error - Tracker type %s has not been coded for.", value));
                                    break;
                            }*/
                        } catch (IllegalArgumentException ae) {
                            Log.w(TAG, String.format("Unknown tracker type read from %s: %s", file.getAbsolutePath(), line));
                            mTrackerDeviceType = null;
                        }
                        break;

                    default:
                        if (mTrackerDevice == null || !mTrackerDevice.loadFromSave(name, value)) {
                            Log.w(TAG, String.format("Unexpected value read from %s: %s", file.getAbsolutePath(), line));
                        }
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

    HistoryManager getHistory() {
        return mHistory;
    }

    void setHistory() {
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
            writer.write(TI_COLOUR + ":" + mColour);
            writer.newLine();
            writer.write(TI_TYPE + ":" + mTrackerDeviceType);
            writer.newLine();

            if (mTrackerDevice != null) {
                mTrackerDevice.saveValuesToFile(writer);
            }

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

    void setEnabled(boolean enabled) {
        mEnabled = enabled;
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

    void setPingingButtonState(boolean on) {
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
    //mHistoryFileDirectory = getHistoryDir();
    //}

    String getName() {
        return mName;
    }

    void setName(String name) {
        mName = name;
    }

    int getColour() {
        return mColour;
    }

    /*String getTelephoneCountryCode() {
        return mTelephoneCountryCode;
    }*/

    /*public void setTelephoneCountryCode(String code) {
        mTelephoneNumber = code;
    }*/

    /*public String getTelephoneNumber() {
        return mTelephoneNumber;
    }*/

    /*public void setTelephoneNumber(String telephoneNumber) {
        mTelephoneNumber = telephoneNumber;
    }*/

    /*public String getLocationCommand() {
        return mLocationCommand;
    }*/

    /*public void setLocationCommand(String locationCommand) {
        mLocationCommand = locationCommand;
    }*/

    /*public int getNumberOfResponses() {
        return mNumberOfResponses;
    }*/

    /*public void setNumberOfResponses(int numberOfResponses) {
        mNumberOfResponses = numberOfResponses;
    }*/

    /*public int getSecondsBetweenResponses() {
        return mSecondsBetweenResponses;
    }*/

    /*public void setSecondsBetweenResponses(int secondsBetweenResponses) {
        mSecondsBetweenResponses = secondsBetweenResponses;
    }*/

    /*public int getAutoResendSeconds() {
        return mAutoResendSeconds;
    }*/

    /*public void setAutoResendSeconds(int autoResendSeconds) {
        mAutoResendSeconds = autoResendSeconds;
    }*/

    void setColour(int colour) {
        mColour = colour;
    }

    String getType() {
        return mTrackerDeviceType;
    }

    void setTrackerType(String trackerDeviceType) {
        if (!mTrackerDeviceType.equals(trackerDeviceType)) {
            mTrackerDeviceType = trackerDeviceType;
            try {
                String className = TrackedItems.getClassForTrackerDevice(trackerDeviceType);
                if (className != null) {
                    Log.d(TAG, String.format("Class for tracker device type %s is %s", trackerDeviceType, className));
                    mTrackerDevice = TrackerDevice.newInstance(className);
                } else {
                    Log.e(TAG, String.format("No could not match a class to tracker device type %s", trackerDeviceType));
                    mTrackerDevice = null;
                }
            } catch (Exception ex) {
                Log.e(TAG, String.format("Exception creating tracker device %s - %s", trackerDeviceType, ex.toString()));
                mTrackerDevice = null;
            }
        }
    }

    TrackerDevice getTrackerDevice() {
        return mTrackerDevice;
    }

    void putToSharedPreferences(SharedPreferences sp) {
        SharedPreferences.Editor edit = sp.edit();

        edit.clear();

        edit.putString(TI_NAME, mName);
        edit.putBoolean(TI_ENABLED, mEnabled);
        edit.putInt(TI_COLOUR, mColour);
        edit.putString(TI_TYPE, mTrackerDeviceType);

        edit.commit();

        if (mTrackerDevice != null) {
            mTrackerDevice.putToSharedPreferences(sp);
        }
    }

    /*public boolean getAutoResendCommand() {
        return mPing;
    }*/

    /*public void setAutoResendCommand(boolean ping) {
        mPing = ping;
    }*/

    void clearSharedPreferences(SharedPreferences sp) {
        SharedPreferences.Editor edit = sp.edit();

        edit.clear();

        edit.putString(TI_NAME, "");
        edit.putBoolean(TI_ENABLED, true);
        edit.putInt(TI_COLOUR, 0);
        edit.putString(TI_TYPE, mTrackerDeviceType);

        edit.commit();

        if (mTrackerDevice != null) {
            mTrackerDevice.clearSharedPreferences(sp);
        }
    }

    void getFromSharedPreferences(SharedPreferences sp) {
        mName = sp.getString(TI_NAME, mName);
        mEnabled = sp.getBoolean(TI_ENABLED, mEnabled);
        mColour = sp.getInt(TI_COLOUR, mColour);

        mTrackerDeviceType = sp.getString(TI_TYPE, mTrackerDeviceType);

        if (mTrackerDevice != null) {
            mTrackerDevice.getFromSharedPreferences(sp);
        }
    }

    public void sendPing(Activity activity) {
        SMSSender sender = new SMSSender();
        sender.sendPingMessage(activity, this);
    }

    //public int getNumberOfResponsesReceived() {
    //return mNumberOfResponsesReceived;
    //}

    //public void setNumberOfResponsesReceived(int responses) {
    //mNumberOfResponsesReceived = responses;
    //}

    //public boolean getSentPingRequest() {
    //return mSentPingRequest;
    //}

    //public void setSentPingRequest(boolean sent) {
    //mSentPingRequest = sent;
    //}
//endregion

    void newLocationReceived(Context context, Location location) {
        Log.d(TAG, String.format("New location for %s - %s", mName, location.toString()));

        mHistory.recordLocation(location);

        //++mNumberOfResponsesReceived;
        //if (mNumberOfResponsesReceived >= mNumberOfResponses) {
        //setPingingButtonState(false);
        //}

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

    enum TrackedItemTypes {
        UNKNOWN(0),
        TK103A(1);

        final int value;

        TrackedItemTypes(int id) {
            this.value = id;
        }
    }
}
