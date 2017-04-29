package me.marcsymonds.tracker;

import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.ListAdapter;

import java.util.HashMap;

/**
 * Created by Marc on 09/04/2017.
 */

class PreferenceBinder {
    private final static String TAG = "PreferenceBinder";

    private HashMap<String, Preference.OnPreferenceChangeListener> mOtherListeners = null;

    private final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            boolean cont = true;
            String key = preference.getKey();

            if (mOtherListeners != null && mOtherListeners.containsKey(key)) {
                Log.d(TAG, "onPreferenceChange: Calling other Preference Change Listener for " + key);
                Preference.OnPreferenceChangeListener cl = mOtherListeners.get(key);
                cont = cl.onPreferenceChange(preference, value);
                Log.d(TAG, "onPreferenceChange: Result = " + Boolean.toString(cont));
            }

            if (cont) {
                String stringValue = value.toString();
                if (preference instanceof ListPreference) {
                    // For list preference_text, look up the correct display value in
                    // the preference's 'entries' list.
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);

                    // Set the summary to reflect the new value.
                    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
                } else {
                    // For all other preference_text, set the summary to the value's
                    // simple string representation.

                    // If the preference key contains the word "password", we'll assume it is a password
                    // and not display the actual value.
                    if (preference.getKey().contains("password") && stringValue != null && !stringValue.isEmpty()) {
                        stringValue = "********";
                    }

                    preference.setSummary(stringValue);
                }
            }

            return cont;
        }
    };

    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        Preference.OnPreferenceChangeListener cl = preference.getOnPreferenceChangeListener();
        if (cl != null && !cl.equals(sBindPreferenceSummaryToValueListener)) {
            if (mOtherListeners == null) {
                mOtherListeners = new HashMap<>();
            }

            String key = preference.getKey();

            if (mOtherListeners.containsKey(key)) {
                mOtherListeners.remove(key);
            }

            Log.d(TAG, "bindPreferenceSummaryToValue: Saving other Preference Change Listener for " + key);
            mOtherListeners.put(preference.getKey(), cl);
        }

        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's current value.

        preference.getOnPreferenceChangeListener().onPreferenceChange(preference,
                preference
                        .getPreferenceManager()
                        .getSharedPreferences()
                        .getString(preference.getKey(), ""));
    }

    /**
     * Bind all of the preference controls within the specified preference screen. This binding will
     * show the value entered for a preference as the summary of the preference.
     *
     * @param preferenceScreen
     */
    void bindControls(PreferenceScreen preferenceScreen) {
        bindControls(preferenceScreen, null);
    }

    /**
     * @param preferenceScreen
     * @param excludeKeys      list of preference item keys to not bind to.
     */
    void bindControls(PreferenceScreen preferenceScreen, String[] excludeKeys) {
        ListAdapter l = preferenceScreen.getRootAdapter();

        int prefCount = l.getCount();
        for (int i = 0; i < prefCount; i++) {
            Object obj = l.getItem(i);

            if ((obj instanceof EditTextPreference || obj instanceof ListPreference) && !isIn(((Preference) obj).getKey(), excludeKeys)) {
                bindPreferenceSummaryToValue((Preference) obj);
            }
        }
    }

    void bindControlsForGroup(PreferenceScreen preferenceScreen, String prefGroupName) {
        bindControlsForGroup(preferenceScreen, prefGroupName, null);
    }

    void bindControlsForGroup(PreferenceScreen preferenceScreen, String prefGroupName, String[] excludeKeys) {
        PreferenceGroup pg = (PreferenceGroup) preferenceScreen.findPreference(prefGroupName);
        if (pg != null) {
            int prefs = pg.getPreferenceCount();
            for (int i = 0; i < prefs; i++) {
                Object obj = pg.getPreference(i);

                if ((obj instanceof EditTextPreference || obj instanceof ListPreference) && !isIn(((Preference) obj).getKey(), excludeKeys)) {
                    bindPreferenceSummaryToValue((Preference) obj);
                }
            }
        }
    }

    void bindControl(Object obj) {
        if (obj instanceof EditTextPreference) {
            bindPreferenceSummaryToValue((Preference) obj);
        } else if (obj instanceof ListPreference) {
            bindPreferenceSummaryToValue((Preference) obj);
        }
    }

    private boolean isIn(String value, String[] values) {
        if (values == null) {
            return false;
        }

        for (int i = 0; i < values.length; i++) {
            if (value.equals(values[i])) {
                return true;
            }
        }

        return false;
    }
}
