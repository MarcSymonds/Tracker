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
    private double mLatitude;
    private double mLongitude;
    private boolean mGPS;

    Location () {
    }

    Location(String savedData) throws ParseException, IllegalArgumentException {
        String[] values = savedData.split("\\|");
        int idx = 0;

        //Log.d(TAG, String.format("Parsing location data: %s", savedData));

        if (values.length < 4)
            throw new IllegalArgumentException(String.format("Invalid Location data: %s", savedData));

        if (values.length == 5) {
            mDevice = Integer.parseInt(values[idx]);
            ++idx;
        }

        mDateTime = SimpleDateFormat.getDateTimeInstance().parse(values[idx]);
        ++idx;
        mLatitude = Double.parseDouble(values[idx]);
        ++idx;
        mLongitude = Double.parseDouble(values[idx]);
        ++idx;
        mGPS = (values[idx].equals("1"));
        ++idx;
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

    public String toString() {
        return String.format(Locale.getDefault(), "%d|%s|%f|%f|%d",
                mDevice,
                SimpleDateFormat.getDateTimeInstance().format(mDateTime),
                mLatitude,
                mLongitude,
                mGPS ? 1 : 0);
    }

    /**
     * Generates the snippet text to be shown in the Map Marker info window.
     *
     * @return snippet text.
     */
    String snippetText() {
        String dt = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(mDateTime);

        return String.format(Locale.getDefault(), "%s\n%f, %f", dt, mLatitude, mLongitude);
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

    double getLongitude() {
        return mLongitude;
    }

    void setLongitude(double longitude) {
        mLongitude = longitude;
    }

    double getLatitude() {
        return mLatitude;
    }

    void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    boolean isGPS() {
        return mGPS;
    }

    void setGPS(boolean gps) {
        mGPS = gps;
    }

    LatLng getLatLng() {
        return new LatLng(mLatitude, mLongitude);
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
