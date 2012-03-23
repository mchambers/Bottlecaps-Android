package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
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
    
    ArrayList<JSONObject> unlockedSets;
    ArrayList<JSONObject> lockedSets;
    CapSetListAdapter lockedAdapter;
    CapSetListAdapter unlockedAdapter;
    
    ListView lv;
    
    View            listHeader;
    ImageView       leftSelector;
    ImageView       rightSelector;

    BottlecapsDatabaseAdapter db=new BottlecapsDatabaseAdapter(this);

    public void leftSelectorTapped(View v)
    {
        if(!lv.getAdapter().equals(unlockedAdapter))
        {
            lv.setAdapter(unlockedAdapter);
            unlockedAdapter.notifyDataSetChanged();

            View arrow=listHeader.findViewById(R.id.capsetsHeaderSelectorArrow);
            
            listHeader.findViewById(R.id.capsetsHeaderUnlocks).setVisibility(View.GONE);

            Animation animation = AnimationUtils.loadAnimation(this,
                    R.anim.slideleft);
            arrow.startAnimation(animation);

            leftSelector.setImageResource(R.drawable.selectorlon);
            rightSelector.setImageResource(R.drawable.selectorroff);
        }
    }

    public void rightSelectorTapped(View v)
    {
        if(!lv.getAdapter().equals(lockedAdapter))
        {
            lv.setAdapter(lockedAdapter);
            lockedAdapter.notifyDataSetChanged();

            View arrow=listHeader.findViewById(R.id.capsetsHeaderSelectorArrow);

            listHeader.findViewById(R.id.capsetsHeaderUnlocks).setVisibility(View.VISIBLE);

            Animation animation = AnimationUtils.loadAnimation(this,
                    R.anim.slideright);
            arrow.startAnimation(animation);

            leftSelector.setImageResource(R.drawable.selectorloff);
            rightSelector.setImageResource(R.drawable.selectorron);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Player p;
        
        p=new Player(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.capsets);

        lv=(ListView)findViewById(R.id.capSetsListView);

        lv.setDivider(null);

        lv.setCacheColorHint(0);

        this.lockedSets=new ArrayList<JSONObject>();
        this.unlockedSets=new ArrayList<JSONObject>();

        this.lockedAdapter=new CapSetListAdapter(this, lockedSets);
        this.unlockedAdapter=new CapSetListAdapter(this, unlockedSets);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        View v=inflater.inflate(R.layout.capsets_header, null);

        leftSelector=(ImageView)v.findViewById(R.id.capsetsHeaderLeftSelector);
        rightSelector=(ImageView)v.findViewById(R.id.capsetsHeaderRightSelector);

        ((TextView)v.findViewById(R.id.capsetsHeaderMyCapsCaption)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Pacifico.ttf"));
        ((TextView)v.findViewById(R.id.capsetsHeaderLeftSelectorCaption)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Coolvetica.ttf"));
        ((TextView)v.findViewById(R.id.capsetsHeaderRightSelectorCaption)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Coolvetica.ttf"));

        int capsToNext=p.capsToNextUnlock();

        ((TextView)v.findViewById(R.id.capsetsHeaderUnlocks)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Coolvetica.ttf"));

        if(capsToNext>0)
        {
            ((TextView)v.findViewById(R.id.capsetsHeaderUnlocks)).setText("Collect "+capsToNext+" more Common caps to unlock a set!");
        }
        else
        {
            ((TextView)v.findViewById(R.id.capsetsHeaderUnlocks)).setText("Select a locked set to unlock it!");
        }

        lv.addHeaderView(v, null, false);
        lv.setAdapter(unlockedAdapter);
        lv.setItemsCanFocus(false);

        listHeader=v;

        db.open();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                JSONObject item = (JSONObject) lv.getAdapter().getItem(position);
                try {
                    Intent capSetIntent=new Intent(getApplicationContext(), CapSetActivity.class);
                    capSetIntent.putExtra("setID", item.getLong("id"));
                    startActivity(capSetIntent);
                } catch (JSONException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        });

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

                        if(db.setExistsInDatabase(set.getInt("id")))
                            unlockedSets.add(set);
                        else
                            lockedSets.add(set);
                    }
                } catch(JSONException e)
                {
                    //
                }

                db.close();

                unlockedAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(java.lang.Throwable throwable)
            {
                   db.close();
            }
        });
    }
}
