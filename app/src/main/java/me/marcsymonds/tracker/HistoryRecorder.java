package me.marcsymonds.tracker;

import android.app.Activity;
import android.content.Context;

import java.io.File;

/**
 * Created by Marc on 06/04/2017.
 */

class HistoryRecorder {
    static private final String TAG = "HistoryRecorder";
    static private final String RECORDED_HISTORY_DIR = "RecordedHistory";

    static private File mHistoryDir;
    static private HistoryManager mHistoryManager;

    static void initialise(Activity activity) {
        mHistoryDir = activity.getDir(RECORDED_HISTORY_DIR, Context.MODE_PRIVATE);

        mHistoryManager = new HistoryManager(mHistoryDir, -1, 20, true);
    }

    static void tearDown(Activity activity) {
        mHistoryManager = null;
    }

    static synchronized void recordHistory(Location location) {
        if (mHistoryManager != null) {
            mHistoryManager.recordLocation(location);
        }
    }

    static HistoryManager getHistoryManager() {
        return mHistoryManager;
    }
}
