package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
    ScoreboardListAdapter adapter;
    
    Facebook facebook;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        facebook.authorizeCallback(requestCode, resultCode, data);
    }

    public void facebookConnect()
    {
        facebook.authorize(this, new DialogListener() {
            @Override
            public void onComplete(Bundle values) {
                // tell GetBonkers about the new FBID
            }

            @Override
            public void onFacebookError(FacebookError error) {}

            @Override
            public void onError(DialogError e) {}

            @Override
            public void onCancel() {}
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.scoreboard);
        
        facebook=new Facebook("220182624731035");

        allScores=new ArrayList<JSONObject>();

        adapter=new ScoreboardListAdapter(this, allScores);

        ((ListView)findViewById(R.id.scoreboardList)).setAdapter(adapter);

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

                            adapter.notifyDataSetChanged();
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


}
