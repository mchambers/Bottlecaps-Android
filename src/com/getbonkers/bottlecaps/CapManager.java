package com.getbonkers.bottlecaps;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import java.lang.reflect.Array;
import java.sql.Time;
import java.util.*;

/*

Add time
Joker
Hi-lite
Freeze
Momentum boost

 */
public class CapManager {
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

    private final boolean capManagerInit=false;
    Context _context;
    public long circulation;
    private Stack<Cap> capsBuffer;
    private Stack<Boost> boostsBuffer;
    private Stack<Cap> comboCaps;
    private ArrayList<Set> sets;
    private ArrayList<Cap> allCaps;
    private ArrayList<Boost> boostsAvailable;

    private int level;

    private Cap currentMostPlayedCap;

    public int[] combosDelivered;

    public CapManager(Context context, int difficulty)
    {
        _context=context;

        level = difficulty;

        combosDelivered=new int[10];

        capsBuffer=new Stack<Cap>();
        comboCaps=new Stack<Cap>();
        boostsAvailable=new ArrayList<Boost>();
        boostsBuffer=new Stack<Boost>();

        this.loadCaps();

        this.fillCapsBuffer();
        this.fillBoostsBuffer();
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