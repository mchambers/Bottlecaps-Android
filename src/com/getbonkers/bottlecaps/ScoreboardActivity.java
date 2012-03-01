package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.facebook.android.*;
import com.facebook.android.Facebook.*;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 2/27/12
 * Time: 3:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScoreboardActivity extends Activity {
    
    ArrayList<JSONObject> allScores;
    ArrayList<JSONObject> friendScores;
    ScoreboardListAdapter allAdapter;
    ScoreboardListAdapter friendAdapter;
    
    Facebook facebook;
    
    ImageView leftSelector;
    ImageView rightSelector;

    SharedPreferences mPrefs;

    public void leftSelectorTapped(View v)
    {
        View arrow=findViewById(R.id.scoreboardHeaderSelectorArrow);

        Animation animation = AnimationUtils.loadAnimation(this,
                R.anim.slideleft);
        arrow.startAnimation(animation);

        leftSelector.setImageResource(R.drawable.selectorlon);
        rightSelector.setImageResource(R.drawable.selectorroff);
    }

    public void rightSelectorTapped(View v)
    {
        View arrow=findViewById(R.id.scoreboardHeaderSelectorArrow);

        Animation animation = AnimationUtils.loadAnimation(this,
                R.anim.slideright);
        arrow.startAnimation(animation);

        leftSelector.setImageResource(R.drawable.selectorloff);
        rightSelector.setImageResource(R.drawable.selectorron);

        if(facebook.isSessionValid())
        {
            findViewById(R.id.scoreboardList).setVisibility(View.VISIBLE);
        }
        else
        {
            findViewById(R.id.scoreboardList).setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        facebook.authorizeCallback(requestCode, resultCode, data);
    }

    /*
    NSURL *url = [NSURL URLWithStringNSString stringWithFormat"%@/admin/players/%@",kServerUrl,AppDelegate.user.uuid]];

ASIFormDataRequest *request = [ASIFormDataRequest requestWithURL:url];

[request addPostValueuser objectForKey"id"] forKey"facebook_id"];
[request addPostValueuser objectForKey"name"] forKey"name"];
[request addPostValueuser objectForKey"picture"] forKey"avatar"];
[request startAsynchronous];
     */
    public void connectFacebook(View v)
    {
        facebook.authorize(this, new DialogListener() {
            @Override
            public void onComplete(Bundle values) {
                // tell GetBonkers about the new FBID
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putString("access_token", facebook.getAccessToken());
                editor.putLong("access_expires", facebook.getAccessExpires());
                editor.commit();

                JSONObject meResponse;
                RequestParams params=new RequestParams();

                try {
                    meResponse=new JSONObject(facebook.request("me"));
                    params.put("facebook_id", meResponse.getString("id"));
                    params.put("name", meResponse.getString("name"));
                    params.put("avatar", "http://graph.facebook.com/"+meResponse.getString("id")+"/picture");
                } catch(Exception e)
                {
                    e.printStackTrace();
                }

                GetBonkersAPI.post("/admin/players/"+GetBonkersAPI.getPlayerUUID(getApplicationContext()), params, getApplicationContext(), new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(String response)
                    {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateScoreboard();
                            }
                        });
                    }
                });
            }

            @Override
            public void onFacebookError(FacebookError error) {}

            @Override
            public void onError(DialogError e) {}

            @Override
            public void onCancel() {}
        });
    }

    public void updateScoreboard()
    {
        RequestParams params=new RequestParams("friends", "");

        GetBonkersAPI.post("/admin/players/" + GetBonkersAPI.getPlayerUUID(this) + "/rank", params, this, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject resp = new JSONObject(response);
                    JSONArray scoresAll = resp.getJSONArray("all");
                    final JSONObject myScoreData = resp.getJSONArray("me").getJSONObject(0).getJSONObject("player");

                    for (int i = 0; i < scoresAll.length(); i++) {
                        try {
                            allScores.add(scoresAll.getJSONObject(i).getJSONObject("player"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView myRank = (TextView) findViewById(R.id.scoreboardMyScoreRank);
                            TextView myScore = (TextView) findViewById(R.id.scoreboardMyScoreScore);

                            try {
                                myRank.setText(String.valueOf(myScoreData.getLong("rank")));
                            } catch (JSONException e) {

                            }

                            allAdapter.notifyDataSetChanged();
                        }
                    });

                } catch (JSONException e) {

                }

                Log.d("ScoreboardActivity", response);
                Log.d("ScoreboardActivity", GetBonkersAPI.getPlayerUUID(getApplicationContext()));
            }

            @Override
            public void onFailure(java.lang.Throwable throwable) {
                Log.d("ScoreboardActivity", throwable.getMessage());
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.scoreboard);

        facebook=new Facebook("220182624731035");

        mPrefs = getPreferences(MODE_PRIVATE);
        String access_token = mPrefs.getString("access_token", null);
        long expires = mPrefs.getLong("access_expires", 0);
        if(access_token != null) {
            facebook.setAccessToken(access_token);
        }
        if(expires != 0) {
            facebook.setAccessExpires(expires);
        }

        leftSelector=(ImageView)findViewById(R.id.scoreboardHeaderLeftSelector);
        rightSelector=(ImageView)findViewById(R.id.scoreboardHeaderRightSelector);

        allScores=new ArrayList<JSONObject>();

        allAdapter=new ScoreboardListAdapter(this, allScores);

        ((ListView)findViewById(R.id.scoreboardList)).setAdapter(allAdapter);

        updateScoreboard();
    }


}
