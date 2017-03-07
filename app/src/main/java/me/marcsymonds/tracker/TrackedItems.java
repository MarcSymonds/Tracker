package me.marcsymonds.tracker;


import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Used for managing the list of tracked items.
 */
public class TrackedItems {
    final static private String TAG = "TrackedItems";

    final static private String TRACKED_ITEMS_DIR = "TrackedItems";
    final static private String TRACKED_ITEM_FILE_PREFIX = "TrackedItem";

    public static Activity AppActivity = null;

    private static int mHighestID = 0;
    private static ArrayList<TrackedItem> mItems = new ArrayList<>();

    public static void initialise(Activity activity) {
        AppActivity = activity;
        loadTrackedItems();
    }

    public static int getHighestID() {
        return mHighestID;
    }

    public static void setHighestID(int highestID) {
        mHighestID = highestID;
    }

    static int loadTrackedItems() {
        File dir = AppActivity.getDir(TRACKED_ITEMS_DIR, 0);

        Log.d(TAG, String.format("Loading tracked items from %s", dir.getAbsolutePath()));

        mItems.clear();

        for (File file : dir.listFiles()) {
            String fileName = file.getName();
            if (fileName.startsWith(TRACKED_ITEM_FILE_PREFIX)) {
                int ID = Integer.parseInt(fileName.substring(TRACKED_ITEM_FILE_PREFIX.length() + 1));
                try {
                    TrackedItem item = new TrackedItem(file);
                    mItems.add(item);

                    if (ID > mHighestID) {
                        mHighestID = ID;
                    }

                    item.loadHistory();
                }
                catch (FileNotFoundException nf) {
                    Log.e(TAG, String.format("Unable to load tracked item file %s : %s", fileName, nf.toString()));
                }
                catch (IOException io) {

                }
            }
        }

        return mItems.size();
    }

    public static int add(TrackedItem trackedItem) {
        if (trackedItem.getID() == 0) {
            trackedItem.setID(getNextID());
        }

        mItems.add(trackedItem);

        return trackedItem.getID();
    }

    public static void saveTrackedItem(TrackedItem item) {
        File dir = AppActivity.getDir(TRACKED_ITEMS_DIR, 0);
        File file = new File(dir, String.format("%s.%d", TRACKED_ITEM_FILE_PREFIX, item.getID()));
        item.saveToFile(file);
    }

    public static void deleteTrackedItem(TrackedItem item) {
        File dir = AppActivity.getDir(TRACKED_ITEMS_DIR, 0);
        File file = new File(dir, String.format("%s.%d", TRACKED_ITEM_FILE_PREFIX, item.getID()));

        Log.d(TAG, String.format("Deleting file %s", file.getAbsolutePath()));

        if (file.exists()) {
            file.delete();
        }

        int i = mItems.indexOf(item);
        if (i >= 0) {
            mItems.remove(item);
        }

        item.removeMapMarker();
    }

    public static int getNextID() {
        ++mHighestID;
        return mHighestID;
    }

    public static TrackedItem getItemByID(int id) {
        for (TrackedItem item : mItems) {
            if (item.getID() == id) {
                return item;
            }
        }

        return null;
    }

    public static ArrayList<TrackedItem> getTrackedItemsList() {
        return mItems;
    }

    public static int size() {
        return mItems.size();
    }

    public static TrackedItem item(int index) {
        return mItems.get(index);
    }
}
