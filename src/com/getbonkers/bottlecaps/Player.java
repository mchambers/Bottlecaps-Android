package com.getbonkers.bottlecaps;

import android.content.Context;
import android.database.Cursor;
import android.util.JsonWriter;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 2/6/12
 * Time: 2:18 PM
 * To change this template use File | Settings | File Templates.
 */
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

    public Player(Context context)
    {
        _context=context;
        _db=new BottlecapsDatabaseAdapter(_context);
        _db.open();
    }

    public void addCollectedCap(long capID)
    {
        _db.addCapSettlement(capID);
    }

    public void postScore(long score)
    {
        RequestParams params=new RequestParams("score", String.valueOf(score));
        params.put("guid", GetBonkersAPI.getPlayerUUID(_context));
        GetBonkersAPI.post("/scores", params, _context, new AsyncHttpResponseHandler() {
            // wutever
        });
    }

    public void reconcileCollectedCaps(AsyncNetworkDelegate completionDelegate)
    {
        PlayerDataReconciler reconciler=new PlayerDataReconciler(completionDelegate);

        Cursor settlements=_db.getOutstandingCapSettlements();
        long[] batchIds=new long[settlements.getCount()];
        int i=0;
        while(settlements.moveToNext())
        {
            batchIds[i++]=settlements.getLong(settlements.getColumnIndex(BottlecapsDatabaseAdapter.KEY_SETTLEMENTS_CAP));
        }
        settlements.close();

        _db.clearCapSettlements();

        reconciler.addBatchQueueItem(PlayerDataReconciler.DATA_EARNED_CAP_BATCH, batchIds);

        new Thread(reconciler).start();
    }
}
