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

public class CapManager implements CapManagerLoadingDelegate {

    public class CapDownloadManager implements Runnable {
        public class CapDownloadManagerQueueItem {
            public long setID;
            public boolean getAssets;
            public boolean getData;
        }
        
        private int requestsOutstanding=0;
        
        public final Stack<CapDownloadManagerQueueItem> queue=new Stack<CapDownloadManagerQueueItem>();
        public CapManagerLoadingDelegate delegate;

        public CapDownloadManager(CapManagerLoadingDelegate dg) {
            delegate=dg;
        }

        private boolean loadCapSetFromZip(long setID, boolean storedInAssets)
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

                BufferedOutputStream outputStream=null;
                BufferedInputStream inputStream=null;

                while( (entry=zipF.getNextEntry()) !=null )
                {
                    Log.d("CapManager", entry.getName());

                    inputStream = new BufferedInputStream(zipF);
                    try {
                        outputStream = new BufferedOutputStream(new FileOutputStream(_context.getFilesDir().getPath()+"/"+entry.getName()));
                    } catch (FileNotFoundException e) {
                        // yeah;
                    }

                    if(outputStream!=null)
                    {
                        try {
                            IOUtils.copy(inputStream, outputStream);

                        } catch (IOException e)
                        {
                            // blah?
                        }
                        finally {
                            outputStream.close();
                            //inputStream.close();
                        }
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

        private void storeCapSetInDatabase(final long setID)
        {
            requestsOutstanding++;

            Log.d("CapLoader", "Storing cap set "+setID+" into database");

            GetBonkersAPI.get("/sets/"+setID, new RequestParams(), _context, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(String response) {
                    try {
                        Log.d("CapManager", response);

                        JSONObject set=new JSONObject(response).getJSONObject("cap_set");

                        adapter.insertSet(setID, set.getString("name"), set.getString("artist"), set.getString("description"));

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

                    requestsOutstanding--;
                }
            });
        }

        private void downloadCapSetAssets(long setID)
        {
            Log.d("CapLoader", "Downloading cap set "+setID);
            // check to see if there's enough space on the phone
            // if not, check if there's enough space on the SD card
            // if not, show an error to the user and abort.

            requestsOutstanding++;

            if(capSetExistsInAssets(setID))
            {
                loadCapSetFromZip(setID, true);
                requestsOutstanding--;
            }
            else
            {
                // download it from the server.
                requestsOutstanding--;
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
        
        public void queueSetAssetsForDownload(long setID)
        {
            synchronized (queue) {
                CapDownloadManagerQueueItem item=new CapDownloadManagerQueueItem();
                item.setID=setID;
                item.getAssets=true;
                item.getData=false;
                queue.push(item);
            }
        }

        public void queueSetDataForDownload(long setID)
        {
            synchronized (queue) {
                CapDownloadManagerQueueItem item=new CapDownloadManagerQueueItem();
                item.setID=setID;
                item.getAssets=false;
                item.getData=true;
                queue.push(item);
            }
        }

        public void run() {
            synchronized (queue) {
                if(queue.size()>0)
                {
                    while(queue.size()>0)
                    {
                        CapDownloadManagerQueueItem s=queue.pop();
                        if(s.getAssets)
                        {
                            downloadCapSetAssets(s.setID);
                        }

                        if(s.getData)
                        {
                            storeCapSetInDatabase(s.setID);
                        }
                    }

                    while(requestsOutstanding>0)
                    {
                        try {
                            Thread.sleep(500);
                        } catch(InterruptedException e) {

                        }
                    }
                }
                delegate.onCapSetLoadComplete();
            }
        }
    }

    public interface CapManagerDelegate {
        public static final int CM_STATUS_NONE=0;
        public static final int CM_STATUS_LOADING_NET=1;
        public static final int CM_STATUS_LOADING_DISK=2;
        public static final int CM_STATUS_BUILDING=3;
        public static final int CM_STATUS_COMPLETE=4;
        
        public void onCapManagerReady();
        public void onCapManagerLoadFailure(int error);
        public void capManagerProgressUpdate(int code);
        
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
        @Override
        public void putCapInPlay(Context context, boolean lowMemoryMode)
        {
            this.resourceId=context.getResources().getIdentifier("boostjoker", "drawable", "com.getbonkers.bottlecaps");
            this.index=0;
            super.putCapInPlay(context, lowMemoryMode);
        }

        public boolean equals(Cap o)
        {
            return true;
        }
    }

    public class FreezeBoost extends Boost {

        @Override
        public void putCapInPlay(Context context, boolean lowMemoryMode)
        {

        }

        public void performBoostEffects(GameBoardActivity.GameBoard board)
        {

        }
    }

    public class MomentumBoost extends Boost {

        @Override
        public void putCapInPlay(Context context, boolean lowMemoryMode)
        {
            this.resourceId=context.getResources().getIdentifier("boostnitro", "drawable", "com.getbonkers.bottlecaps");
            this.index=0;
            super.putCapInPlay(context, lowMemoryMode);
        }

        public void performBoostEffects(GameBoardActivity.GameBoard board)
        {
            board.currentMomentum+=(100-board.currentMomentum)/2;
        }
    }

    public class TimeBoost extends Boost {
        @Override
        public void putCapInPlay(Context context, boolean lowMemoryMode)
        {
            this.resourceId=context.getResources().getIdentifier("boostincreasetime", "drawable", "com.getbonkers.bottlecaps");
            this.index=0;
            super.putCapInPlay(context, lowMemoryMode);
        }

        public void performBoostEffects(GameBoardActivity.GameBoard board)
        {
            board.gameTimers[GameBoardActivity.GameBoard.GAME_TIMER_REMAINING]+=(10*1000); // add 10 seconds
        }
    }

    public class HighlightCombosBoost extends Boost {
        @Override
        public void putCapInPlay(Context context, boolean lowMemoryMode)
        {
            this.resourceId=context.getResources().getIdentifier("boosthighlight", "drawable", "com.getbonkers.bottlecaps");
            this.index=0;
            super.putCapInPlay(context, lowMemoryMode);
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
            return (o.index==this.index);
        }

        public boolean isCurrentlyDrawable()
        {
            if(image!=null && image.getBitmap()!=null && !image.getBitmap().isRecycled()) return true;
            return false;
        }

        public void putCapInPlay(Context context, boolean lowMemoryMode)
        {
            if(numberInPlay<=0)
            {
                BitmapFactory.Options options=new BitmapFactory.Options();

                if(lowMemoryMode)
                    options.inSampleSize=2;

                if(this.index==0)
                {
                    this.image=new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(), this.resourceId, options));
                }
                else
                {
                    String file=_context.getFilesDir().getPath()+"/"+this.index+".png";

                    if(lowMemoryMode)
                        this.image=new BitmapDrawable(BitmapFactory.decodeFile(_context.getFilesDir().getPath()+"/"+this.index+".png", options));
                    else
                        this.image=new BitmapDrawable(file);
                }
                //                    options.inSampleSize=4;
                //this.image=new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(), this.resourceId, options));
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
                    try {
                        image.getBitmap().recycle();
                    } catch (NullPointerException e)
                    {
                        // oops;
                    }
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
        int id;
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
    
    private CapDownloadManager downloadManager;
    private int capSetsDownloading;

    private int level;

    BottlecapsDatabaseAdapter adapter;

    private Cap currentMostPlayedCap;

    public int[] combosDelivered;

    private CapManagerDelegate _delegate;
    private boolean lowMemoryMode=false;

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

        _delegate=delegate;

        downloadManager=new CapDownloadManager(this);
        ensureCapSetAssetsExist();
    }

    public void onCapSetLoadComplete()
    {
        // we're not in the main thread here
        _delegate.capManagerProgressUpdate(CapManagerDelegate.CM_STATUS_BUILDING);
        buildWorkingSet();
    }

    public void onWorkingCapSetAvailable()
    {
        // not in the main thread here either
        //_delegate.capManagerProgressUpdate(CapManagerDelegate.CM_STATUS_COMPLETE);
        fillBoostsBuffer();
        fillCapsBuffer();
        _delegate.onCapManagerReady();
    }

    public void putCapInPlay(Context context, Cap cap)
    {
        double totalMemoryUsed = (Runtime.getRuntime().totalMemory() + android.os.Debug.getNativeHeapAllocatedSize());
        int percentUsed = (int)(totalMemoryUsed / Runtime.getRuntime().maxMemory() * 100);

        if(percentUsed>85)
            lowMemoryMode=true;
        else
            lowMemoryMode=false;

        try {
            cap.putCapInPlay(context, lowMemoryMode);
        } catch(OutOfMemoryError e)
        {
            //the VM has requested we stop hemorrhaging resources.
            lowMemoryMode=true;
            try {
                cap.putCapInPlay(context, lowMemoryMode);
            } catch(OutOfMemoryError e2) {
                throw e2; // fuck, so much for "low memory mode."
            }
        }
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

        if(capsInSet.isLast()) return false;

        while(capsInSet.moveToNext())
        {
            File f=new File(_context.getFilesDir().getPath()+"/"+capsInSet.getLong(capsInSet.getColumnIndex(BottlecapsDatabaseAdapter.KEY_ROWID))+".png");
            if(!f.exists())
            {
                Log.d("CapLoader", "Cap set "+setID+" doesn't exist on disk");
                return false;
            }
            else
            {
                Log.d("CapLoader", "Cap set "+setID+" exists on disk");
            }
        }

        //if(capSetExistsInAssets(setID))
           // return true;
        return true;
    }
    
    public boolean capSetExistsInAssets(long setID)
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



    public void buildWorkingSet()
    {
        long setID;
        int targetCapAmount=125;
        int maxNumberOfSets=7;
        int actualSetAmount=0;
        int actualCapAmount=0;
        boolean getAnotherSet=true;

        allCaps=new ArrayList<Cap>();

        while(getAnotherSet)
        {
            setID=adapter.getRandomSetID();
            Cursor set=adapter.getCapsInSet(setID);
            actualCapAmount+=set.getCount();

            while(set.moveToNext())
            {
                Cap c=new Cap();
                c.available=set.getInt(set.getColumnIndex(BottlecapsDatabaseAdapter.KEY_CAPS_AVAILABLE));
                c.issued=set.getInt(set.getColumnIndex(BottlecapsDatabaseAdapter.KEY_CAPS_ISSUED));
                this.circulation+=c.issued;
                c.rarityClass=set.getInt(set.getColumnIndex(BottlecapsDatabaseAdapter.KEY_CAPS_SCARCITY));
                c.index=set.getInt(set.getColumnIndex(BottlecapsDatabaseAdapter.KEY_ROWID));
                c.resourceId=0;
                
                allCaps.add(c);
            }

            actualSetAmount++;
            if(actualCapAmount>=targetCapAmount || actualSetAmount>=maxNumberOfSets) getAnotherSet=false;
        }

        Collections.sort(allCaps, new CapTotalAvailableComparator());

        long min=0;

        for(int x=0; x<allCaps.size(); x++)
        {
            allCaps.get(x).probabilityMax=min+allCaps.get(x).available-1;
            allCaps.get(x).probabilityMin=min;

            min=allCaps.get(x).probabilityMax+1;
        }

        Collections.sort(allCaps, new CapMaxProbabilityComparator());

        this.onWorkingCapSetAvailable();
    }

    /*
     * in this function, we sweep through the sets in the database and see what assets we're missing.
     * we also sweep through the content that comes pre-loaded and make sure there's database
     * entries for them.
     */
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
                        if(cursor.getCount()>0 || capSetExistsInAssets(setID))
                        {
                            if(cursor.getCount() <=0) {
                                downloadManager.queueSetDataForDownload(setID);
                                downloadManager.queueSetAssetsForDownload(setID);
                            }
                            else
                                Log.d("CapLoader", "Cap set "+setID+" exists in the database");

                            // do we have the assets already?
                            if(!capSetExistsOnDisk(setID))
                            {
                                downloadManager.queueSetAssetsForDownload(setID);
                            }
                        }
                    }
                } catch(JSONException e)
                {
                    _delegate.onCapManagerLoadFailure(2);
                }

                new Thread(downloadManager).start();

            }

            @Override
            public void onFailure(java.lang.Throwable throwable)
            {
                _delegate.onCapManagerLoadFailure(1);
            }
        });
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