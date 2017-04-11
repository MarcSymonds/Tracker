package me.marcsymonds.tracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class TrackedItemEntryActivity extends AppCompatPreferenceActivity {
    static final private String TAG = "TrackerItemEntryAct";

    // Name of the SharedPreferences file to use for editing a TrackedItem.
    static final private String TRACKED_ITEM_ENTRY_PREFS = "TrackedItemPrefs";

    static final private String KEY_TRACKED_ITEM_DEVICE_TYPE = "tracked_item_device_type";
    static final private String KEY_TRACKED_DEVICE_SETTINGS_GROUP = "device_specific_settings";

    private int mTrackedItemID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = this.getSharedPreferences(TRACKED_ITEM_ENTRY_PREFS, MODE_PRIVATE);
        /*
        // Output all of the shared preferences.

        Map<String, ?> x =  sp.getAll();

        for(Map.Entry<String,?> entry : x.entrySet()){
            Log.d("map values",entry.getKey() + ": " +
                    (entry.getValue() == null ? "NULL" : entry.getValue()) + " - " + entry.getValue().getClass().toString());
        }*/

        mTrackedItemID = getIntent().getExtras().getInt("ID");

        if (mTrackedItemID > 0) {
            TrackedItem trackedItem = TrackedItems.getItemByID(mTrackedItemID);
            if (trackedItem != null) {
                trackedItem.putToSharedPreferences(sp);
            }
        } else {
            sp.edit().clear().commit();
        }

        setContentView(R.layout.activity_tracked_item_entry);
        setupActionBar();
    }

        /*
    Replaced this with the "onOptionsItemSelected" method below. This function didn't work on my
    GT-N8010; when clicking the Up button, it didn't go back to the parent activity but just seemed
    to reload this activity. It did seem to work on the other devices though.

    This also required a change when starting this activity; instead of using "startActivity(...)" I
    used "startActivityForResult(...)" instead.
    */

    /*
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        Log.d(TAG, String.format("onMenuItemSelected %d", id));
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tracked_item_entry, menu);
        return true;
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        Log.d(TAG, String.format("setUpActionBar: %s", actionBar == null ? "NULL" : "NOT NULL"));
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Log.d(TAG, String.format("onOptionsItemSelected %d", id));

        switch (id) {
            case android.R.id.home:
                setResult(0);
                finish();
                break;

            case R.id.menu_tracked_item_entry_done:
                int ti = saveTrackedItem();
                setResult(ti);
                finish();
                break;

            default:
                return false;
        }

        return true;
    }

    private int saveTrackedItem() {
        TrackedItem trackedItem;

        if (mTrackedItemID > 0) {
            trackedItem = TrackedItems.getItemByID(mTrackedItemID);
        } else {
            trackedItem = new TrackedItem();
            TrackedItems.add(trackedItem);
        }

        if (trackedItem != null) {
            SharedPreferences sp = getApplicationContext().getSharedPreferences(TRACKED_ITEM_ENTRY_PREFS, MODE_PRIVATE);

            // Update the tracker device type.
            trackedItem.setTrackerType(sp.getString(KEY_TRACKED_ITEM_DEVICE_TYPE, ""));

            // Get the preferences from Shared Preferences.
            trackedItem.getFromSharedPreferences(sp);

            // Save the preferences to file.
            trackedItem.saveToFile();

            return trackedItem.getID();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isValidFragment(String fragmentName) {
        return fragmentName.equals(TrackedItemEntryFragment.class.getName());
        //|| fragmentName.equals(TrackerDeviceEntryFragment.class.getName());
    }

    public static class TrackedItemEntryFragment extends PreferenceFragment {
        private final static String TAG = "TrackedItemEntryFrag";

        private final static PreferenceBinder mPreferenceBinder = new PreferenceBinder();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            try {
                // Make sure PreferenceManager is using the right SharedPreferences file.
                PreferenceManager pm = getPreferenceManager();
                pm.setSharedPreferencesName(TRACKED_ITEM_ENTRY_PREFS);

                // Add the base "Tracked Item" prefererences to the preferences.
                addPreferencesFromResource(R.xml.pref_tracked_item_entry);

                // and bind the controls to their summary values, so the user can see the current
                // value of each preference.
                // Ignore the tracked item device type, we handle that specially below...
                mPreferenceBinder.bindControls(getPreferenceScreen(), new String[]{KEY_TRACKED_ITEM_DEVICE_TYPE});

                // The "Device Type" preference needs special attention. If the device type is
                // changed, then we need to show the preferences for that device type.
                ListPreference p = (ListPreference) findPreference(KEY_TRACKED_ITEM_DEVICE_TYPE);
                if (p != null) {
                    // Change listener for the device type.
                    // If the device type is changed, then load the preferences for that device
                    // type.
                    p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object o) {
                            ListPreference lp = (ListPreference) preference;

                            int index = lp.findIndexOfValue(o.toString());

                            String name = index >= 0 ? lp.getEntries()[index].toString() : null;
                            String code = index >= 0 ? lp.getEntryValues()[index].toString() : null;

                            // Set the summary to reflect the new value.
                            preference.setSummary(name);
                            Log.d(TAG, String.format("LIST CHANGE: %s - %s - %s - %s - %s - %s", o, code, name, lp.getEntry(), lp.getValue(), lp.getKey()));

                            // Add the preferences for this device type. The preferences for the
                            // current device type are removed.
                            addDevicePreferences(code);

                            // Bind the controls for the device preferences, so that the value of
                            // each preference is shown.
                            mPreferenceBinder.bindControlsForGroup(getPreferenceScreen(), KEY_TRACKED_DEVICE_SETTINGS_GROUP, null);

                            // Acceot the change.
                            return true;
                        }
                    });

                    // Fire the "Device Type" changed event.
                    p.getOnPreferenceChangeListener().onPreferenceChange(p, p.getValue());
                } else {
                    Log.d(TAG, "Not found device type");
                }

                setHasOptionsMenu(true);
            } catch (Exception ex) {
                Log.d(TAG, ex.toString());
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();

            return id == android.R.id.home || super.onOptionsItemSelected(item);
        }

        private void addDevicePreferences(String deviceTypeName) {
            PreferenceScreen ps = getPreferenceScreen();

            // Find the existing "device_specific_settings" group.
            Preference pc = findPreference(KEY_TRACKED_DEVICE_SETTINGS_GROUP);
            // and remove it.
            if (pc != null && pc instanceof PreferenceCategory) {
                ps.removePreference(pc);
            }

            // Get the resource ID of the preferences XML file for the new device type.
            int resID = getResources().getIdentifier("pref_tracker_device_" + deviceTypeName.toLowerCase(), "xml", "me.marcsymonds.tracker");
            Log.d(TAG, String.format("RESOURCE ID: %d", resID));
            // and add those preferences to the form.
            if (resID > 0) {
                addPreferencesFromResource(resID);
            }
        }
    }
}
