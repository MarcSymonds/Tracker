package me.marcsymonds.tracker;

import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * Created by Marc on 06/04/2017.
 */

class HistoryRecorder {
    static private final String TAG = "HistoryRecorder";
    static private final String RECORDED_HISTORY_DIR = "RecordedHistory";

    private static HistoryRecorder mHistoryRecorder = null;

    private File mHistoryDir;
    private HistoryManager mHistoryManager;

    static synchronized HistoryRecorder getInstance(Context context) {
        if (mHistoryRecorder == null) {
            mHistoryRecorder = new HistoryRecorder();
            mHistoryRecorder.initialise(context.getApplicationContext());
        }

        return mHistoryRecorder;
    }

    private void initialise(Context context) {
        mHistoryDir = context.getDir(RECORDED_HISTORY_DIR, Context.MODE_PRIVATE);

        mHistoryManager = new HistoryManager(mHistoryDir, -1, 20, true);
    }

    synchronized void recordHistory(Location location) {
        Log.d(TAG, "recordHistory: " + location.toString());
        mHistoryManager.recordLocation(location);
    }

    HistoryManager getHistoryManager() {
        return mHistoryManager;
    }
}
