package me.marcsymonds.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Marc on 26/04/2017.
 */

class Pref {
    static final String PREF_HISTORY_UPLOAD_LOCAL_URL = "history_upload_local_url";
    static final String PREF_HISTORY_UPLOAD_REMOTE_URL = "history_upload_remote_url";

    static final String PREF_KEEP_SCREEN_ON = "keepScreenOn";

    static final String PREF_MY_LOCATION_UPDATE_INTERVAL = "my_location_update_interval";
    static final String PREF_MY_LOCATION_FASTEST_UPDATE_INTERVAL = "my_location_fastest_update_interval";

    static final String PREF_MY_LOCATION_BACKGROUND_UPDATES_ENABLED = "my_location_background_updates_enabled";
    static final String PREF_MY_LOCATION_BACKGROUND_UPDATE_INTERVAL = "my_location_background_update_interval";

    static final String PREF_BACKGROUND_LOCATION_UPDATE_LAST_GPS = "bglu_last_gps_update";
    static final String PREF_BACKGROUND_LOCATION_UPDATE_GPS_INTERVAL = "bglu_gps_update_interval";

    static boolean getKeepScreenOn(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_KEEP_SCREEN_ON, false);
    }

    static void setKeepScreenOn(Context context, boolean keepOn) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(PREF_KEEP_SCREEN_ON, keepOn);
        editor.apply();
    }
}
