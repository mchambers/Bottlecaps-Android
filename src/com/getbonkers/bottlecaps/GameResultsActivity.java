package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * Created by IntelliJ IDEA.
 * User: marc
 * Date: 12/29/11
 * Time: 1:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class GameResultsActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        int scoreInt;
        int momentumInt;
        int biggestComboInt;

        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.gameresults);

        scoreInt=getIntent().getExtras().getInt("GAME_RESULTS_SCORE");
        momentumInt=getIntent().getExtras().getInt("GAME_RESULTS_MOMENTUM");
        biggestComboInt=getIntent().getExtras().getInt("GAME_RESULTS_BIGGESTCOMBO");

        TextView score=(TextView)findViewById(R.id.resultsScore);
        TextView momentum=(TextView)findViewById(R.id.resultsMomentum);
        TextView biggestCombo=(TextView)findViewById(R.id.resultsBiggestCombo);
        score.setText("Score: "+scoreInt);
        momentum.setText("Highest Momentum: "+momentumInt);
        biggestCombo.setText("Biggest Combo: "+biggestComboInt);
    }

    public void onMenuButtonClick(View v)
    {
        finish();
    }
}
