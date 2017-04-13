package me.marcsymonds.tracker;

import com.google.android.gms.maps.model.LatLng;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

// NB. There is another class named Location at android.location.Location.

class Location {
    private final static String TAG = "Location";

    private static final double EARTH_RADIUS_KM = 6371;

    private int mDevice;
    private Date mDateTime;
    private double mLatitude = 0.0;
    private double mLongitude = 0.0;
    private boolean mGPS = false;
    private String mLAC = null;
    private String mCID = null;
    private Double mLastKnownLatitude = 0.0;
    private Double mLastKnownLongitude = 0.0;
    private String mMessage = null;

    Location () {
    }

    Location(String savedData) throws ParseException, IllegalArgumentException {
        String[] values = savedData.split("\\|", -1); // -1 otherwise split will discard empty strings at end.
        int idx = 0;

        //Log.d(TAG, String.format("Parsing location data: %s", savedData));

        if (values.length < 4)
            throw new IllegalArgumentException(String.format("Invalid Location data: %s", savedData));

        if (values.length >= 5) {
            mDevice = Integer.parseInt(values[idx++]);
        }

        mDateTime = SimpleDateFormat.getDateTimeInstance().parse(values[idx++]);
        mLatitude = Double.parseDouble(values[idx++]);
        mLongitude = Double.parseDouble(values[idx++]);
        mGPS = (values[idx++].equals("1"));

        if (idx < values.length) {
            mLAC = values[idx].length() == 0 ? null : values[idx];
            ++idx;
            mCID = values[idx].length() == 0 ? null : values[idx];
            ++idx;
            mLastKnownLatitude = Double.parseDouble(values[idx++]);
            mLastKnownLongitude = Double.parseDouble(values[idx++]);
            mMessage = values[idx].length() == 0 ? null : values[idx];
            ++idx;
        }
    }

    Location(int device, double latitude, double longitude, boolean gps) {
        this(device, Calendar.getInstance().getTime(), latitude, longitude, gps);
    }

    private Location(int device, Date dateTime, double latitude, double longitude, boolean gps) {
        mDevice = device;
        mDateTime = dateTime;
        mLatitude = latitude;
        mLongitude = longitude;
        mGPS = gps;
    }

    Location(int device, android.location.Location location) {
        this(device, location.getLatitude(), location.getLongitude(), (location.getAccuracy() > 0.0 && location.getAccuracy() < 5.0)); // If accuracy within 5 meters, assume GPS.
    }

    Location(int device) {
        mDevice = device;
        mDateTime = Calendar.getInstance().getTime();
    }

    /**
     * This is used to save the data to file, so needs to include everything that needs to be saved.
     *
     * @return
     */
    public String toString() {
        return String.format(Locale.getDefault(), "%d|%s|%f|%f|%d|%s|%s|%f|%f|%s",
                mDevice,
                SimpleDateFormat.getDateTimeInstance().format(mDateTime),
                mLatitude,
                mLongitude,
                mGPS ? 1 : 0,
                mLAC == null ? "" : mLAC,
                mCID == null ? "" : mCID,
                mLastKnownLatitude,
                mLastKnownLongitude,
                mMessage == null ? "" : mMessage);
    }

    boolean hasLocation() {
        return mLongitude != 0.0 || mLatitude != 0.0;
    }

    boolean hasLastKnownLocation() {
        return (mLastKnownLongitude != 0.0 && mLastKnownLongitude != mLongitude)
                || (mLastKnownLatitude != 0.0 && mLastKnownLatitude != mLatitude);
    }

    boolean hasAdditionalInfo() {
        return
                mLAC != null
                        || mCID != null
                        || (mLastKnownLatitude != 0.0 && mLastKnownLatitude != mLatitude)
                        || (mLastKnownLongitude != 0.0 && mLastKnownLongitude != mLongitude)
                        || mMessage != null;
    }
    /**
     * Generates the snippet text to be shown in the Map Marker info window.
     *
     * @return snippet text.
     */
    String snippetText() {
        String dt = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(mDateTime);

        if (hasLocation()) {
            return String.format(Locale.getDefault(), "%s\n%f, %f", dt, mLatitude, mLongitude);
        } else if (hasLastKnownLocation()) {
            return String.format(Locale.getDefault(), "%s\n%f, %f", dt, mLastKnownLatitude, mLastKnownLongitude);
        } else {
            return String.format(Locale.getDefault(), "%s", dt);
        }
    }

    int getDevice() {
        return mDevice;
    }

    Date getDateTime() {
        return mDateTime;
    }

    void setDateTime(Date dateTime) {
        mDateTime = dateTime;
    }

    void setLocation(double latitude, double longitude) {
        mLongitude = longitude;
        mLatitude = latitude;
    }

    double getLongitude() {
        return mLongitude;
    }

    double getLatitude() {
        return mLatitude;
    }

    void setLastKnownLocation(double latitiude, double longitude) {
        mLastKnownLongitude = longitude;
        mLastKnownLatitude = latitiude;
    }

    double getLastKnownLongitude() {
        return mLastKnownLongitude;
    }

    double getLastKnownLatitude() {
        return mLastKnownLatitude;
    }

    void setLACCID(String lac, String cid) {
        mLAC = lac;
        mCID = cid;
    }

    String getLAC() {
        return mLAC;
    }

    String getCID() {
        return mCID;
    }

    String getMessage() {
        return mMessage;
    }

    void setMessage(String message) {
        mMessage = message.replace("|", "!");
    }

    boolean isGPS() {
        return mGPS;
    }

    void setGPS(boolean gps) {
        mGPS = gps;
    }

    LatLng getLatLng() {
        if (hasLocation()) {
            return new LatLng(mLatitude, mLongitude);
        } else {
            return new LatLng(mLastKnownLatitude, mLastKnownLongitude);
        }
    }

    /**
     * Calculate the distance, in meters, between this location and the passed location.
     * Uses the Haversine formula.
     *
     * @param targetLocation location to calculate distance to.
     * @return distance to target location in meters.
     */
    double distanceTo(Location targetLocation) {
        double diffLat = Math.toRadians(targetLocation.getLatitude() - mLatitude);
        double diffLong = Math.toRadians(targetLocation.getLongitude() - mLongitude);
        double myLat = Math.toRadians(mLatitude);
        double trgLat = Math.toRadians(targetLocation.getLatitude());

        double a = Math.pow(Math.sin(diffLat / 2), 2)
                + Math.pow(Math.sin(diffLong / 2), 2)
                * Math.cos(myLat) * Math.cos(trgLat);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c * 1000;
    }
}
