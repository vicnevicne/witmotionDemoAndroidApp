package com.bt901.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

/**
 * ${GWB}
 * 2017/5/23.
 */
public class SQLite {

    public static final String DB_ACTION = "db_action"; //LogCat
    private static final String DB_NAME = "BD001.db";
    private static final int DB_VERSION = 1;
    private static SQLite mDBAdapter;
    private static Context xContext;
    private boolean isOpen = false;
    private SQLiteDatabase db;
    private DBOpenHelper dbOpenHelper;

    private SQLite() {

    }

    public static SQLite init(Context context) {
        if (mDBAdapter != null) {
            return mDBAdapter;
        }
        xContext = context;
        mDBAdapter = new SQLite();
        return mDBAdapter;
    }

    public void open() throws SQLiteException {
        if (isOpen) {
            return;
        }
        dbOpenHelper = new DBOpenHelper(xContext, DB_NAME, null, DB_VERSION);
        try {
            db = dbOpenHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            db = dbOpenHelper.getReadableDatabase();
        }
    }

    public byte[] BitmapToByte(Bitmap bitmap) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        return os.toByteArray();
    }

    public Bitmap ByteToBitmap(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    public long insert(String table, ContentValues newValues) {
        if (newValues == null) {
            return -1;
        }
        return db.insert(table, null, newValues);
    }

    public long insertScene(Bitmap bitmap) {
        ContentValues newValues = new ContentValues();
        newValues.put(StartTable.STARTPIC, BitmapToByte(bitmap));
        return db.insert(StartTable.DB_TABLE, null, newValues);
    }


    public int upDataforTable(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return db.update(table, values, whereClause, whereArgs);
    }


    public long deleteOneData(String table, String whereClause, String[] whereArgs) {
        return db.delete(table, whereClause, whereArgs);
    }

    public Cursor queryAllData(String table) {
        Cursor result = db.query(table, null, null, null, null, null, null);
        return result;
    }


    public Cursor queryDataByMAC(String mac) {
        Cursor result = db.query(LightTable.DB_TABLE, null, LightTable._ID + "=" + "'" + mac + "'", null, null, null, null);
        return result;
    }


    private static class DBOpenHelper extends SQLiteOpenHelper {

        private static final String START_DB_CREATE =
                "CREATE TABLE " + StartTable.DB_TABLE
                        + " (" + StartTable._ID + " integer primary key autoincrement, "
                        + StartTable.STARTPIC + " BLOB " + ");";

        private static final String LIGHT_DB_CREATE =
                "CREATE TABLE " + LightTable.DB_TABLE
                        + " (" + LightTable._ID + " varchar primary key, "
                        + LightTable.NAME + " varchar " + ");";

        private static final String NETWORKTABLE_CREATE =
                "CREATE TABLE " + NetworkTable.DB_TABLE
                        + " (" + NetworkTable._ID + " integer primary key autoincrement, "
                        + NetworkTable.CAROUSELFIGURE + " integer, "
                        + NetworkTable.CATEGORY + " integer, "
                        + NetworkTable.CLASSIFICATION + " integer, "
                        + NetworkTable.INFORMATION + " integer, "
                        + NetworkTable.STARTUPPAGE + " integer " + ");";


        public DBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(START_DB_CREATE);
            db.execSQL(NETWORKTABLE_CREATE);
            db.execSQL(LIGHT_DB_CREATE);

            ContentValues contentValues = new ContentValues();
            contentValues.put(NetworkTable.CAROUSELFIGURE, 0);
            contentValues.put(NetworkTable.STARTUPPAGE, 0);
            contentValues.put(NetworkTable.CATEGORY, 0);
            contentValues.put(NetworkTable.CLASSIFICATION, 0);
            contentValues.put(NetworkTable.INFORMATION, 0);
            db.insert(NetworkTable.DB_TABLE, null, contentValues);
        }


        @Override
        public void onUpgrade(SQLiteDatabase _db, int oldVersion, int newVersion) {

        }

    }


}
