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
public class GameResultsActivity extends Activity implements AsyncNetworkDelegate {
    Player player;

    @Override
    public void onOperationFailed(long operationID) {}

    @Override
    public void onOperationComplete(long operationID) {}

    @Override
    public void onOperationProgress(int progress) {}

    @Override
    public void onCapReconcileComplete(long capID) {}

    @Override
    public void onBatchCapReconcileComplete(long[] capIDs) {}

    @Override
    public void onQueueRunComplete() {}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        int scoreInt;
        int momentumInt;
        int biggestComboInt;

        player=new Player(getApplicationContext());

        player.reconcileCollectedCaps(this);

        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.gameresults);

        scoreInt=getIntent().getExtras().getInt("GAME_RESULTS_SCORE");
        momentumInt=getIntent().getExtras().getInt("GAME_RESULTS_MOMENTUM");
        biggestComboInt=getIntent().getExtras().getInt("GAME_RESULTS_BIGGESTCOMBO");

        player.postScore(scoreInt);

        TextView score=(TextView)findViewById(R.id.resultsScore);
        TextView momentum=(TextView)findViewById(R.id.resultsMomentum);
        TextView biggestCombo=(TextView)findViewById(R.id.resultsBiggestCombo);
        score.setText(String.valueOf(scoreInt));
        momentum.setText("Highest Momentum: "+momentumInt);
        biggestCombo.setText(String.valueOf(biggestComboInt));
    }

    public void onMenuButtonClick(View v)
    {
        finish();
    }
}
