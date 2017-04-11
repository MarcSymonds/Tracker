package me.marcsymonds.tracker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedWriter;
import java.io.IOException;

interface ITrackerDevice {
    void putToSharedPreferences(SharedPreferences sp);

    void getFromSharedPreferences(SharedPreferences sp);

    void clearSharedPreferences(SharedPreferences sp);

    boolean loadFromSave(String name, String value);

    void saveValuesToFile(BufferedWriter writer) throws IOException;

    boolean isMessageFor(String source);

    boolean messageReceived(Context context, TrackedItem trackedItem, String source, String message);

    void pingDevice(Activity activity, TrackedItem trackedItem);

    boolean messageSent(TrackedItem trackedItem, int action);

    boolean messageFailed(TrackedItem trackedItem, int action, int resultCode, String message);

    boolean openContextMenu(Activity activity, TrackedItem trackedItem);

    String getTelephoneNumber();
}
