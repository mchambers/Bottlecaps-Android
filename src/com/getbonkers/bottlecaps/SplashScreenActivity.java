package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

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
    }

    public void onKidsModeClick(View v)
    {
        Intent kidsModeIntent=new Intent(this, GameBoardActivity.class);
        kidsModeIntent.putExtra("GAME_DIFFICULTY", 0);
        this.startActivity(kidsModeIntent);
    }

    public void onPlayButtonClick(View v)
    {
        Intent playIntent = new Intent(this, GameBoardActivity.class);
        playIntent.putExtra("GAME_DIFFICULTY", 1);
        this.startActivity(playIntent);
    }
}
