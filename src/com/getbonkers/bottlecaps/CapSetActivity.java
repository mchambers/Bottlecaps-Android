package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 2/23/12
 * Time: 11:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class CapSetActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.capset);

        GetBonkersAPI.get("/sets/"+getIntent().getExtras().getLong("setID"), new RequestParams(), this, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                JSONObject capSet;

                TextView capsName=(TextView)findViewById(R.id.capSetName);
                TextView capsAmount=(TextView)findViewById(R.id.capSetDetailsTotal);
                TextView capsAuthor=(TextView)findViewById(R.id.capSetDetailsAuthor);
                TextView capsDate=(TextView)findViewById(R.id.capSetDetailsDate);
                TextView capsDetails=(TextView)findViewById(R.id.capSetQuoteText);

                capsName.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Pacifico.ttf"));
                capsAmount.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Coolvetica.ttf"));
                capsAuthor.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Coolvetica.ttf"));
                capsDate.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Coolvetica.ttf"));
                //capsDetails.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Coolvetica.ttf"));

                Log.d("CapSetActivity", response);

                try {
                    capSet=new JSONObject(response).getJSONObject("cap_set");

                    capsName.setText(capSet.getString("name"));
                    capsAmount.setText(String.valueOf(capSet.getJSONArray("cap").length()));
                    capsAuthor.setText(capSet.getString("artist"));
                    capsDetails.setText(capSet.getString("description"));

                } catch (JSONException e)
                {
                    e.printStackTrace();
                }


            }
        });
    }

}
