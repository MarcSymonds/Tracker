package me.marcsymonds.tracker;

import android.content.Context;
import android.util.Log;

import java.util.UUID;

/**
 *
 */
class ApplicationIdentifier {
    private static final String TAG = "ApplicationIdentifier";

    private static String mIdentifier = null;

    /**
     * Returns a GUID created for the application. This will be used when sending the tracker
     * history to the TrackerHistory web service to save the history.
     *
     * @param context
     * @return a string with the 36 character GUID; e.g. 8ba0949d-d8a6-4383-a51f-03bfbea9f327
     */
    static String getIdentifier(Context context) {
        if (mIdentifier == null) {
            try {
                DBStore dbStore = new DBStore(context);
                mIdentifier = dbStore.getAppID();
                if (mIdentifier == null) {
                    mIdentifier = UUID.randomUUID().toString();

                    dbStore.storeAppID(mIdentifier);
                }
            } catch (Exception ex) {
                Log.d(TAG, String.format("Exception getting application ID from DB: %s", ex.toString()));
            }
        }

        return mIdentifier;
    }
}
