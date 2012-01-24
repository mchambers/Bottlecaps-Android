package com.getbonkers.bottlecaps;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Array;
import java.sql.Time;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;

/*

Add time
Joker
Hi-lite
Freeze
Momentum boost

 */
public class CapManager {
    public interface CapManagerDelegate {
        public void onCapSetsLoadComplete();
        public void onCapSetsLoadFailure();
    }

    public class Boost extends Cap {
        public void performBoostEffects(GameBoardActivity.GameBoard board)
        {
        }

        public long getStandardDuration()
        {
            return 0;
        }
    }

    public class JokerBoost extends Boost {
        public void putCapInPlay(Context context)
        {
            this.resourceId=context.getResources().getIdentifier("boostjoker", "drawable", "com.getbonkers.bottlecaps");
            super.putCapInPlay(context);
        }

        public boolean equals(Cap o)
        {
            return true;
        }
    }

    public class FreezeBoost extends Boost {
        public void putCapInPlay(Context context)
        {

        }

        public void performBoostEffects(GameBoardActivity.GameBoard board)
        {

        }
    }

    public class MomentumBoost extends Boost {
        public void putCapInPlay(Context context)
        {
            this.resourceId=context.getResources().getIdentifier("boostnitro", "drawable", "com.getbonkers.bottlecaps");
            super.putCapInPlay(context);
        }

        public void performBoostEffects(GameBoardActivity.GameBoard board)
        {
            board.currentMomentum+=(100-board.currentMomentum)/2;
        }
    }

    public class TimeBoost extends Boost {
        public void putCapInPlay(Context context)
        {
            this.resourceId=context.getResources().getIdentifier("boostincreasetime", "drawable", "com.getbonkers.bottlecaps");
            super.putCapInPlay(context);
        }

        public void performBoostEffects(GameBoardActivity.GameBoard board)
        {
            board.gameTimers[GameBoardActivity.GameBoard.GAME_TIMER_REMAINING]+=(10*1000); // add 10 seconds
        }
    }

    public class HighlightCombosBoost extends Boost {
        public void putCapInPlay(Context context)
        {
            this.resourceId=context.getResources().getIdentifier("boosthighlight", "drawable", "com.getbonkers.bottlecaps");
            super.putCapInPlay(context);
        }

        public void performBoostEffects(GameBoardActivity.GameBoard board)
        {
            // highlight the caps in the biggest combo on the board
            // only if they're not already tapped.
            for(int i=0; i<board.gamePieces.size(); i++)
            {
                if(board.gamePieces.get(i).cap.numberInPlay>1)
                    board.gamePieces.get(i).setHighlightedState();
            }
        }
    }

    public class Cap {
        public String filePath;
        public int index;
        public int setNumber;
        public int resourceId;

        public long issued;
        public long available;

        public long probabilityMin;
        public long probabilityMax;

        public int rarityClass;

        public int numberInPlay;

        public BitmapDrawable image;

        public boolean equals(Cap o)
        {
            if(o==null) return false;
            return (o.resourceId==this.resourceId);
        }

        public boolean isCurrentlyDrawable()
        {
            if(image!=null && image.getBitmap()!=null && !image.getBitmap().isRecycled()) return true;
            return false;
        }

        public void putCapInPlay(Context context)
        {
            if(numberInPlay<=0)
            {
                BitmapFactory.Options options=new BitmapFactory.Options();
                //options.inSampleSize=4;
                this.image=new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(), this.resourceId, options));
            }
            this.numberInPlay++;
        }

        public void removeCapFromPlay()
        {
            this.numberInPlay--;
            if(image!=null)
            {
                if(numberInPlay<=0)
                {
                    image.getBitmap().recycle();
                    image=null;
                }
            }
        }
    }

    public class CapTotalAvailableComparator implements Comparator<Cap> {
        public int compare(Cap o1, Cap o2) {
            if(o1.available<o2.available) return -1;
            if(o1.available>o2.available) return 1;
            return 0;
        }
    }

    public class CapMaxProbabilityComparator implements Comparator<Cap> {
        public int compare(Cap o1, Cap o2) {
            if(o1.probabilityMax<o2.probabilityMax) return -1;
            if(o1.probabilityMax>o2.probabilityMax) return 1;
            return 0;
        }
    }

    public class Set {
        ArrayList<Cap> capsInSet;

        public Set()
        {
            capsInSet=new ArrayList<Cap>();
        }
    }

    private boolean capManagerInit=false;
    Context _context;
    public long circulation;
    private Stack<Cap> capsBuffer;
    private Stack<Boost> boostsBuffer;
    private Stack<Cap> comboCaps;
    private ArrayList<Set> sets;
    private ArrayList<Cap> allCaps;
    private ArrayList<Boost> boostsAvailable;

    private int level;

    BottlecapsDatabaseAdapter adapter;

    private Cap currentMostPlayedCap;

    public int[] combosDelivered;

    public CapManager(Context context, int difficulty, final CapManagerDelegate delegate)
    {
        _context=context;

        adapter=new BottlecapsDatabaseAdapter(context);
        adapter.open();
        
        level = difficulty;

        combosDelivered=new int[10];

        capsBuffer=new Stack<Cap>();
        comboCaps=new Stack<Cap>();
        boostsAvailable=new ArrayList<Boost>();
        boostsBuffer=new Stack<Boost>();

        capManagerInit=false;

        Runnable capLoader=new Runnable() {
            @Override
            public void run() {
                ensureCapSetAssetsExist();
                buildWorkingSet();

                while(!capManagerInit) {
                    try {
                        Thread.sleep(20);

                    } catch(Exception e)
                    {

                    }
                }

                //fillCapsBuffer();
                //fillBoostsBuffer();

                delegate.onCapSetsLoadComplete();
            }
        };

        new Thread(capLoader).start();
    }

    public void putCapInPlay(Context context, Cap cap)
    {
        cap.putCapInPlay(context);
        if(currentMostPlayedCap==null || cap.numberInPlay>currentMostPlayedCap.numberInPlay)
            currentMostPlayedCap=cap;
    }

    public void removeCapFromPlay(Context context, Cap cap)
    {
        cap.removeCapFromPlay();
    }

    public int capsBufferRemaining()
    {
        return capsBuffer.size();
    }

    public boolean capSetExistsOnDisk(int setID)
    {
         // verify we have the png for each cap
        Cursor capsInSet=adapter.getCapsInSet(setID);
        while(capsInSet.moveToNext())
        {
            File f=new File(_context.getFilesDir().getPath()+"/"+capsInSet.getLong(capsInSet.getColumnIndex(BottlecapsDatabaseAdapter.KEY_ROWID))+".png");
            if(f.exists())
            {
                Log.d("CapLoader", "Cap set "+setID+" exists on disk");
                return true;
            }
        }

        //if(capSetExistsInAssets(setID))
           // return true;
        Log.d("CapLoader", "Cap set "+setID+" doesn't exist on disk");
        return false;
    }
    
    public boolean capSetExistsInAssets(int setID)
    {
        InputStream i=null;
        try {
            i=_context.getAssets().open(setID+".zip");
            
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if(i!=null) i.close();
            } catch(Exception e) {

            }

        }

        Log.d("CapLoader", "Cap set "+setID+" exists in assets");
        return true;
    }

    public boolean loadCapSetFromZip(int setID, boolean storedInAssets)
    {
        Log.d("CapLoader", "Loading cap set "+setID+" from ZIP");
        try {
            ZipInputStream zipF;

            if(storedInAssets)
                zipF=new ZipInputStream(_context.getAssets().open(setID + ".zip"));
            else
            {
                zipF=new ZipInputStream(new FileInputStream(Environment.getDownloadCacheDirectory().getPath()+"/"+setID+".zip"));
            }

            ZipEntry entry;

            while( (entry=zipF.getNextEntry()) !=null )
            {
                Log.d("CapManager", entry.getName());

                BufferedInputStream inputStream = new BufferedInputStream(zipF);
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(_context.getFilesDir().getPath()+"/"+entry.getName()));

                try {
                    IOUtils.copy(inputStream, outputStream);
                } finally {
                    outputStream.close();
                    inputStream.close();
                }
            }
        }  catch(IOException e)
        {
            Log.d("CapLoader", "Caught exception "+e.getMessage()+" while loading cap set "+setID+" from ZIP");
            e.printStackTrace();
            return false;
        }
        return true;

    }

    private void storeCapSetInDatabase(final int setID)
    {
        Log.d("CapLoader", "Storing cap set "+setID+" into database");

        GetBonkersAPI.get("/sets/"+setID, new RequestParams(), _context, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    Log.d("CapManager", response);
                   
                    JSONObject set=new JSONObject(response).getJSONObject("cap_set");

                    adapter.insertSet((long)setID, set.getString("name"), set.getString("artist"), set.getString("description"));

                    JSONArray caps=set.getJSONArray("cap");

                    for(int i=0; i<caps.length(); i++)
                    {
                        JSONObject cap=caps.getJSONObject(i);
                        adapter.insertCapIntoSet((long)setID, cap.getInt("id"), cap.getInt("available"), cap.getInt("issued"), cap.getString("name"), cap.getString("description"), cap.getInt("scarcity"));
                        Log.d("CapManager", "Cap: "+caps.getJSONObject(i).getString("name"));
                    }
                } catch(JSONException e)
                {

                } 
                catch (Exception e) {

                }
            }
        });
    }

    private void downloadCapSetAssets(int setID)
    {
        Log.d("CapLoader", "Downloading cap set "+setID);
        // check to see if there's enough space on the phone
        // if not, check if there's enough space on the SD card
        // if not, show an error to the user and abort.

        if(capSetExistsInAssets(setID))
        {
            loadCapSetFromZip(setID, true);
        }
        else
        {
            // download it from the server.
        }

        /*
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long sdBytesAvailable = (long)stat.getBlockSize() *(long)stat.getBlockCount();
        long sdCardSpaceAvailable = sdBytesAvailable / 1048576;

        StatFs phoneStat = new StatFs(Environment.getDataDirectory().getPath());
        long phoneBytesAvailable = (long)phoneStat.getBlockSize() * (long)phoneStat.getAvailableBlocks();
        long phoneSpaceAvailable = (long) (phoneBytesAvailable / (1024.f * 1024.f));
          */


    }

    public void buildWorkingSet()
    {
        int numberOfSets=4;
        long setID;

        for(int i=0; i<numberOfSets; i++)
        {
            setID=adapter.getRandomSetID();

        }
    }

    private void ensureCapSetAssetsExist()
    {
        Log.d("CapLoader", "Running ensureCapSetAssetsExist()");
        GetBonkersAPI.get("/sets", new RequestParams(), _context, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                Log.d("CapManager", response);                
                // Do we already have this set in the DB?
                try {
                    JSONArray sets=new JSONArray(response);
                    for(int i=0; i<sets.length(); i++)
                    {
                        JSONObject set=sets.getJSONObject(i).getJSONObject("cap_set");

                        int setID=set.getInt("id");
                        Cursor cursor=adapter.getSet(set.getInt("id"));
                        if(cursor.getCount()>0)
                        {
                            Log.d("CapLoader", "Cap set "+setID+" exists in the database");

                            // do we have the assets already?
                            if(!capSetExistsOnDisk(setID))
                            {
                                // download the cap set
                                downloadCapSetAssets(setID);
                                storeCapSetInDatabase(setID);
                            }
                        } else
                        {
                            Log.d("CapLoader", "Cap set "+setID+" doesn't exist in the database");

                            // if we have the assets, we need to add this set
                            // to the DB.
                            if(capSetExistsOnDisk(setID) || capSetExistsInAssets(setID))
                            {
                                if(!capSetExistsOnDisk(setID)) loadCapSetFromZip(setID, true);
                                storeCapSetInDatabase(setID);
                            }
                        }

                        //Log.d("CapManager", String.valueOf(set.getInt("cap_count")) + " caps in set "+set.getString("name")+" ("+set.getInt("id")+")");
                    }
                } catch(JSONException e)
                {
                }

            }
        });

        capManagerInit=true;
    }

    public void loadCaps()
    {
        sets=new ArrayList<Set>();
        allCaps=new ArrayList<Cap>();

        int sets[] = new int[] { 9, 24, 7, 15 };

        for(int j=0; j<sets.length; j++)
        {
            //Set set=new Set();

            for(int i=0; i<sets[j]; i++)
            {
                Cap cap=new Cap();
                cap.setNumber=j+1;
                cap.index=i+1;

                cap.resourceId=_context.getResources().getIdentifier("set"+cap.setNumber+"_"+cap.index, "drawable", "com.getbonkers.bottlecaps");

                cap.issued=(i+1)*(i+1);
                cap.available=cap.issued;

                this.circulation+=cap.issued;

                //set.capsInSet.add(cap);
                allCaps.add(cap);
            }
        }

        Collections.sort(allCaps, new CapTotalAvailableComparator());

        long min=0;
        double rarityClassD=0;

        for(int x=0; x<allCaps.size(); x++)
        {
            allCaps.get(x).probabilityMax=min+allCaps.get(x).available-1;
            allCaps.get(x).probabilityMin=min;

            min=allCaps.get(x).probabilityMax+1;

            rarityClassD=(allCaps.get(x).available/circulation)*100;
            if(rarityClassD<=5)
                allCaps.get(x).rarityClass=5;
            else if(rarityClassD<=10)
                allCaps.get(x).rarityClass=4;
            else if(rarityClassD<=25)
                allCaps.get(x).rarityClass=3;
            else if(rarityClassD<=50)
                allCaps.get(x).rarityClass=2;
            else
                allCaps.get(x).rarityClass=1;
        }

        Collections.sort(allCaps, new CapMaxProbabilityComparator());
    }

    public void prepNextBoost()
    {
        if(boostsAvailable.size()>0)
        {
            Random rand=new Random();
            boostsBuffer.push(boostsAvailable.get(rand.nextInt(boostsAvailable.size())));
        }
    }

    public void removeBoostFromAvailability(Boost boost)
    {
        boostsAvailable.remove(boost);
    }

    public synchronized void prepNextCombo(double momentum)
    {
        Random random=new Random();

        int nextComboLength;
        int[] sizeArray;

        if(level==0)
        {
            sizeArray=new int[] { 2, 2, 2, 2, 2, 2, 2, 3, 3, 4 };
        }
        else
        {
            sizeArray=new int[] { 2, 2, 2, 3, 3, 3, 4, 4, 4, 5 };
        }

        nextComboLength=sizeArray[random.nextInt(10)];

        if(nextComboLength<0)
            nextComboLength=0;
        if(nextComboLength>5)
            nextComboLength=5;

        nextComboLength+=((int)momentum)/100;

        Cap nextCap=this.getNextCap(true);

        Log.d("CapManager", "Prepping combo of cap "+nextCap.index+" (set "+nextCap.setNumber+"), size " + nextComboLength + " with momentum " + momentum);

        for(int i=0; i<nextComboLength; i++)
        {
            comboCaps.push(nextCap);
        }
        combosDelivered[nextComboLength]++;
    }

    public void fillBoostsBuffer()
    {
        boostsAvailable=new ArrayList<Boost>();
        boostsAvailable.add(new MomentumBoost());
        boostsAvailable.add(new TimeBoost());
        boostsAvailable.add(new MomentumBoost());
        boostsAvailable.add(new HighlightCombosBoost());
        boostsAvailable.add(new HighlightCombosBoost());
        // BOOM!
    }

    public void fillCapsBuffer()
    {
        Cap cap;
        int maxRange;
        int range;
        int cutStartIdx;
        int bufferLength;

        //if(allCaps.size()>=50)
        //    bufferLength=50;
        //else
        //    bufferLength=allCaps.size();
        bufferLength=30;

        ArrayList<Cap> usedCaps=new ArrayList<Cap>();

        Random random=new Random();
        maxRange=(int)circulation;

        for(int i=0; i<bufferLength; i++)
        {
            cutStartIdx=-1;

            while(cutStartIdx<0)
            {
                range=random.nextInt(maxRange);

                for(int j=0; j<allCaps.size(); j++)
                {
                    if(allCaps.get(j).probabilityMin<=range && allCaps.get(j).probabilityMax>=range && !usedCaps.contains(allCaps.get(j)))
                    {
                        cutStartIdx=j;
                        break;
                    }
                }
            }

            Log.d("CapManager", "Adding cap to buffer: "+allCaps.get(cutStartIdx).index+" (set "+allCaps.get(cutStartIdx).setNumber+")");

            capsBuffer.push(allCaps.get(cutStartIdx));
            usedCaps.add(allCaps.get(cutStartIdx));
        }
    }

    public synchronized Cap getNextCap(boolean forCombo)
    {
        Cap cap;

        if(boostsBuffer.size()>0 && !forCombo)
        {
            cap=boostsBuffer.pop();
        }
        else if(comboCaps.size()>0 && !forCombo)
        {
            cap=comboCaps.pop();
        }
        else
        {
            cap=capsBuffer.pop();

            if(capsBuffer.size()==0)
                this.fillCapsBuffer();
        }

        Log.d("CapManager", "Next cap is: "+cap.index+" (set "+cap.setNumber+") forCombo: "+String.valueOf(forCombo) );

        return cap;
    }
}