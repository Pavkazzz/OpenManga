package org.nv95.openmanga.utils.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.nv95.openmanga.utils.FileLogger;

/**
 * Created by nv95 on 01.08.16.
 */

public class SyncQueue {

    private static final String TABLE_DELETED_FAVOURITES = "deleted_fav";
    private static final String TABLE_DELETED_HISTORY = "deleted_hist";

    private final DBHelper mDbHelper;
    private final boolean mEnabled;

    public SyncQueue(Context context) {
        mDbHelper = new DBHelper(context);
        mEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("sync.enabled", false);
    }

    private void addDeleted(String table, long id) {
        SQLiteDatabase database = null;
        try {
            database = mDbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("id", id);
            cv.put("timestamp", System.currentTimeMillis());
            if (database.update(table, cv, "id=?", new String[]{String.valueOf(id)}) == 0) {
                database.insert(table, null, cv);
            }
        } catch (Exception e) {
            FileLogger.getInstance().report(e);
        } finally {
            if (database != null) {
                database.close();
            }
        }
    }

    private JSONArray getAll(String table) {
        JSONArray ja = new JSONArray();
        SQLiteDatabase database = null;
        Cursor cursor = null;
        try {
            database = mDbHelper.getReadableDatabase();
            cursor = database.query(table, new String[]{"id"}, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    ja.put(cursor.getInt(0));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            FileLogger.getInstance().report(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (database != null) {
                database.close();
            }
        }
        return ja;
    }

    private void clearAll(String table) {
        SQLiteDatabase database = null;
        try {
            database = mDbHelper.getWritableDatabase();
            database.delete(table, null, null);
        } catch (Exception e) {
            FileLogger.getInstance().report(e);
        } finally {
            if (database != null) {
                database.close();
            }
        }
    }

    public void clearHistory() {
        if (mEnabled) {
            clearAll(TABLE_DELETED_HISTORY);
        }
    }

    public void clearFavourites() {
        if (mEnabled) {
            clearAll(TABLE_DELETED_FAVOURITES);
        }
    }

    public void pushFavouritesDeleted(long id) {
        if (mEnabled) {
            addDeleted(TABLE_DELETED_FAVOURITES, id);
        }
    }

    public void pushHistoryDeleted(long id) {
        if (mEnabled) {
            addDeleted(TABLE_DELETED_HISTORY, id);
        }
    }

    public JSONArray getDeletedHistory() {
        return getAll(TABLE_DELETED_HISTORY);
    }

    public JSONArray getDeletedFavourites() {
        return getAll(TABLE_DELETED_HISTORY);
    }

    private class DBHelper extends SQLiteOpenHelper {

        DBHelper(Context context) {
            super(context, "sync_queue", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE deleted_fav ("
                    + "id INTEGER PRIMARY KEY,"
                    + "timestamp INTEGER"
                    + ");");

            db.execSQL("CREATE TABLE deleted_hist ("
                    + "id INTEGER PRIMARY KEY,"
                    + "timestamp INTEGER"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    @Override
    protected void finalize() throws Throwable {
        mDbHelper.close();
        super.finalize();
    }
}
