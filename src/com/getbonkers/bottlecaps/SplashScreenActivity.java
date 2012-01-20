package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.app.ProgressDialog;
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


            }
        });
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
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
