package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.loopj.android.http.AsyncHttpClient;
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

    ProgressDialog wait;
    
    int mode;

    public void leftSelectorTapped(View v)
    {
        View arrow=findViewById(R.id.scoreboardHeaderSelectorArrow);

        Animation animation = AnimationUtils.loadAnimation(this,
                R.anim.slideleft);
        arrow.startAnimation(animation);

        leftSelector.setImageResource(R.drawable.selectorlon);
        rightSelector.setImageResource(R.drawable.selectorroff);

        findViewById(R.id.scoreboardList).setVisibility(View.VISIBLE);
        findViewById(R.id.scoreboardFacebookConnect).setVisibility(View.GONE);

        ((ListView)findViewById(R.id.scoreboardList)).setAdapter(allAdapter);
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
            findViewById(R.id.scoreboardFacebookConnect).setVisibility(View.GONE);
            
            ((ListView)findViewById(R.id.scoreboardList)).setAdapter(friendAdapter);
            friendAdapter.notifyDataSetChanged();
        }
        else
        {
            findViewById(R.id.scoreboardList).setVisibility(View.GONE);
            findViewById(R.id.scoreboardFacebookConnect).setVisibility(View.VISIBLE);
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
        wait.show();

        facebook.authorize(this, new String[] { "publish_actions" }, new DialogListener() {
            @Override
            public void onComplete(Bundle values) {
                // tell GetBonkers about the new FBID
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putString("access_token", facebook.getAccessToken());
                editor.putLong("access_expires", facebook.getAccessExpires());
                editor.commit();

                JSONObject meResponse;
                String appAccessToken;
                RequestParams params=new RequestParams();

                try {
                    meResponse=new JSONObject(facebook.request("me"));
                    //facebook.setAccessToken(null);
                    //facebook.setAccessExpires(0);

                    Facebook appTokenGetter=new Facebook(facebook.getAppId());

                    Bundle appTokenParams=new Bundle();
                    appTokenParams.putString("client_id", "220182624731035");
                    appTokenParams.putString("client_secret", "4eaad26ffa800e232438647bbc8af28f");
                    appTokenParams.putString("grant_type", "client_credentials");

                    appAccessToken=appTokenGetter.request("oauth/access_token", appTokenParams);

                    editor.putString("app_access_token", appAccessToken.replace("access_token=", ""));

                    Log.d("ScoreboardActivity", "Received app access token: " + appAccessToken);

                    editor.putString("facebook_id", meResponse.getString("id"));
                    editor.commit();

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
                                wait.dismiss();
                                if(mode==1)
                                {
                                    finish();
                                }
                                else
                                {
                                    updateScoreboard();
                                    rightSelectorTapped(null);
                                }
                            }
                        });
                    }
                    
                    @Override
                    public void onFailure(Throwable error)
                    {
                        wait.dismiss();
                    }
                });
            }

            @Override
            public void onFacebookError(FacebookError error) {
                wait.dismiss();
            }

            @Override
            public void onError(DialogError e) {
                wait.dismiss();
            }

            @Override
            public void onCancel() {
                wait.dismiss();
            }
        });
    }
    
    public void callForScoreUpdates(String fbFriendList)
    {
        RequestParams params=new RequestParams("friends", fbFriendList);

        GetBonkersAPI.post("/admin/players/" + GetBonkersAPI.getPlayerUUID(this) + "/rank", params, this, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject resp = new JSONObject(response);
                    JSONArray scoresAll = resp.getJSONArray("all");
                    JSONArray scoresFriends=resp.getJSONArray("friends");

                    final JSONObject myScoreData = resp.getJSONArray("me").getJSONObject(0).getJSONObject("player");

                    for (int i = 0; i < scoresAll.length(); i++) {
                        try {
                            allScores.add(scoresAll.getJSONObject(i).getJSONObject("player"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    for(int j=0; j<scoresFriends.length(); j++) {
                        try {
                            friendScores.add(scoresFriends.getJSONObject(j).getJSONObject("player"));
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

    public void updateScoreboard()
    {
        allScores.clear();
        friendScores.clear();

        allAdapter.notifyDataSetChanged();
        friendAdapter.notifyDataSetChanged();

        if(facebook.isSessionValid())
        {
            AsyncHttpClient fbRest=new AsyncHttpClient();
            fbRest.get("https://api.facebook.com/method/friends.getAppUsers?format=json&access_token="+facebook.getAccessToken(), new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(final String response)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(response.length()>=3)
                            {
                                String fixedResponse;
                                fixedResponse=response.replace("[", "");
                                fixedResponse=fixedResponse.replace("]", "");
                                callForScoreUpdates(fixedResponse);
                            }
                            else
                                callForScoreUpdates("");
                        }
                    });
                }
            });
        }
        else
        {
            callForScoreUpdates("");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.scoreboard);

        wait=new ProgressDialog(this);

        facebook=new Facebook("220182624731035");

        mPrefs = getSharedPreferences("BottlecapsPlayer", MODE_PRIVATE);
        String access_token = mPrefs.getString("access_token", null);
        long expires = mPrefs.getLong("access_expires", 0);
        if(access_token != null) {
            facebook.setAccessToken(access_token);
        }
        if(expires != 0) {
            facebook.setAccessExpires(expires);
        }

        if(getIntent().hasExtra("fbMode"))
            mode=getIntent().getExtras().getInt("fbMode", 0);
        else
            mode=0;

        if(mode==1)
        {
            connectFacebook(null);
        }

        ((TextView)findViewById(R.id.scoreboardHeaderLeftSelectorCaption)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Coolvetica.ttf"));
        ((TextView)findViewById(R.id.scoreboardHeaderRightSelectorCaption)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Coolvetica.ttf"));

        leftSelector=(ImageView)findViewById(R.id.scoreboardHeaderLeftSelector);
        rightSelector=(ImageView)findViewById(R.id.scoreboardHeaderRightSelector);

        allScores=new ArrayList<JSONObject>();
        friendScores=new ArrayList<JSONObject>();

        allAdapter=new ScoreboardListAdapter(this, allScores);
        friendAdapter=new ScoreboardListAdapter(this, friendScores);

        ListView lv=((ListView)findViewById(R.id.scoreboardList));
        lv.setAdapter(allAdapter);
        lv.setCacheColorHint(0); // fooey

        updateScoreboard();
    }


}
