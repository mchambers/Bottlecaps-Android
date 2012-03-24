package com.getbonkers.bottlecaps;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.util.Log;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Time;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;

import com.getbonkers.bottlecaps.GameBoardActivity.GameBoard;

/*

Add time
Joker
Hi-lite
Freeze
Momentum boost

 */

public class CapManager implements CapManagerLoadingDelegate {

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
        public long timeRemaining;
        public long interval;
        public long intervalTimer;

        public void performBoostEffects(GameBoardActivity.GameBoard board)
        {
        }
        
        public void performExpirationEffect(GameBoard board)
        {
        }

        public long getStandardDuration()
        {
            return 0;
        }

    }

    public class JokerBoost extends Boost {
        public JokerBoost()
        {
            this.index=Player.PLAYER_BOOST_TYPE_JOKER;
        }

        @Override
        public void putCapInPlay(Context context, boolean lowMemoryMode)
        {
            this.resourceId=R.drawable.boostjoker; //context.getResources().getIdentifier("boostjoker", "drawable", "com.getbonkers.bottlecaps");
            super.putCapInPlay(context, lowMemoryMode);
        }

        public boolean equals(Cap o)
        {
            return true;
        }
    }

    public class FrenzyBoost extends Boost {
        public FrenzyBoost()
        {
            this.index=Player.PLAYER_BOOST_TYPE_FRENZY;
        }
        
        @Override
        public void putCapInPlay(Context context, boolean lowMemoryMode)
        {
            this.resourceId=R.drawable.boostfrenzy;//context.getResources().getIdentifier("boostfrenzy", "drawable", "com.getbonkers.bottlecaps");
            timeRemaining=5000;
            interval=500;
            super.putCapInPlay(context, lowMemoryMode);
        }

        @Override
        public void performExpirationEffect(GameBoard board)
        {
            board.capManager.comboCaps.clear();
        }

        @Override
        public long getStandardDuration()
        {
            return 5000;
        }

        public void performBoostEffects(GameBoardActivity.GameBoard board)
        {
            prepNextCombo(board.currentMomentum);
        }
    }

    public class MomentumBoost extends Boost {
        public MomentumBoost()
        {
            this.index=Player.PLAYER_BOOST_TYPE_NITRO;
        }           

        @Override
        public void putCapInPlay(Context context, boolean lowMemoryMode)
        {
            this.resourceId=R.drawable.boostnitro; //context.getResources().getIdentifier("boostnitro", "drawable", "com.getbonkers.bottlecaps");
            super.putCapInPlay(context, lowMemoryMode);
        }

        public void performBoostEffects(GameBoardActivity.GameBoard board)
        {
            board.currentMomentum+=(100-board.currentMomentum)/2;
        }
    }

    public class TimeBoost extends Boost {
        public TimeBoost()
        {
            this.index=Player.PLAYER_BOOST_TYPE_MORETIME;
        }
        
        @Override
        public void putCapInPlay(Context context, boolean lowMemoryMode)
        {
            this.resourceId=R.drawable.boostincreasetime;//context.getResources().getIdentifier("boostincreasetime", "drawable", "com.getbonkers.bottlecaps");
            super.putCapInPlay(context, lowMemoryMode);
        }

        public void performBoostEffects(GameBoardActivity.GameBoard board)
        {
            board.gameTimers[GameBoardActivity.GameBoard.GAME_TIMER_REMAINING]+=(10*1000); // add 10 seconds
        }
    }

    /*
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
    }*/

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

                /*
                int density=_context.getResources().getDisplayMetrics().densityDpi;

                options.inDensity=160;
                options.inTargetDensity=density;
                  */

                if(_context.getResources().getDisplayMetrics().density<1.5)
                    options.inSampleSize=2;

                if(lowMemoryMode)
                {
                    if(options.inSampleSize>0)
                        options.inSampleSize*=2;
                    else
                        options.inSampleSize=2;
                }

                options.inScaled=true;

                if(this.resourceId!=0)
                {
                    this.image=new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(), this.resourceId, options));
                }
                else
                {
                    String file=_context.getFilesDir().getPath()+"/"+this.index+".png";

                    //if(lowMemoryMode)
                    this.image=new BitmapDrawable(_context.getResources(), BitmapFactory.decodeFile(file, options));
                    //else
                    //    this.image=new BitmapDrawable(_context.getResources(), file, options);
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

    /*
    public class Set {
        int id;
        ArrayList<Cap> capsInSet;

        public Set()
        {
            capsInSet=new ArrayList<Cap>();
        }
    }   */

    private boolean capManagerInit=false;
    Context _context;
    public long circulation;
    private Stack<Cap> capsBuffer;
    private Stack<Boost> boostsBuffer;
    private Stack<Cap> comboCaps;
    //private ArrayList<Set> sets;
    private ArrayList<Cap> allCaps;
    //private ArrayList<Boost> boostsAvailable;
    
    private CapDownloadManager downloadManager;
    private int capSetsDownloading;

    private int level;

    BottlecapsDatabaseAdapter adapter;
    Player p;

    Random rand=new Random();

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
        //boostsAvailable=new ArrayList<Boost>();
        boostsBuffer=new Stack<Boost>();

        _delegate=delegate;

        downloadManager=new CapDownloadManager(context, this);
        //ensureCapSetAssetsExist();

        p=new Player(_context);
        p.addBoosts(5, Player.PLAYER_BOOST_TYPE_ALL);
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

        lowMemoryMode=(percentUsed>90); // down-rez this cap image if we're close to running out of memory

        try {
            cap.putCapInPlay(context, lowMemoryMode);
        } catch(OutOfMemoryError e)
        {
            //the VM has requested we stop hemorrhaging resources.
            lowMemoryMode=true;
            try {
                cap.putCapInPlay(context, lowMemoryMode);
            } catch(OutOfMemoryError e2) {
                throw e2; // :-/
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

    public boolean capAssetExistsOnDisk(int capID)
    {
        File f=new File(_context.getFilesDir().getPath()+"/"+capID+".png");
        return f.exists();
    }

    public boolean capSetExistsOnDisk(int setID)
    {
         // verify we have the png for each cap
        Cursor capsInSet=adapter.getCapsInSet(setID);

        if(capsInSet.isLast()) 
        {
            capsInSet.close();
            return false;
        }

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

        capsInSet.close();

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
            adapter.updateSetLastPlayed(setID, new Date());
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

                if(capAssetExistsOnDisk(c.index))
                    allCaps.add(c);
            }

            actualSetAmount++;
            if(actualCapAmount>=targetCapAmount || actualSetAmount>=maxNumberOfSets) getAnotherSet=false;
            
            set.close();
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
    public void ensureCapSetAssetsExist()
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
                        cursor.close();
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
              /*
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
    }     */

    private final int BOOST_PROB_NONE=0;
    private final int BOOST_PROB_BASE=10;
    private final int BOOST_PROB_HALF=5;
    private final int BOOST_PROB_DOUBLE=20;
    private final int BOOST_PROB_NEG3X=3;

    HashMap<Integer, Integer> boostAmounts=new HashMap<Integer, Integer>();
    int[] boostPicker=new int[BOOST_PROB_DOUBLE*4];
    HashMap<Integer, Integer> boostProb=new HashMap<Integer, Integer>();

    public void prepNextBoost(GameBoard board)
    {
        int probSum=0;

        int i=0;
        int j;
        int boostTypeToPush;
        Boost theBoost;

        // Set up our default probabilities for each of these boosts
        boostProb.put(Player.PLAYER_BOOST_TYPE_NITRO, BOOST_PROB_BASE);
        boostProb.put(Player.PLAYER_BOOST_TYPE_JOKER, BOOST_PROB_BASE);
        boostProb.put(Player.PLAYER_BOOST_TYPE_FRENZY, BOOST_PROB_NONE);
        boostProb.put(Player.PLAYER_BOOST_TYPE_MORETIME, BOOST_PROB_HALF);

        // Add up the current probability total
        probSum+=BOOST_PROB_BASE;
        probSum+=BOOST_PROB_BASE;
        probSum+=BOOST_PROB_HALF;

        //  NITRO
        // 1x if momentum is <=2x, -3x if momentum > 2x, -3x in last 10 seconds
        if(board.currentMomentum>29 || board.gameTimers[GameBoard.GAME_TIMER_REMAINING]<(10*1000))
        {
            probSum-=boostProb.put(Player.PLAYER_BOOST_TYPE_NITRO, BOOST_PROB_NEG3X);
            probSum+=BOOST_PROB_NEG3X;
        }

        // FRENZY
        // -2x if under 5x
        if(board.currentMomentum<49)
        {
            probSum-=boostProb.put(Player.PLAYER_BOOST_TYPE_FRENZY, BOOST_PROB_HALF);
            probSum+=BOOST_PROB_HALF;
        }

        // TIME
        // -2x, 2x if above 4x in last 10 seconds
        if(board.currentMomentum>39 && board.gameTimers[GameBoard.GAME_TIMER_REMAINING]<(10*1000))
        {
            probSum-=boostProb.put(Player.PLAYER_BOOST_TYPE_MORETIME, BOOST_PROB_DOUBLE);
            probSum+=BOOST_PROB_DOUBLE;
        }

        for (int key : boostProb.keySet()) {
            if(p.numberOfBoostsForType(key)<=0)                // if our player doesn't own any of these boosts,
                probSum-=boostProb.put(key, BOOST_PROB_NONE);  // reduce the probability to zero.

            for(j=0; j<boostProb.get(key); j++)                // explode the boost probability into the array.
                boostPicker[i++]=key;
        }

        // if we've got any chance at finding one, pick a boost and build it
        if(probSum>0)
        {
            try {
                boostTypeToPush=boostPicker[rand.nextInt(probSum)]; // pick a random number and index into the array
                                                                    // to find which boost we're gonna use.
                theBoost=null;

                switch (boostTypeToPush)                            // in lieu of a Boost factory
                {
                    case Player.PLAYER_BOOST_TYPE_FRENZY:
                        theBoost=new FrenzyBoost();
                        break;
                    case Player.PLAYER_BOOST_TYPE_JOKER:
                        theBoost=new JokerBoost();
                        break;
                    case Player.PLAYER_BOOST_TYPE_MORETIME:
                        theBoost=new TimeBoost();
                        break;
                    case Player.PLAYER_BOOST_TYPE_NITRO:
                        theBoost=new MomentumBoost();
                        break;
                }
            } catch(Exception e)
            {
                e.printStackTrace();
                theBoost=null;
            }
        }
        else
            theBoost=null;

        // if we didn't have any eligible boosts, don't act on the boost buffer
        if(theBoost!=null)
            boostsBuffer.push(theBoost);
        else
            Log.d("CapManager", "No boost was available to push");
    }

    public void removeBoostFromAvailability(Boost boost)
    {
        p.spendBoost(boost.index);
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

        //Log.d("CapManager", "Prepping combo of cap "+nextCap.index+" (set "+nextCap.setNumber+"), size " + nextComboLength + " with momentum " + momentum);

        for(int i=0; i<nextComboLength; i++)
        {
            comboCaps.push(nextCap);
        }
        combosDelivered[nextComboLength]++;
    }

    public void fillBoostsBuffer()
    {

        /*
        boostsAvailable=new ArrayList<Boost>();
        
        Player p=new Player(_context);
        
        int numFrenzyBoosts=p.numberOfBoostsForType(Player.PLAYER_BOOST_TYPE_FRENZY);
        int numTimeBoosts=p.numberOfBoostsForType(Player.PLAYER_BOOST_TYPE_MORETIME);
        int numNitro=p.numberOfBoostsForType(Player.PLAYER_BOOST_TYPE_NITRO);
        int numJoker=p.numberOfBoostsForType(Player.PLAYER_BOOST_TYPE_JOKER);

        for(int i=0; i<numFrenzyBoosts; i++)
        {
            // add frenzies
        }
        
        for(int i=0; i<numTimeBoosts; i++)
        {
            boostsAvailable.add(new TimeBoost());
        }
        
        for(int i=0; i<numNitro; i++)
        {
            boostsAvailable.add(new MomentumBoost());
        }
        
        for(int i=0; i<numJoker; i++)
        {
            boostsAvailable.add(new JokerBoost());
        }
        */

        //YEEAAAHHHHHH
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

            //Log.d("CapManager", "Adding cap to buffer: "+allCaps.get(cutStartIdx).index+" (set "+allCaps.get(cutStartIdx).setNumber+")");

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

        //Log.d("CapManager", "Next cap is: "+cap.index+" (set "+cap.setNumber+") forCombo: "+String.valueOf(forCombo) );

        return cap;
    }
    
    static public String getUrlForCapImage(Context ctx, long capID, boolean isCollected)
    {
        int capSize;

        DisplayMetrics metrics;
        metrics=ctx.getResources().getDisplayMetrics();
        String capURL="";

        switch(metrics.densityDpi){
            case DisplayMetrics.DENSITY_LOW:
                capURL="http://data.getbonkers.com/bottlecaps/caps/75/"+capID+".png";
                capSize=48;
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                capSize=75;
                if(isCollected)
                    capURL="http://data.getbonkers.com/bottlecaps/caps/75/"+capID+".png";
                else
                    capURL="http://data.getbonkers.com/bottlecaps/caps/150_BW/"+capID+".png";
                break;
            case DisplayMetrics.DENSITY_HIGH:
                if(isCollected)
                    capURL="http://data.getbonkers.com/bottlecaps/caps/150/"+capID+".png";
                else
                    capURL="http://data.getbonkers.com/bottlecaps/caps/150_BW/"+capID+".png";
                capSize=112;
                break;
        }

        return capURL;
    }
}