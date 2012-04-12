package com.getbonkers.bottlecaps;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.JsonWriter;
import android.util.Log;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Stack;

public class Player {
    public class PlayerDataReconciler implements Runnable {
        public static final int DATA_EARNED_CAP=1;
        public static final int DATA_COMPLETED_GOAL=2;
        public static final int DATA_EARNED_CAP_BATCH=3;
      
        public class DataReconcilerQueueItem {
            public int type;
            public long ID;
            public long[] IDs;

            public DataReconcilerQueueItem(int iType, long lID)
            {
                type=iType;
                ID=lID;
            }

            public DataReconcilerQueueItem(int iType, long[] lIDs)
            {
                type=iType;
                IDs=lIDs;
            }
        }

        private Stack<DataReconcilerQueueItem> _queue;
        private AsyncNetworkDelegate _delegate;

        private int requestsOutstanding=0;

        public PlayerDataReconciler(AsyncNetworkDelegate delegate)
        {
            _queue=new Stack<DataReconcilerQueueItem>();
            _delegate=delegate;
        }

        public void addQueueItem(int type, long id)
        {
            if(_queue==null) _queue=new Stack<DataReconcilerQueueItem>();
            _queue.add(new DataReconcilerQueueItem(type, id));
        }
        
        public void addBatchQueueItem(int type, long[] ids)
        {
            if(_queue==null) _queue=new Stack<DataReconcilerQueueItem>();
            _queue.add(new DataReconcilerQueueItem(type, ids));
        }

        private void sendCapReconcileToServer(long id)
        {
                // blah!
        }

        private void sendCapReconcileToServer(long[] ids)
        {
            JSONArray capArray=new JSONArray();
            for(int i=0; i<ids.length; i++)
            {
                try {
                    capArray.put(i, ids[i]);
                } catch(JSONException e)
                {
                    e.printStackTrace();
                }
            }

            JSONObject transaction=new JSONObject();
            try {
                transaction.put("guid", GetBonkersAPI.getPlayerUUID(_context));
                transaction.put("transaction_id", java.util.UUID.randomUUID().toString());
                transaction.put("caps", capArray);
            } catch(JSONException e)
            {
                e.printStackTrace();
            }

            GetBonkersAPI.postJson("/player_caps", transaction, _context, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(String response) {
                    requestsOutstanding--;
                }
                
                @Override
                public void onFailure(Throwable e)
                {
                    requestsOutstanding--;
                }
            });
        }

        public void run()
        {
            synchronized (_queue) {
                if(_queue.size()>0)
                {
                    while(_queue.size()>0)
                    {
                        DataReconcilerQueueItem item=_queue.pop();
                        switch(item.type)
                        {
                            case DATA_COMPLETED_GOAL:
                                break;
                            case DATA_EARNED_CAP:
                                sendCapReconcileToServer(item.ID);
                                break;
                            case DATA_EARNED_CAP_BATCH:
                                sendCapReconcileToServer(item.IDs);
                                break;
                            default:
                                break;
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
                _delegate.onQueueRunComplete();
            }
        }
    }

    private Context _context;
    private BottlecapsDatabaseAdapter _db;
    
    private Facebook facebook;
    SharedPreferences mPrefs;
    
    public static final int PLAYER_BOOST_TYPE_ALL=-1;
    public static final int PLAYER_BOOST_TYPE_NITRO=0;
    public static final int PLAYER_BOOST_TYPE_JOKER=1;
    public static final int PLAYER_BOOST_TYPE_FRENZY=2;
    public static final int PLAYER_BOOST_TYPE_MORETIME=3;
    
    public static final String[] PLAYER_BOOST_TYPE_KEYS={
            "PLAYER_BOOST_TYPE_NITRO",
            "PLAYER_BOOST_TYPE_JOKER",
            "PLAYER_BOOST_TYPE_FRENZY",
            "PLAYER_BOOST_TYPE_MORETIME" };
    
    public static final String[] PLAYER_BOOST_TYPE_NAMES={
            "Nitro",
            "Joker",
            "Frenzy",
            "Time"};

    public Player(Context context)
    {
        _context=context;
        _db=new BottlecapsDatabaseAdapter(_context);

        mPrefs = _context.getSharedPreferences("BottlecapsPlayer", Context.MODE_PRIVATE);

        validateFacebookConnection();
    }

    public boolean getUserHasRatedApp()
    {
        return mPrefs.getBoolean("ratedApp", false);
    }

    public void setUserHasRatedApp()
    {
        SharedPreferences.Editor edit=mPrefs.edit();

        edit.putBoolean("ratedApp", true);
        edit.commit();
    }
    
    public int getNumberOfCapsCollected()
    {
        _db.openReadOnly();
        int caps=(int)_db.numberOfUniqueCapsCollected();
        _db.close();
        return caps;
    }
    
    public int getTotalNumberOfCaps()
    {
        _db.openReadOnly();
        int caps=(int)_db.numberOfCapsInDatabase();
        _db.close();
        return caps;
    }
    
    public boolean hasSeenTutorial(int mode)
    {
        return mPrefs.getBoolean("seenTutorial"+mode, false);
    }
    
    public void setHasSeenTutorial(int mode)
    {
        SharedPreferences.Editor edit=mPrefs.edit();

        edit.putBoolean("seenTutorial"+mode, true);
        edit.commit();
    }

    public boolean hasAudioEnabled()
    {
        return mPrefs.getBoolean("audioEnabled", true);
    }

    public void setAudioEnabled(boolean v)
    {
        SharedPreferences.Editor edit=mPrefs.edit();
        edit.putBoolean("audioEnabled", v);
        edit.commit();
    }

    public void spendBoost(int type)
    {
        SharedPreferences.Editor edit=mPrefs.edit();
        edit.putInt(PLAYER_BOOST_TYPE_KEYS[type], numberOfBoostsForType(type)-1);
        edit.commit();
    }

    public void addBoosts(int amount, int type)
    {
        SharedPreferences.Editor edit=mPrefs.edit();

        int newAmount=0;

        if(type==PLAYER_BOOST_TYPE_ALL)
        {
            for(int i=0; i<PLAYER_BOOST_TYPE_KEYS.length; i++)
            {
                newAmount=amount;
                newAmount+=mPrefs.getInt(PLAYER_BOOST_TYPE_KEYS[i], 0);
                edit.putInt(PLAYER_BOOST_TYPE_KEYS[i], newAmount);
            }
        }
        else
        {
            newAmount=amount+mPrefs.getInt(PLAYER_BOOST_TYPE_KEYS[type], 0);
            edit.putInt(PLAYER_BOOST_TYPE_KEYS[type], newAmount);
        }

        edit.commit();
    }
    
    public int numberOfBoostsForType(int type)
    {
        return mPrefs.getInt(PLAYER_BOOST_TYPE_KEYS[type], 0);
    }

    public void addCollectedCap(long capID)
    {
        _db.open();
        _db.addCapSettlement(capID);
        _db.close();
    }

         /*
    public int unlocksAvailable()
    {
        return mPrefs.getInt("unlocksAvailable", 0);
    }      */

    
    public int capsToNextUnlock()
    {
        //long earnedCaps=_db.numberOfUniqueCapsCollected();
        //long availableCaps=_db.numberOfCapsInDatabase();
        //return (int)((Math.floor(availableCaps*0.75))-earnedCaps);

        _db.open();
        int uncol=_db.numberOfUncollectedCommonCaps();
        _db.close();
        return uncol;
    }

            /*
    public boolean awardUnlocksIfNecessary()
    {
        long earnedCaps=_db.numberOfUniqueCapsCollected();
        long availableCaps=_db.numberOfCapsInDatabase();

        int curUnlocks=this.unlocksAvailable();
        
        if(earnedCaps > Math.floor(availableCaps*0.75))
        {
            curUnlocks++;
            SharedPreferences.Editor editPrefs=mPrefs.edit();
            editPrefs.putInt("unlocksAvailable", curUnlocks);
            editPrefs.commit();
            return true;
        }

        return false;
    }
            */

    public void validateFacebookConnection()
    {
        facebook=new Facebook("220182624731035");
        String access_token = mPrefs.getString("access_token", null);
        long expires = mPrefs.getLong("access_expires", 0);
        if(access_token != null) {
            facebook.setAccessToken(access_token);
        }
        if(expires != 0) {
            facebook.setAccessExpires(expires);
        }
    }

    public boolean isConnectedToFacebook()
    {
        return facebook.isSessionValid();
    }
    
    public long getHighScore()
    {
        return mPrefs.getLong("highscore", 0);
    }
    
    public int getBiggestCombo()
    {
        return mPrefs.getInt("biggestcombo", 0);
    }
    
    public void postBiggestCombo(int biggestCombo)
    {
        int curBigCombo=getBiggestCombo();

        if(curBigCombo<biggestCombo)
        {
            SharedPreferences.Editor edit=mPrefs.edit();
            edit.putInt("biggestcombo", biggestCombo);
            edit.commit();
        }
    }

    public void invokeScorePostDialog(long score)
    {
        facebook.dialog(_context, "feed", new Facebook.DialogListener() {
            @Override
            public void onComplete(Bundle values) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void onFacebookError(FacebookError e) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void onError(DialogError e) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void onCancel() {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });
    }

    public void postScore(long score)
    {
        long hiScore=mPrefs.getLong("highscore", 0);
        if(hiScore<score)
        {
            SharedPreferences.Editor edit=mPrefs.edit();
            edit.putLong("highscore", score);
            edit.commit();
        }

        if(facebook.isSessionValid())
        {
            try {
                Bundle params=new Bundle();
                params.putString("score", String.valueOf(score));
                facebook.setAccessToken(mPrefs.getString("app_access_token", null));
                String ret=facebook.request(mPrefs.getString("facebook_id", "me")+"/scores", params, "POST");
                //Log.d("Player", ret);
            } catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        RequestParams params=new RequestParams("score", String.valueOf(score));
        params.put("guid", GetBonkersAPI.getPlayerUUID(_context));
        GetBonkersAPI.post("/scores", params, _context, new AsyncHttpResponseHandler() {
            // wutever
        });
    }

    public void reconcileCollectedCaps(AsyncNetworkDelegate completionDelegate)
    {
        PlayerDataReconciler reconciler=new PlayerDataReconciler(completionDelegate);

        _db.open();

        Cursor settlements=_db.getOutstandingCapSettlements();
        long[] batchIds=new long[settlements.getCount()];
        int i=0;
        while(settlements.moveToNext())
        {
            batchIds[i++]=settlements.getLong(settlements.getColumnIndex(BottlecapsDatabaseAdapter.KEY_SETTLEMENTS_CAP));
        }
        settlements.close();

        _db.clearCapSettlements();

        _db.close();

        reconciler.addBatchQueueItem(PlayerDataReconciler.DATA_EARNED_CAP_BATCH, batchIds);

        new Thread(reconciler).start();
    }
}
