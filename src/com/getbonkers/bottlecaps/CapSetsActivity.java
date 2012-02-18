package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 2/16/12
 * Time: 6:57 PM
 * To change this template use File | Settings | File Templates.
 */

public class CapSetsActivity extends Activity {
    
    ArrayList<JSONObject> listItems;
    CapSetListAdapter adapter;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.capsets);

        ListView lv=(ListView)findViewById(R.id.capSetsListView);

        lv.setCacheColorHint(0);

        this.listItems=new ArrayList<JSONObject>();
        
        this.adapter = new CapSetListAdapter(this, listItems);

        lv.setAdapter(adapter);

        GetBonkersAPI.get("/sets", new RequestParams(), this, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                Log.d("CapManager", response);
                // Do we already have this set in the DB?
                try {
                    JSONArray sets=new JSONArray(response);
                    for(int i=0; i<sets.length(); i++)
                    {
                        JSONObject set=sets.getJSONObject(i).getJSONObject("cap_set");

                        //int setID=set.getInt("id");
                        listItems.add(set);
                    }
                    
                } catch(JSONException e)
                {
                    //
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(java.lang.Throwable throwable)
            {

            }
        });
    }
}
