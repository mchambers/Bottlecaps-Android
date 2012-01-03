package com.getbonkers.bottlecaps;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by IntelliJ IDEA.
 * User: marc
 * Date: 12/28/11
 * Time: 7:27 PM
 * To change this template use File | Settings | File Templates.
 */

public class CapDatabaseOpenHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bottlecaps";
    private static final int DATABASE_VERSION = 2;

    CapDatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String createCapsTable="CREATE TABLE CAPS (FILEPATH TEXT, " +
                "_id integer primary key autoincrement, " +
                "SET INTEGER, " +
                "CREATEDATE TEXT, " +
                "BASETYPE INTEGER, " +
                "NAME TEXT, " +
                "DESCRIPTION TEXT, " +
                "IDENTIFIER TEXT, " +
                "ISSUED INTEGER,  " +
                "COLLECTED INTEGER)";

        String createSetsTable="CREATE TABLE SETS (NAME TEXT, DESCRIPTION TEXT, IDENTIFIER TEXT";

        db.execSQL(createCapsTable);
        db.execSQL(createSetsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}