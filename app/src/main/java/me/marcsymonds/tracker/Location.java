package me.marcsymonds.tracker;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

// NB. There is another class named Location at android.location.Location.

public class Location {
    private final static String TAG = "Location";

    private Date mDateTime;
    private double mLatitude;
    private double mLongitude;
    private boolean mGPS;

    Location () {
    }

    Location(String savedData) throws ParseException, IllegalArgumentException {
        String[] values = savedData.split("\\|");

        Log.d(TAG, String.format("Parsing location data: %s", savedData));

        if (values.length < 4)
            throw new IllegalArgumentException(String.format("Invalid Location data: %s", savedData));

        mDateTime = SimpleDateFormat.getDateTimeInstance().parse(values[0]);
        mLatitude = Double.parseDouble(values[1]);
        mLongitude = Double.parseDouble(values[2]);
        mGPS = (values[3] == "1");
    }

    Location(double latitude, double longitude, boolean gps) {
        this(Calendar.getInstance().getTime(), latitude, longitude, gps);
    }

    Location(Date dateTime, double latitude, double longitude, boolean gps) {
        mDateTime = dateTime;
        mLatitude = latitude;
        mLongitude = longitude;
        mGPS = gps;
    }

    Location(android.location.Location location) {
        this(location.getLatitude(), location.getLongitude(), (location.getAccuracy() > 0.0 && location.getAccuracy() < 10.0)); // If accuracy within 10 meters, assume GPS.
    }

    public String toString() {
        return String.format("%s|%f|%f|%d",
                SimpleDateFormat.getDateTimeInstance().format(mDateTime),
                mLatitude,
                mLongitude,
                mGPS ? 1 : 0);
    }

    public String snippetText() {
        String dt = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(mDateTime);

        return String.format(Locale.getDefault(), "%s\n%f, %f", dt, mLatitude, mLongitude);
    }

    public Date getDateTime() {
        return mDateTime;
    }

    public void setDateTime(Date dateTime) {
        mDateTime = dateTime;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    public boolean isGPS() {
        return mGPS;
    }

    public void setGPS(boolean gps) {
        mGPS = gps;
    }

    public LatLng getLatLng() {
        return new LatLng(mLatitude, mLongitude);
    }

}
