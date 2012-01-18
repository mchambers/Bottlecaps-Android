package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.JsonReader;
import android.view.View;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: marc
 * Date: 12/27/11
 * Time: 6:44 PM
 * To change this template use File | Settings | File Templates.
 */

public class SplashScreenActivity extends Activity {
    boolean readyToGo;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashscreen);

        GetBonkersAPI.initPersistentCookieStore(getApplicationContext());

        RequestParams params=new RequestParams();
        params.put("user[email]", "android@getbonkers.com");
        params.put("user[password]", "Par1sH1lt0n");
        params.put("format", "json");

        GetBonkersAPI.post("/admin/login", params, this, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                Log.d("SplashScreen", response);

                GetBonkersAPI.get("/sets", new RequestParams(), getApplicationContext(), new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(String response) {
                        Log.d("SplashScreen", response);
                        try {
                            JSONArray sets=new JSONArray(response);
                            for(int i=0; i<sets.length(); i++)
                            {
                                JSONObject set=sets.getJSONObject(i).getJSONObject("cap_set");
                                Log.d("SplashScreen", String.valueOf(set.getInt("cap_count")) + " caps in set "+set.getString("name")+" ("+set.getInt("id")+")");
                                GetBonkersAPI.get("/sets/"+set.getInt("id"), new RequestParams(), getApplicationContext(), new AsyncHttpResponseHandler() {
                                    @Override
                                    public void onSuccess(String response) {
                                        try {
                                            Log.d("SplashScreen", response);
                                            JSONObject set=new JSONObject(response).getJSONObject("cap_set");
                                            JSONArray caps=set.getJSONArray("cap");
                                            for(int i=0; i<caps.length(); i++)
                                            {
                                                Log.d("SplashScreen", "Cap: "+caps.getJSONObject(i).getString("name"));
                                            }

                                        } catch(JSONException e)
                                        {

                                        }
                                    }
                                });
                            }
                        } catch(JSONException e)
                        {
                        }
                    }
                });
            }
        });
    }

    public void onKidsModeClick(View v)
    {
        //if(!readyToGo) return;

        Intent kidsModeIntent=new Intent(this, GameBoardActivity.class);
        kidsModeIntent.putExtra("GAME_DIFFICULTY", 0);
        this.startActivity(kidsModeIntent);
    }

    public void onPlayButtonClick(View v)
    {
        //if(!readyToGo) return;

        Intent playIntent = new Intent(this, GameBoardActivity.class);
        playIntent.putExtra("GAME_DIFFICULTY", 1);
        this.startActivity(playIntent);
    }
}
