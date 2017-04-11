package me.marcsymonds.tracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Marc on 09/04/2017.
 */

/**
 * Class for interacting with an SQLite database.
 */
class DBStore extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "MSS_Tracker";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_APPID = "AppID";
    private static final String COLUMN_APPID_APPID = "AppID";

    private static final Object syncObject = new Object();

    DBStore(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_APPID +
                        "(" +
                        COLUMN_APPID_APPID + " TEXT" +
                        ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int a, int b) {

    }

    String getAppID() {
        String appID = null;

        synchronized (syncObject) {
            SQLiteDatabase db = this.getReadableDatabase();

            try {
                Cursor c = db.query(TABLE_APPID, new String[]{COLUMN_APPID_APPID}, null, null, null, null, null);
                try {
                    if (c.moveToFirst()) {
                        appID = c.getString(0);
                    }
                } finally {
                    c.close();
                    c = null;
                }
            } finally {
                db.close();
                db = null;
            }

        }
        return appID;
    }

    boolean storeAppID(String appID) {
        boolean stored = false;

        synchronized (syncObject) {
            SQLiteDatabase db = this.getWritableDatabase();

            try {
                db.delete(TABLE_APPID, null, null);

                ContentValues contentValues = new ContentValues();
                contentValues.put(COLUMN_APPID_APPID, appID);

                db.insert(TABLE_APPID, null, contentValues);
                stored = true;
            } finally {
                db.close();
                db = null;
            }
        }

        return stored;
    }
}
