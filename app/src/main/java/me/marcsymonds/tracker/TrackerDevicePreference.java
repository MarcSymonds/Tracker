package me.marcsymonds.tracker;

/**
 * Created by Marc on 15/03/2017.
 */

class TrackerDevicePreference {
    private String mName;
    private Object mValue;

    TrackerDevicePreference(String name, Object defaultValue) {
        mName = name;
        mValue = defaultValue;
    }

    String getName() {
        return mName;
    }

    Object getValue() {
        return mValue;
    }
}
