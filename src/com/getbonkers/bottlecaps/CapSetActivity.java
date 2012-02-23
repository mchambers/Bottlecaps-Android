package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

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
                Log.d("CapSetActivity", response);
            }
        });
    }

}
