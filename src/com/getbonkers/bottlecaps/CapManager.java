package com.getbonkers.bottlecaps;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

import java.lang.reflect.Array;
import java.util.*;

import static ch.lambdaj.Lambda.*;

/**
 * Created by IntelliJ IDEA.
 * User: marc
 * Date: 12/27/11
 * Time: 3:14 PM
 * To change this template use File | Settings | File Templates.
 */

public class CapManager {
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

        public boolean isCurrentlyDrawable()
        {
            if(image!=null && image.getBitmap()!=null && !image.getBitmap().isRecycled()) return true;
            return false;
        }

        public void putCapInPlay(Context context)
        {
            if(numberInPlay<=0)
            {
                this.numberInPlay++;
                BitmapFactory.Options options=new BitmapFactory.Options();
                //options.inSampleSize=4;
                this.image=new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(), this.resourceId, options));
            }
        }

        public void removeCapFromPlay()
        {
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
    private ArrayList<Cap> comboCaps;
    private ArrayList<Set> sets;
    private ArrayList<Cap> allCaps;

    public int[] combosDelivered;

    public CapManager(Context context)
    {
        _context=context;
        this.loadCaps();

        comboCaps=new ArrayList<Cap>();

        combosDelivered=new int[10];

        capsBuffer=new Stack<Cap>();

        this.fillCapsBuffer();
    }

    public int capsBufferRemaining()
    {
        return capsBuffer.size();
    }

    public void loadCaps()
    {
        sets=new ArrayList<Set>();
        allCaps=new ArrayList<Cap>();

        Set set1=new Set();

        for(int i=0; i<9; i++)
        {
            Cap cap=new Cap();
            cap.setNumber=1;
            cap.index=i+1;

            cap.resourceId=_context.getResources().getIdentifier("set"+cap.setNumber+"_"+cap.index, "drawable", "com.getbonkers.bottlecaps");
            //cap.image=new BitmapDrawable(_context.getResources(), BitmapFactory.decodeResource(_context.getResources(), cap.resourceId));

            cap.issued=(i+1)*(i+1);
            cap.available=cap.issued;

            this.circulation+=cap.issued;

            set1.capsInSet.add(cap);
            allCaps.add(cap);
        }

        sets.add(set1);

        Set set2=new Set();

        for(int i=0; i<24; i++)
        {
            Cap cap=new Cap();
            cap.setNumber=2;
            cap.index=i+1;

            cap.resourceId=_context.getResources().getIdentifier("set"+cap.setNumber+"_"+cap.index, "drawable", "com.getbonkers.bottlecaps");
            //cap.image=new BitmapDrawable(_context.getResources(), BitmapFactory.decodeResource(_context.getResources(), cap.resourceId));

            cap.issued=(long)Math.pow(i, 2); //(i+1)*(i+1);
            cap.available=cap.issued;

            this.circulation+=cap.issued;

            set2.capsInSet.add(cap);
            allCaps.add(cap);
        }

        sets.add(set2);

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

    public void prepNextCombo()
    {
        Random random=new Random();

        int nextComboLength=random.nextInt(4);
        if(nextComboLength==0)nextComboLength++;

        Cap nextCap=this.getNextCap();

        for(int i=0; i<nextComboLength; i++)
        {
            comboCaps.add(nextCap);
        }
        combosDelivered[nextComboLength]++;
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
        bufferLength=20;

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

            capsBuffer.push(allCaps.get(cutStartIdx));
            usedCaps.add(allCaps.get(cutStartIdx));
        }
    }

    public Cap getNextCap()
    {
        Cap cap;

        if(comboCaps.size()>0)
        {
            cap=comboCaps.get(0);
            comboCaps.remove(cap);
        }
        else
        {
            if(capsBuffer.size()<5)
                this.fillCapsBuffer();
            cap=capsBuffer.pop();
        }

        return cap;
    }
}
