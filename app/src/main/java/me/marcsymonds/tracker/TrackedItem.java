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
import java.util.UUID;

public class TrackedItem {
    private final static String TI_COLOUR = "tracked_item_colour";
    private final static String TI_TYPE = "tracked_item_device_type";
    final static private String TI_ID = "id";
    final static private String TI_GUID = "guid";
    // These values must match the android:key values in the pref_tracked_item_entry.xml file.
    // These values are also used when saving the record to file.
    final static private String TI_NAME = "tracked_item_name";
    final static private String TI_ENABLED = "tracked_item_enabled";
    final private String TAG = "TrackedItem";

    private int mID = 0;
    private String mGUID = "";

    private boolean mEnabled = false;
    private String mName = "";

    // Colour is used for the map marker. The map uses hues for colouring, so this value is the
    // hue (0-359) and is converted to an actual colour where needed.
    private int mColour = 0;

    private String mTrackerDeviceType = "";
    private TrackerDevice mTrackerDevice = null;
    private File mSaveFile = null;
    private File mHistoryDirectory;
    private HistoryManager mHistory;
    private TrackedItemButton mButton = null;
    private Marker mMapMarker = null;
    private boolean mSentPingRequest = false;

    // Control data for tracked item.
    private int mNumberOfResponsesReceived = 0;
    TrackedItem() {
        mID = TrackedItems.getNextID();
        mGUID = UUID.randomUUID().toString();
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

                    case TI_GUID:
                        mGUID = value;
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
            writer.write(TI_GUID + ":" + mGUID);
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

            /*BufferedReader r = new BufferedReader(new FileReader(mSaveFile));
            String t = "";
            String l;
            l = r.readLine();
            while (l != null) {
                t = t + l + ", ";
                l = r.readLine();
            }
            r.close();
            Log.v(TAG, String.format("Saved data for %s : %s", mSaveFile.getAbsolutePath(), t));*/
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

    TrackedItemButton getButton(Activity activity, boolean updateAppearance) {
        if (mButton == null) {
            mButton = new TrackedItemButton(activity, this);
        } else if (updateAppearance) {
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

    String getGUID() {
        /*if (mGUID.length() == 0) {
            mGUID = UUID.randomUUID().toString();
            saveToFile();
        }*/

        return mGUID;
    }

    String getName() {
        return mName;
    }

    void setName(String name) {
        mName = name;
    }

    int getColour() {
        return mColour;
    }

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

    void sendPing(Activity activity) {
        if (mTrackerDevice != null) {
            mTrackerDevice.pingDevice(activity, this);
        }
    }
//endregion

    void newLocationReceived(Context context, Location location) {
        Log.d(TAG, String.format("New location for %s - %s", mName, location.toString()));

        mHistory.recordLocation(location);
        HistoryRecorder.recordHistory(location);

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
}
