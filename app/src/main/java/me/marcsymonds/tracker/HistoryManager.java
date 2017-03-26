package me.marcsymonds.tracker;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

/**
 * Class for storing and managing location histories.
 */
class HistoryManager {
    private final static int MAX_HISTORY_FILES = 10;
    private final static int MAX_HISTORY_ENTRIES_PER_FILE = 50;
    private final static int MAX_RECENT_HISTORY = 10;
    private final String TAG = "HistoryManager";
    private File mHistoryFileDirectory; // Directory where history files for this instance are located.
    private ArrayList<String> mHistoryFiles; // List of existing history file names.
    private File mCurrentHistoryFile; // Current/Last history file.
    private int mCurrentHistoryFileID; // ID of the Current/Last history file.
    private ArrayList<Location> mCurrentHistory;
    private ArrayList<Location> mRecentHistory;
    private boolean mHistoryChanged;

    HistoryManager(File historyDir) {
        int histID;
        int i;

        mHistoryFileDirectory = historyDir;

        Log.d(TAG, String.format("Loading history from directory %s", mHistoryFileDirectory.getAbsoluteFile()));

        if (!mHistoryFileDirectory.exists()) {
            Log.d(TAG, String.format("Creating history directory %s", mHistoryFileDirectory.getAbsoluteFile()));
            if (!mHistoryFileDirectory.mkdirs()) {
                Log.w(TAG, String.format("Failed to create history directory %s", mHistoryFileDirectory.getAbsoluteFile()));
            }
        }

        // Get list of history files for this Tracked Item.
        // History file names are just a number.

        mHistoryFiles = new ArrayList<>();
        mCurrentHistoryFileID = 0;

        for (File file : mHistoryFileDirectory.listFiles()) {
            histID = Integer.parseInt(file.getName());

            // Could use .sort, but that requires later API.

            if (mHistoryFiles.size() == 0 || histID > mCurrentHistoryFileID) {
                mHistoryFiles.add(file.getName());
                mCurrentHistoryFileID = histID;
            } else if (histID < Integer.parseInt(mHistoryFiles.get(0))) {
                mHistoryFiles.add(0, file.getName());
            } else {
                i = 0;
                while (histID < Integer.parseInt(mHistoryFiles.get(i))) {
                    ++i;
                }
                mHistoryFiles.add(i, file.getName());
            }
        }

        Log.d(TAG, "History files:-");
        for (String s : mHistoryFiles) {
            Log.d(TAG, "+++ " + s);
        }

        mRecentHistory = new ArrayList<>();

        // If there are history files, then load the last one.
        if (mCurrentHistoryFileID > 0) {
            mCurrentHistoryFile = new File(mHistoryFileDirectory, mHistoryFiles.get(mHistoryFiles.size() - 1));
            mCurrentHistory = loadHistoryFile(mCurrentHistoryFile);

            // Get list of recent location history.
            i = mCurrentHistory.size();
            while (i > 0 && mRecentHistory.size() < MAX_RECENT_HISTORY) {
                --i;
                mRecentHistory.add(0, mCurrentHistory.get(i));
            }

            // If we haven't got enough recent history, then get it from the previous history file.
            if (mRecentHistory.size() < MAX_RECENT_HISTORY && mHistoryFiles.size() > 1) {
                File oldFile = new File(mHistoryFileDirectory, mHistoryFiles.get(mHistoryFiles.size() - 2));
                ArrayList<Location> oldHist = loadHistoryFile(oldFile);

                i = oldHist.size();
                while (i > 0 && mRecentHistory.size() < MAX_RECENT_HISTORY) {
                    --i;
                    mRecentHistory.add(0, oldHist.get(i));
                }
            }
        } else {
            // Otherwise, set up for the first history file.
            mCurrentHistoryFile = new File(mHistoryFileDirectory, "1");
            mCurrentHistoryFileID = 1;
            mCurrentHistory = new ArrayList<>();
        }

        mHistoryChanged = false;
    }

    /**
     * Add a new location to the history.
     *
     * @param location location to add.
     */
    public void recordLocation(Location location) {
        mRecentHistory.add(location);
        while (mRecentHistory.size() > MAX_RECENT_HISTORY) {
            mRecentHistory.remove(0);
        }

        // If the current history is full, save it and start a new one.
        if (mCurrentHistory.size() >= MAX_HISTORY_ENTRIES_PER_FILE) {
            // Save the current history.
            saveHistoryFile(mCurrentHistory, mCurrentHistoryFile);

            // Start a new history file.
            startNewHistoryFile();
        }

        mCurrentHistory.add(location);
        mHistoryChanged = true;
    }

    public Location getLastLocation() {
        if (mRecentHistory.size() > 0) {
            return mRecentHistory.get(mRecentHistory.size() - 1);
        } else {
            return null;
        }
    }

    /**
     * Starts a new history file, and deletes old history files.
     */
    private void startNewHistoryFile() {
        mCurrentHistoryFileID++;
        mCurrentHistoryFile = new File(mHistoryFileDirectory, String.valueOf(mCurrentHistoryFileID));
        mCurrentHistory.clear();

        mHistoryFiles.add(String.valueOf(mCurrentHistoryFileID));
        while (mHistoryFiles.size() > MAX_HISTORY_FILES) {
            File fileToDelete = new File(mHistoryFileDirectory, mHistoryFiles.get(0));
            if (fileToDelete.exists()) {
                Log.d(TAG, String.format("Deleting history file %s", fileToDelete.getAbsoluteFile()));
                if (!fileToDelete.delete()) {
                    Log.w(TAG, String.format("Unable to delete history file %s", fileToDelete));
                }
            }

            mHistoryFiles.remove(0);
        }

    }

    /**
     * Gets a list of the history files, and loads the history from the last history file.
     */
    private ArrayList<Location> loadHistoryFile(File file) {
        ArrayList<Location> locations = new ArrayList<>();

        loadHistoryFile(file, locations);

        return locations;
    }

    private void loadHistoryFile(File file, ArrayList<Location> locations) {
        String line;
        Location location;

        Log.d(TAG, String.format("Loading history from %s", file.getAbsolutePath()));

        locations.clear();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            line = reader.readLine();
            while (line != null) {
                try {
                    location = new Location(line);
                    locations.add(location);

                    Log.d(TAG, String.format("+++ %s", location.toString()));
                } catch (ParseException pe) {
                    Log.e(TAG, String.format("Exception parsing location history entry from %s - %s: %s", file.getAbsolutePath(), line, pe.toString()));
                } catch (IllegalArgumentException ia) {
                    Log.e(TAG, ia.toString());
                }
                line = reader.readLine();
            }

            reader.close();
        } catch (IOException io) {
            Log.e(TAG, String.format("IOException reading file %s - %s", file.getAbsolutePath(), io.toString()));
        }
    }

    void saveHistory() {
        saveHistoryFile(mCurrentHistory, mCurrentHistoryFile);
    }

    boolean isHistoryChanged() {
        return mHistoryChanged;
    }

    private void saveHistoryFile(ArrayList<Location> locations, File file) {
        Log.d(TAG, String.format("Saving history to file %s", file.getAbsoluteFile()));

        try {
            if (file.exists() && !file.delete()) {
                Log.w(TAG, String.format("Failed to delete history file %s", file.getAbsoluteFile()));
            }

            if (locations != null) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));

                for (Location loc : locations) {
                    Log.d(TAG, String.format("+++ %s", loc.toString()));

                    writer.write(loc.toString());
                    writer.newLine();
                }

                writer.close();
            }

            mHistoryChanged = false;
        } catch (IOException io) {
            Log.e(TAG, String.format("IOException writing location history to %s - %s", file.getAbsoluteFile(), io.toString()));
        }
    }

    void deleteAllHistory() {
        for (File file : mHistoryFileDirectory.listFiles()) {
            Log.d(TAG, String.format("Deleting history file %s", file.getAbsoluteFile()));
            if (!file.delete()) {
                Log.w(TAG, String.format("Failed to delete file %s", file.getAbsoluteFile()));
            }
        }

        Log.d(TAG, String.format("Deleting history directory %s", mHistoryFileDirectory.getAbsoluteFile()));
        if (!mHistoryFileDirectory.delete()) {
            Log.w(TAG, String.format("Failed to delete file %s", mCurrentHistoryFile.getAbsoluteFile()));
        }
    }
}
