package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.flurry.android.FlurryAgent;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 2/13/12
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class GameMenuActivity extends Activity {
    @Override
    public void onStop()
    {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        FlurryAgent.onStartSession(this, "LG9MLAYBEKLAFWLBMDAJ");
        setContentView(R.layout.main);
    }
    
    public void onBoostsButtonPressed(View v) {
        Intent i=new Intent(this, BoostsActivity.class);
        startActivity(i);
    }

    public void onMyCapsButtonPressed(View v) {
        boolean runTutorial;
        Intent i;
        
        Player p=new Player(this);

        if(!p.hasSeenTutorial(TutorialActivity.MODE_CAPMANAGER))
        {
            p.setHasSeenTutorial(TutorialActivity.MODE_CAPMANAGER);
            i=new Intent(this, TutorialActivity.class);
            i.putExtra("mode", TutorialActivity.MODE_CAPMANAGER);
        }
        else
        {
            i=new Intent(this, CapSetsActivity.class);
        }

        startActivity(i);
    }
    
    public void onHelpButtonPressed(View v)
    {
        Intent i=new Intent(this, TutorialActivity.class);
        i.putExtra("mode", 2);
        startActivity(i);
    }
    
    public void onScoreboardButtonPressed(View v) {
        Intent i=new Intent(this, ScoreboardActivity.class);
        startActivity(i);
    }
    
    public void onHomeButtonPressed(View v) {
        finish();
    }
}
