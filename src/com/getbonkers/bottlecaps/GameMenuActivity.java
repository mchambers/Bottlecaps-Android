package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 2/13/12
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class GameMenuActivity extends Activity {
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    public void onMyCapsButtonPressed(View v) {
        Intent i=new Intent(this, CapSetsActivity.class);
        startActivity(i);
    }
}
