package com.getbonkers.bottlecaps;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 1/19/12
 * Time: 4:14 PM
 * To change this template use File | Settings | File Templates.
 */

public class BottlecapsDatabaseAdapter {
    public static final String KEY_ROWID = "_id";
    
    public static final String KEY_SETS_NAME="name";
    public static final String KEY_SETS_ARTIST="artist";
    public static final String KEY_SETS_DESCRIPTION="description";
    public static final String KEY_SETS_LASTPLAYED="lastPlayed";
    
    public static final String KEY_CAPS_AVAILABLE="available";
    public static final String KEY_CAPS_DESCRIPTION="description";
    public static final String KEY_CAPS_ISSUED="issued";
    public static final String KEY_CAPS_NAME="name";
    public static final String KEY_CAPS_SETID="setID";
    public static final String KEY_CAPS_SCARCITY="scarcity";
    public static final String KEY_CAPS_COLLECTED="collected";
    
    public static final String KEY_SETTLEMENTS_CAP="cap";
    
    private static final String TAG = "DBAdapter";

    private static final String DATABASE_NAME = "bottlecaps";
    private static final String DATABASE_CAPS_TABLE = "caps";
    private static final String DATABASE_SETS_TABLE = "sets";
    private static final String DATABASE_SETTLEMENTS_TABLE = "settlements";

    private static final int DATABASE_VERSION = 4;

    private static final String DATABASE_SETS_CREATE =
            "create table sets (_id integer unique primary key, "
                    + "name text, artist text, "
                    + "description text, lastPlayed datetime);";
    private static final String DATABASE_CAPS_CREATE =
            "create table caps (_id integer unique primary key, "
                    + "available integer, issued integer, description text, name text, setID integer, scarcity integer, collected integer);";
    private static final String DATABASE_SETTLEMENTS_CREATE =
            "create table settlements (_id integer unique primary key autoincrement, " +
                    "cap integer);";

    private final Context context;

    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public BottlecapsDatabaseAdapter(Context ctx)
    {
        this.context = ctx;
        DBHelper = new DatabaseHelper(context);
    }

    private static class DatabaseHelper extends SQLiteOpenHelper
    {
        DatabaseHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL(DATABASE_CAPS_CREATE);
            db.execSQL(DATABASE_SETS_CREATE);
            db.execSQL(DATABASE_SETTLEMENTS_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                              int newVersion)
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion
                    + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS caps");
            db.execSQL("DROP TABLE IF EXISTS sets");
            db.execSQL("DROP TABLE IF EXISTS settlements");
            onCreate(db);
        }
    }

    public BottlecapsDatabaseAdapter openReadOnly()
    {
        if(db!=null) db.close();
        db=DBHelper.getReadableDatabase();
        return this;
    }

    //---opens the database---
    public BottlecapsDatabaseAdapter open() throws SQLException
    {
        db = DBHelper.getWritableDatabase();
        return this;
    }

    //---closes the database---
    public void close()
    {
        DBHelper.close();
    }
    
    public boolean capIsCollected(long capID)
    {
        int numCollected;

        Cursor howMany=db.query(DATABASE_CAPS_TABLE, new String[] { KEY_CAPS_COLLECTED }, KEY_ROWID+"="+capID, null, null, null, null);
        howMany.moveToFirst();

        if(howMany.getCount()==0)
            numCollected=0;
        else
            numCollected=howMany.getInt(howMany.getColumnIndex(KEY_CAPS_COLLECTED));

        howMany.close();

        return numCollected>0;
    }

    public boolean updateSetLastPlayed(long setID, Date newDate)
    {
        ContentValues values=new ContentValues();
        values.put(KEY_SETS_LASTPLAYED, newDate.getTime());
        return db.update(DATABASE_SETS_TABLE, values, KEY_ROWID+"="+setID, null) > 0;
    }
    
    public long insertSet(long setID, String setName, String artist, String description)
    {
        ContentValues values=new ContentValues();
        values.put(KEY_ROWID, setID);
        values.put(KEY_SETS_ARTIST, artist);
        values.put(KEY_SETS_NAME, setName);
        values.put(KEY_SETS_DESCRIPTION, description);
        return db.insertWithOnConflict(DATABASE_SETS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    
    public int numberOfUncollectedCommonCaps()
    {
        Cursor capsCursor=db.query(DATABASE_CAPS_TABLE, new String[] { KEY_ROWID }, KEY_CAPS_COLLECTED+"<=0 AND "+KEY_CAPS_SCARCITY+"<2", null, null, null, null);

        capsCursor.moveToFirst();
        int count=capsCursor.getCount();
        capsCursor.close();

        return count;
    }

    public Cursor getUncollectedCommonCapsAsCursor()
    {
        return db.query(DATABASE_CAPS_TABLE, new String[] { KEY_ROWID, KEY_CAPS_AVAILABLE, KEY_CAPS_ISSUED, KEY_CAPS_SCARCITY }, KEY_CAPS_COLLECTED+"<=0 AND "+KEY_CAPS_SCARCITY+"<2", null, null, null, null);
          /*
        while(capsCursor.moveToNext())
        {
            caps.add(capsCursor.getLong(capsCursor.getColumnIndex(KEY_ROWID)));
        }

        capsCursor.close();
         */
    }
    
    public ArrayList<Long> getUncollectedCommonCaps()
    {
        ArrayList<Long> caps=new ArrayList<Long>();
        
        Cursor capsCursor=db.query(DATABASE_CAPS_TABLE, new String[] { KEY_ROWID }, KEY_CAPS_COLLECTED+"<=0 AND "+KEY_CAPS_SCARCITY+"<2", null, null, null, null);

        while(capsCursor.moveToNext())
        {
            caps.add(capsCursor.getLong(capsCursor.getColumnIndex(KEY_ROWID)));
        }
        
        capsCursor.close();
        
        return caps;
    }

    public long numberOfUniqueCapsCollected()
    {
        Cursor howMany=db.query(DATABASE_CAPS_TABLE, new String[] { KEY_ROWID }, KEY_CAPS_COLLECTED+">0", null, null, null, null);

        howMany.moveToFirst();
        long capCount=howMany.getCount();
        howMany.close();

        return capCount;
    }
    
    public long capsCollectedInSet(long setID)
    {
        Cursor howMany=db.query(DATABASE_CAPS_TABLE, new String[] { KEY_ROWID }, KEY_CAPS_COLLECTED+">0 and "+KEY_CAPS_SETID+"="+setID, null, null, null, null);

        long inSet=howMany.getCount();
        howMany.close();

        return inSet;
    }

    public long numberOfCapsInDatabase()
    {
        Cursor howMany=db.query(DATABASE_CAPS_TABLE, new String[] { KEY_ROWID }, null, null, null, null, null);

        howMany.moveToFirst();
        long capCount=howMany.getCount();
        howMany.close();

        return capCount;
    }
    
    public long addCapSettlement(long capID)
    {
        ContentValues values=new ContentValues();
        values.put(KEY_SETTLEMENTS_CAP, capID);
        long ret1=db.insertWithOnConflict(DATABASE_SETTLEMENTS_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);

        Cursor howMany=db.query(DATABASE_CAPS_TABLE, new String[] { KEY_ROWID, KEY_CAPS_COLLECTED }, KEY_ROWID+"="+capID, null, null, null, null);
        howMany.moveToFirst();
        ContentValues capCollectedUpdate=new ContentValues();
        capCollectedUpdate.put(KEY_CAPS_COLLECTED, howMany.getInt(howMany.getColumnIndex(KEY_CAPS_COLLECTED))+1);
        long ret2=db.updateWithOnConflict(DATABASE_CAPS_TABLE, capCollectedUpdate, KEY_ROWID+"="+capID, null, SQLiteDatabase.CONFLICT_IGNORE);

        howMany.close();

        return ret1;
    }
    
    public Cursor getOutstandingCapSettlements()
    {
        return db.query(DATABASE_SETTLEMENTS_TABLE, new String[] { KEY_SETTLEMENTS_CAP}, null, null, null, null, null, null);
    }

    public void clearCapSettlements()
    {
        db.delete(DATABASE_SETTLEMENTS_TABLE, null, null);
    }

    public long[] getNextPlayableSetsTotalingNumberOfCaps(int numberOfCaps)
    {
        /*
        order the sets by most recently played ascending (oldest first)
        include a set and get another set if we still need more caps
         */

        return new long[]{0};
    }

    public long getRandomSetID()
    {
        Cursor ret=db.query(DATABASE_SETS_TABLE, new String[] { KEY_ROWID }, null, null, null, null, "RANDOM()", "1");
        ret.moveToFirst();
        long id=ret.getLong(ret.getColumnIndex(KEY_ROWID));
        ret.close();
        return id;
    }
    
    public Cursor getCap(long capID)
    {
        return db.query(DATABASE_CAPS_TABLE, new String[] { KEY_ROWID, KEY_CAPS_COLLECTED, KEY_CAPS_SCARCITY, KEY_CAPS_AVAILABLE }, KEY_ROWID+"="+capID, null, null, null, null);
    }
    
    public long insertCapIntoSet(long setID, long capID, int available, int issued, String name, String description, int scarcity)
    {
        ContentValues values=new ContentValues();
        values.put(KEY_ROWID, capID);
        values.put(KEY_CAPS_SETID, setID);
        values.put(KEY_CAPS_AVAILABLE, available);
        values.put(KEY_CAPS_ISSUED, issued);
        values.put(KEY_CAPS_NAME, name);
        values.put(KEY_CAPS_DESCRIPTION, description);
        values.put(KEY_CAPS_SCARCITY, scarcity);
        values.put(KEY_CAPS_COLLECTED, 0);
        return db.insertWithOnConflict(DATABASE_CAPS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Cursor getSet(long setID)
    {
        return db.query(DATABASE_SETS_TABLE, new String[] { KEY_ROWID, KEY_SETS_DESCRIPTION, KEY_SETS_NAME, KEY_SETS_ARTIST }, KEY_ROWID+"="+setID, null, null, null, null);
    }
    
    public boolean setExistsInDatabase(long setID)
    {
        Cursor set=db.query(DATABASE_SETS_TABLE, new String[] { KEY_ROWID, KEY_SETS_DESCRIPTION, KEY_SETS_NAME, KEY_SETS_ARTIST }, KEY_ROWID+"="+setID, null, null, null, null);

        boolean exists=(set.getCount()>0);
        
        set.close();

        return exists;
    }
    
    public Cursor getCapsInSet(long setID)
    {
        return db.query(DATABASE_CAPS_TABLE, new String[] { KEY_ROWID, KEY_CAPS_SCARCITY, KEY_CAPS_AVAILABLE, KEY_CAPS_DESCRIPTION, KEY_CAPS_ISSUED, KEY_CAPS_NAME, KEY_CAPS_SETID}, KEY_CAPS_SETID+"="+setID, null, null, null, null);
    }

    public boolean deleteSet(long setID)
    {
        return db.delete(DATABASE_SETS_TABLE, KEY_ROWID+"="+setID, null) > 0;
    }
}