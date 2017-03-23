package me.marcsymonds.tracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;

public class TrackedItemEntryActivity extends AppCompatPreferenceActivity {
    static final private String TAG = "TrackerItemEntryAct";

    // Name of the SharedPreferences file to use for editing a TrackedItem.
    static final private String TRACKED_ITEM_ENTRY_PREFS = "TrackedItemPrefs";
    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };
    private int mTrackedItemID = 0;

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                preference
                        .getPreferenceManager()
                        .getSharedPreferences()
                        .getString(preference.getKey(), ""));
        //PreferenceManager
        //.getDefaultSharedPreferences(preference.getContext())
        //.getString(preference.getKey(), ""));

    }

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
        }
        //else {
        //TrackedItem.clearSharedPreferences(sp);
        //}

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
            //trackedItem.getFromSharedPreferences(PreferenceManager.getDefaultSharedPreferences(this));
            trackedItem.getFromSharedPreferences(this.getApplicationContext().getSharedPreferences(TRACKED_ITEM_ENTRY_PREFS, MODE_PRIVATE));
            trackedItem.saveToFile();

            return trackedItem.getID();
        } else {
            return 0;
        }
    }

    public static class TrackedItemEntryFragment extends PreferenceFragment {
        private final static String TAG = "TrackedItemEntryFrag";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            try {
                PreferenceManager pm = getPreferenceManager();

                pm.setSharedPreferencesName(TRACKED_ITEM_ENTRY_PREFS);

                addPreferencesFromResource(R.xml.pref_tracked_item_entry);
                setHasOptionsMenu(false);

                bindControls(getPreferenceScreen());
            } catch (Exception ex) {
                Log.d(TAG, ex.toString());
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();

            return id == android.R.id.home || super.onOptionsItemSelected(item);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            boolean b = super.onPreferenceTreeClick(preferenceScreen, preference);

            Log.d(TAG, String.format("Preference Clicked: %s (%s) - %s (%s)", preferenceScreen.toString(), preferenceScreen.getClass().getName(), preference.toString(), preference.getClass().getName()));


            if (preference instanceof PreferenceScreen) {
                bindControls((PreferenceScreen) preference);
            }

            return b;
        }

        private void bindControls(PreferenceScreen preferenceScreen) {
            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.

            ListAdapter l = preferenceScreen.getRootAdapter();

            int prefCount = l.getCount();
            for (int i = 0; i < prefCount; i++) {
                Object prefItem = l.getItem(i);

                // Just binding ListPreference and EditTextPreference items.
                // Other types work automatically.
                if (prefItem instanceof ListPreference || prefItem instanceof EditTextPreference) {
                    bindPreferenceSummaryToValue((Preference) prefItem);
                }
            }
        }
    }
}
