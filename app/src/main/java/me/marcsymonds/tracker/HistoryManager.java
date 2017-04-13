package me.marcsymonds.tracker;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
    private int mEntriesInCurrentFile = 0;
    private ArrayList<Location> mRecentHistory;
    private Location mLastRecordedLocation;
    private boolean mHistoryChanged;
    private int mMaxHistoryFiles = MAX_HISTORY_FILES;
    private int mMaxEntriesPerFile = MAX_HISTORY_ENTRIES_PER_FILE;
    private boolean mHistoryRecorder = false;

    HistoryManager(File historyDir) {
        initialise(historyDir, MAX_HISTORY_FILES, MAX_HISTORY_ENTRIES_PER_FILE, false);
    }

    HistoryManager(File historyDir, int maxHistoryFiles, int maxEntriesperFile, boolean historyRecorder) {
        initialise(historyDir, maxHistoryFiles, maxEntriesperFile, historyRecorder);
    }

    private void initialise(File historyDir, int maxHistoryFiles, int maxEntriesPerFile, boolean historyRecorder) {
        int histID;
        int i;

        mHistoryFileDirectory = historyDir;
        mMaxHistoryFiles = maxHistoryFiles;
        mMaxEntriesPerFile = maxEntriesPerFile;
        mHistoryRecorder = historyRecorder;

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

        File[] historyFiles = mHistoryFileDirectory.listFiles();
        if (historyFiles != null) {
            for (File file : historyFiles) {
                histID = Integer.parseInt(file.getName());

                // Could use .sort, but that requires later API.

                if (mHistoryFiles.size() == 0 || histID > mCurrentHistoryFileID) {
                    mHistoryFiles.add(file.getName());
                    mCurrentHistoryFileID = histID;
                } else if (histID < Integer.parseInt(mHistoryFiles.get(0))) {
                    mHistoryFiles.add(0, file.getName());
                } else {
                    i = 1;
                    while (histID > Integer.parseInt(mHistoryFiles.get(i))) {
                        ++i;
                    }
                    mHistoryFiles.add(i, file.getName());
                }
            }
        }

        {
            String a = "";
            for (String b : mHistoryFiles) {
                a = a + b + ", ";
            }

            Log.d(TAG, String.format("History files in %s: %s", mHistoryFileDirectory.getAbsoluteFile(), a));
        }

        if (!mHistoryRecorder) {
            mRecentHistory = new ArrayList<>();
        }

        mLastRecordedLocation = null;

        // If there are history files, then load the last one.
        if (mCurrentHistoryFileID > 0) {
            mCurrentHistoryFile = new File(mHistoryFileDirectory, mHistoryFiles.get(mHistoryFiles.size() - 1));
            ArrayList<Location> oldHist = loadHistoryFile(mCurrentHistoryFile);
            Location oldHistLoc;
            mEntriesInCurrentFile = oldHist.size();

            if (!mHistoryRecorder) {
                // Get list of recent location history.
                i = mEntriesInCurrentFile - 1;
                int recHistorySize = 0;
                while (i >= 0 && (mLastRecordedLocation == null || recHistorySize < MAX_RECENT_HISTORY)) {
                    oldHistLoc = oldHist.get(i);

                    if (recHistorySize < MAX_RECENT_HISTORY) {
                        mRecentHistory.add(0, oldHistLoc);
                        ++recHistorySize;
                    }

                    if (mLastRecordedLocation == null) {
                        if (oldHistLoc.hasLocation() || oldHistLoc.hasLastKnownLocation()) {
                            mLastRecordedLocation = oldHistLoc;
                        }
                    }

                    --i;
                }
                oldHist.clear();

                // If we haven't got enough recent history, then get it from the previous history file.
                if ((mLastRecordedLocation == null || recHistorySize < MAX_RECENT_HISTORY)) {
                    int histFileIdx = mHistoryFiles.size() - 2;
                    while (histFileIdx >= 0) {
                        File oldFile = new File(mHistoryFileDirectory, mHistoryFiles.get(histFileIdx));
                        oldHist = loadHistoryFile(oldFile);

                        i = oldHist.size() - 1;
                        while (i > 0 && (mLastRecordedLocation == null || recHistorySize < MAX_RECENT_HISTORY)) {
                            oldHistLoc = oldHist.get(i);

                            if (recHistorySize < MAX_RECENT_HISTORY) {
                                mRecentHistory.add(0, oldHistLoc);
                                ++recHistorySize;
                            }

                            if (mLastRecordedLocation == null) {
                                if (oldHistLoc.hasLocation() || oldHistLoc.hasLastKnownLocation()) {
                                    mLastRecordedLocation = oldHistLoc;
                                }
                            }

                            --i;
                        }

                        --histFileIdx;
                    }
                    oldHist.clear();
                }
            }
        } else {
            // Otherwise, set up for the first history file.
            mCurrentHistoryFile = new File(mHistoryFileDirectory, "1");
            mCurrentHistoryFileID = 1;
            mEntriesInCurrentFile = 0;
        }

        mHistoryChanged = false;
    }

    /**
     * Add a new location to the history.
     *
     * @param location location to add.
     */
    void recordLocation(Location location) {
        synchronized (this) {
            if (!mHistoryRecorder) {
                mRecentHistory.add(location);
                while (mRecentHistory.size() > MAX_RECENT_HISTORY) {
                    mRecentHistory.remove(0);
                }

                if (location.hasLocation() || location.hasLastKnownLocation()) {
                    mLastRecordedLocation = location;
                }
            }

            if (mEntriesInCurrentFile >= mMaxEntriesPerFile) {
                try {
                    FileReader fr = new FileReader(mCurrentHistoryFile);
                    char buf[] = new char[128];
                    int r;
                    String s = "";
                    r = fr.read(buf, 0, 128);
                    while (r > 0) {
                        s = s + String.valueOf(buf, 0, r);
                        r = fr.read(buf, 0, 128);
                    }
                    fr.close();
                    Log.d(TAG, String.format("LAST FILE: %s\n%s", mCurrentHistoryFile.getAbsoluteFile(), s));
                } catch (FileNotFoundException fnf) {
                    Log.e(TAG, String.format("File not found: %s", fnf.getMessage()));
                } catch (IOException io) {
                    Log.e(TAG, String.format("IO Exception: %s", io.getMessage()));
                }

                startNewHistoryFile();
            }

            // Append the location to the history file.
            try {
                FileWriter fw = new FileWriter(mCurrentHistoryFile, true);
                fw.write(location.toString());
                fw.write("\n");
                fw.close();

                ++mEntriesInCurrentFile;
                Log.d(TAG, String.format("Recorded history to %s - %s (Entries: %d)", mCurrentHistoryFile.getAbsoluteFile(), location.toString(), mEntriesInCurrentFile));
            } catch (FileNotFoundException fnf) {
                Log.e(TAG, String.format("File not found: %s", fnf.getMessage()));
            } catch (IOException io) {
                Log.e(TAG, String.format("IO Exception: %s", io.getMessage()));
            }

        }
        mHistoryChanged = true;
    }

    Location getLastLocation() {
        return mLastRecordedLocation;
    }

    /**
     * Starts a new history file, and deletes old history files.
     */
    private void startNewHistoryFile() {
        synchronized (this) {
            mCurrentHistoryFileID++;
            mCurrentHistoryFile = new File(mHistoryFileDirectory, String.valueOf(mCurrentHistoryFileID));
            mEntriesInCurrentFile = 0;

            mHistoryFiles.add(String.valueOf(mCurrentHistoryFileID));

            if (!mHistoryRecorder) {
                while (mHistoryFiles.size() > mMaxHistoryFiles) {
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
        }
    }

    /**
     * Gets a list of the history files, and loads the history from the last history file.
     */
    ArrayList<Location> loadHistoryFile(File file) {
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

                    //Log.d(TAG, String.format("+++ %s", location.toString()));
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

    boolean isHistoryChanged() {
        return mHistoryChanged;
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

    String[] getListOfFilesForUpload() {
        String[] fileList = null;

        synchronized (this) {
            if (mHistoryFiles != null) {
                if (mEntriesInCurrentFile > 0) {
                    startNewHistoryFile();
                }

                fileList = new String[mHistoryFiles.size() - 1];
                // subList(fromIdx (Inclusive), toIdx (Exclusive).
                mHistoryFiles.subList(0, mHistoryFiles.size() - 1).toArray(fileList);
            }
        }

        return fileList;
    }

    File getFileFor(String fileName) {
        File file = new File(mHistoryFileDirectory, fileName);
        if (!file.exists()) {
            file = null;
        }

        return file;
    }

    boolean deleteFile(String fileName) {
        return deleteFile(getFileFor(fileName));
    }

    boolean deleteFile(File file) {
        String fileName = file.getName();

        Log.d(TAG, String.format("Delete file: %s - %s", file.getAbsoluteFile(), fileName));
        if (mHistoryFiles.contains(fileName)) {
            mHistoryFiles.remove(fileName);
        }

        if (file.exists()) {
            return file.delete();
        }

        return false;
    }
}
