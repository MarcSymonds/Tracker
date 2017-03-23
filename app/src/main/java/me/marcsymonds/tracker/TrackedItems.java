package me.marcsymonds.tracker;


import android.app.Activity;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Used for managing the list of tracked items.
 */
class TrackedItems {
    final static String TRACKED_ITEM_FILE_PREFIX = "TrackedItem";
    final static private String TAG = "TrackedItems";
    final static private String TRACKED_ITEMS_DIR = "TrackedItems";
    final static private String TRACKED_ITEMS_HISTORY_DIR = "TrackedItemsHistory";

    //public static Activity AppActivity = null;
    private static final ArrayList<TrackedItem> mItems = new ArrayList<>();
    private static File mTrackedItemsSaveDir;
    private static File mTrackedItemsHistoryDir;
    private static int mHighestID = 0;
    private static String[] mTrackerDeviceValues = null;
    private static String[] mTrackerDeviceClasses = null;

    static void initialise(Activity activity) {
        mTrackedItemsSaveDir = activity.getDir(TRACKED_ITEMS_DIR, 0);
        mTrackedItemsHistoryDir = activity.getDir(TRACKED_ITEMS_HISTORY_DIR, 0);

        mTrackerDeviceValues = activity.getResources().getStringArray(R.array.tracker_device_values);
        mTrackerDeviceClasses = activity.getResources().getStringArray(R.array.tracker_device_classes);

        loadTrackedItems(activity);
    }

    static int getHighestID() {
        return mHighestID;
    }

    static void setHighestID(int highestID) {
        mHighestID = highestID;
    }

    static File getTrackedItemsSaveDir() {
        return mTrackedItemsSaveDir;
    }

    static File getTrackedItemsHistoryDir() {
        return mTrackedItemsHistoryDir;
    }

    static String getClassForTrackerDevice(String trackerDeviceType) {
        String className = null;

        for (int i = 0; i < mTrackerDeviceValues.length; i++) {
            if (trackerDeviceType.equals(mTrackerDeviceValues[i])) {
                className = mTrackerDeviceClasses[i];
                break;
            }
        }

        return className;
    }

    private static int loadTrackedItems(Activity activity) {
        //File dir = activity.getDir(TRACKED_ITEMS_DIR, 0);

        Log.d(TAG, String.format("Loading tracked items from %s", mTrackedItemsSaveDir.getAbsolutePath()));

        mItems.clear();

        for (File file : mTrackedItemsSaveDir.listFiles()) {
            String fileName = file.getName();
            if (fileName.startsWith(TRACKED_ITEM_FILE_PREFIX)) {
                int ID = Integer.parseInt(fileName.substring(TRACKED_ITEM_FILE_PREFIX.length() + 1));
                try {
                    TrackedItem item = new TrackedItem(file);
                    mItems.add(item);

                    if (ID > mHighestID) {
                        mHighestID = ID;
                    }

                    item.setHistory();
                }
                catch (FileNotFoundException nf) {
                    Log.e(TAG, String.format("Tracked item file %s not found - %s", file.getAbsoluteFile(), nf.toString()));
                }
                catch (IOException io) {
                    Log.e(TAG, String.format("IO Exception loading tracked item data from %s - %s", file.getAbsoluteFile(), io.toString()));
                }
            }
        }

        return mItems.size();
    }

    public static int add(TrackedItem trackedItem) {
        //if (trackedItem.getID() == 0) {
        //trackedItem.setID(getNextID());
        //}

        mItems.add(trackedItem);

        return trackedItem.getID();
    }

    //static void saveTrackedItem(Activity activity, TrackedItem item) {
        //File dir = activity.getDir(TRACKED_ITEMS_DIR, 0);
        //File file = new File(dir, String.format(Locale.getDefault(), "%s.%d", TRACKED_ITEM_FILE_PREFIX, item.getID()));
//        item.saveToFile();//file);
//    }

    static void deleteTrackedItem(TrackedItem item) {
        File file = new File(TrackedItems.getTrackedItemsSaveDir(), String.format(Locale.getDefault(), "%s.%d", TRACKED_ITEM_FILE_PREFIX, item.getID()));

        Log.d(TAG, String.format("Deleting file %s", file.getAbsolutePath()));

        if (file.exists()) {
            file.delete();
        }

        item.deleteHistory();

        int i = mItems.indexOf(item);
        if (i >= 0) {
            mItems.remove(item);
        }

        item.removeMapMarker();
    }

    static int getNextID() {
        ++mHighestID;
        return mHighestID;
    }

    static TrackedItem getItemByID(int id) {
        for (TrackedItem item : mItems) {
            if (item.getID() == id) {
                return item;
            }
        }

        return null;
    }

    static ArrayList<TrackedItem> getTrackedItemsList() {
        return mItems;
    }

    public static int size() {
        return mItems.size();
    }

    static TrackedItem item(int index) {
        return mItems.get(index);
    }
}
