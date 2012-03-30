package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import com.flurry.android.FlurryAgent;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.image.SmartImageView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;

public class CapSetActivity extends FragmentActivity implements CapManagerLoadingDelegate {

    public class CapSetPagerAdapter extends FragmentPagerAdapter {
        public class CapSetPageFragment extends Fragment {
            private long capID1;
            private long capID2;
            
            private int cap1Rarity;
            private int cap2Rarity;
            
            private int cap1Availability;
            private int cap2Availability;

            @Override
            public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                capID1=getArguments().getLong("capID1");
                capID2=getArguments().getLong("capID2");
                
                cap1Rarity=getArguments().getInt("cap1_rarity");
                cap2Rarity=getArguments().getInt("cap2_rarity");

                cap1Availability=getArguments().getInt("cap1_availability");
                cap2Availability=getArguments().getInt("cap2_availability");
            }
            
            private String getCapUrl(long capId, boolean isCollected)
            {
                int capSize;

                DisplayMetrics metrics;
                metrics=this.getResources().getDisplayMetrics();
                String capURL="";

                switch(metrics.densityDpi){
                    case DisplayMetrics.DENSITY_LOW:
                        capURL="http://data.getbonkers.com/bottlecaps/caps/75/"+capId+".png";
                        capSize=48;
                        break;
                    case DisplayMetrics.DENSITY_MEDIUM:
                        capSize=75;
                        if(isCollected)
                            capURL="http://data.getbonkers.com/bottlecaps/caps/150/"+capId+".png";
                        else
                            capURL="http://data.getbonkers.com/bottlecaps/caps/150_BW/"+capId+".png";
                        break;
                    case DisplayMetrics.DENSITY_HIGH:
                        if(isCollected)
                            capURL="http://data.getbonkers.com/bottlecaps/caps/250/"+capId+".png";
                        else
                            capURL="http://data.getbonkers.com/bottlecaps/caps/250_BW/"+capId+".png";
                        capSize=112;
                        break;
                }

                return capURL;
            }
            
            private String rarityToString(int rarity)
            {
                switch(rarity)
                {
                    case 1:
                        return "Common";
                    case 2:
                        return "Uncommon";
                    case 3:
                        return "Rare";
                    case 4:
                        return "Very Rare";
                    case 5:
                        return "Uber Rare";
                }

                return "";
            }

            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                     Bundle savedInstanceState) {
                super.onCreateView(inflater, container, savedInstanceState);

                View v=inflater.inflate(R.layout.pager_caps, container, false);

                SmartImageView cap1Image=(SmartImageView)v.findViewById(R.id.capPagerCap1Image);
                SmartImageView cap2Image=(SmartImageView)v.findViewById(R.id.capPagerCap2Image);

                Cursor cap1=db.getCap(capID1);
                Cursor cap2=db.getCap(capID2);

                TextView cap1AvailabilityTV=(TextView)v.findViewById(R.id.capPagerCap1Availability);
                TextView cap2AvailabilityTV=(TextView)v.findViewById(R.id.capPagerCap2Availability);
                TextView cap1RarityTV=(TextView)v.findViewById(R.id.capPagerCap1Rarity);
                TextView cap2RarityTV=(TextView)v.findViewById(R.id.capPagerCap2Rarity);

                cap1Image.setImageUrl(getCapUrl(capID1, db.capIsCollected(capID1)));
                cap2Image.setImageUrl(getCapUrl(capID2, db.capIsCollected(capID2)));

                cap1RarityTV.setText(rarityToString(cap1Rarity));
                cap2RarityTV.setText(rarityToString(cap2Rarity));

                if(cap1Rarity==1)
                    cap1AvailabilityTV.setText("Unlimited\nin circulation");
                else
                    cap1AvailabilityTV.setText(NumberFormat.getInstance().format(cap1Availability)+"\nin circulation");
                
                if(cap2Rarity==1)
                    cap2AvailabilityTV.setText("Unlimited\nin circulation");
                else
                    cap2AvailabilityTV.setText(NumberFormat.getInstance().format(cap2Availability)+"\nin circulation");

                cap1.close();
                cap2.close();

                return v;
            }
        }

        protected JSONArray caps;

        public CapSetPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setCaps(JSONArray v)
        {
            caps=v;
        }

        public JSONArray getCaps()
        {
            return caps;
        }

        @Override
        public int getCount() {
            try {
                return caps.length()/2;
            } catch(Exception e)
            {
                return 0;
            }
        }

        @Override
        public Fragment getItem(int position) {
            Fragment capSetPage=new CapSetPageFragment();

            Bundle args=new Bundle();
            
            try {
                args.putLong("capID1", caps.getJSONObject(position*2).getLong("id"));
                args.putLong("capID2", caps.getJSONObject((position * 2) + 1).getLong("id"));
                
                args.putInt("cap1_rarity", caps.getJSONObject(position*2).getInt("scarcity"));
                args.putInt("cap2_rarity", caps.getJSONObject((position*2) + 1).getInt("scarcity"));
                args.putInt("cap1_availability", caps.getJSONObject(position*2).getInt("available"));
                args.putInt("cap2_availability", caps.getJSONObject((position*2) + 1).getInt("available"));
            }
            catch (Exception e) {
               e.printStackTrace();
            }

            capSetPage.setArguments(args);

            return capSetPage;
        }
    }

    BottlecapsDatabaseAdapter db=new BottlecapsDatabaseAdapter(this);
    long setID;
    ProgressDialog dialog;
    
    ViewPager capDetails;
    View artistDetails;

    CapSetPagerAdapter adapter;

    public void artistDetailsClick(View v)
    {
        View arrow=findViewById(R.id.capsetsHeaderSelectorArrow);

        Animation animation = AnimationUtils.loadAnimation(this,
                R.anim.slideright);
        arrow.startAnimation(animation);
        
        ((ImageView)findViewById(R.id.capsetsHeaderRightSelector)).setImageResource(R.drawable.selectorron);
        ((ImageView)findViewById(R.id.capsetsHeaderLeftSelector)).setImageResource(R.drawable.selectorloff);

        artistDetails.setVisibility(View.VISIBLE);
        capDetails.setVisibility(View.GONE);
    }
    
    public void capDetailsClick(View v)
    {
        View arrow=findViewById(R.id.capsetsHeaderSelectorArrow);

        Animation animation = AnimationUtils.loadAnimation(this,
                R.anim.slideleft);
        arrow.startAnimation(animation);

        ((ImageView)findViewById(R.id.capsetsHeaderRightSelector)).setImageResource(R.drawable.selectorroff);
        ((ImageView)findViewById(R.id.capsetsHeaderLeftSelector)).setImageResource(R.drawable.selectorlon);

        capDetails.setVisibility(View.VISIBLE);
        artistDetails.setVisibility(View.GONE);
    }
    
    @Override
    public void onCapSetLoadComplete()
    {
        dialog.dismiss();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshSetData();
            }
        });
    }

    @Override
    public void onWorkingCapSetAvailable()
    {
        // :-/
    }

    private void refreshSetData()
    {
        GetBonkersAPI.get("/sets/"+setID, new RequestParams(), this, new AsyncHttpResponseHandler() {
            @Override
            public void onFailure(Throwable e)
            {
            }

            @Override
            public void onSuccess(String response) {
                JSONObject capSet;
                
                Player p=new Player(getBaseContext());

                Log.d("CapSetActivity", response);

                TextView capsName=(TextView)findViewById(R.id.capSetName);
                TextView capsAmount=(TextView)findViewById(R.id.capSetCollected);
                TextView capsTotal=(TextView)findViewById(R.id.capSetTotal);

                //TextView capsAuthor=(TextView)findViewById(R.id.capSetDetailsAuthor);
                //TextView capsDate=(TextView)findViewById(R.id.capSetDetailsDate);
                TextView capsDetails=(TextView)findViewById(R.id.capSetQuoteText);
                //TextView capsAmountDetails=(TextView)findViewById(R.id.capSetNumberDetails);

                capsName.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Pacifico.ttf"));
                capsAmount.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Coolvetica.ttf"));
                capsTotal.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Coolvetica.ttf"));

                //capsAuthor.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Coolvetica.ttf"));
                //capsDate.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Coolvetica.ttf"));
                //capsDetails.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Coolvetica.ttf"));

                if(db.setExistsInDatabase(setID))
                {
                    findViewById(R.id.capsetUnlockButton).setVisibility(View.GONE);
                    findViewById(R.id.capsetLockedIndicator).setVisibility(View.GONE);
                    
                    findViewById(R.id.capSetDetails).setVisibility(View.VISIBLE);
                }
                else
                {
                    findViewById(R.id.capSetDetails).setVisibility(View.INVISIBLE);

                    if(p.capsToNextUnlock()<=0)
                    {
                        findViewById(R.id.capsetUnlockButton).setVisibility(View.VISIBLE);
                        findViewById(R.id.capsetLockedIndicator).setVisibility(View.GONE);
                    }
                    else
                    {
                        findViewById(R.id.capsetLockedIndicator).setVisibility(View.VISIBLE);
                        findViewById(R.id.capsetUnlockButton).setVisibility(View.GONE);
                    }
                }

                try {
                    capSet=new JSONObject(response).getJSONObject("cap_set");

                    adapter.setCaps(capSet.getJSONArray("caps"));
                    adapter.notifyDataSetChanged();

                    capsName.setText(capSet.getString("name"));
                    capsAmount.setText(String.valueOf(db.capsCollectedInSet(getIntent().getExtras().getLong("setID"))));
                    //capsAmountDetails.setText("collected of "+capSet.getJSONArray("caps").length() + " in set");
                    //capsAmount.setText(String.valueOf(capSet.getJSONArray("caps").length()));
                    //capsAuthor.setText(capSet.getString("artist"));
                    capsTotal.setText(String.valueOf(capSet.getJSONArray("caps").length()));

                    capsDetails.setText(capSet.getString("description"));
                } catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
        db.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        FlurryAgent.onStartSession(this, "LG9MLAYBEKLAFWLBMDAJ");

        db.open();

        setContentView(R.layout.capset);

        setID=getIntent().getExtras().getLong("setID");

        artistDetails=findViewById(R.id.capSetQuoteContent);
        capDetails=(ViewPager)findViewById(R.id.capSetPager);
        
        adapter=new CapSetPagerAdapter(getSupportFragmentManager());
        capDetails.setAdapter(adapter);

        refreshSetData();
    }

    public void unlockThisSet(View v)
    {
        CapDownloadManager dlMgr=new CapDownloadManager(this, this);

        dlMgr.queueSetDataForDownload(setID);
        dlMgr.queueSetAssetsForDownload(setID);
        
        dialog=new ProgressDialog(this);
        dialog.show();

        new Thread(dlMgr).start();
    }
}
