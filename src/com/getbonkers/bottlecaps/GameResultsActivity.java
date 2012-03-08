package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
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
    int scoreInt;
    boolean scorePosted;

    int level;

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
        int capsCollectedInt;
        int biggestComboInt;

        player=new Player(getApplicationContext());

        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.gameresults);

        scoreInt=getIntent().getExtras().getInt("GAME_RESULTS_SCORE");
        capsCollectedInt=getIntent().getExtras().getInt("GAME_RESULTS_CAPSCOLLECTED");
        biggestComboInt=getIntent().getExtras().getInt("GAME_RESULTS_BIGGESTCOMBO");
        level=getIntent().getExtras().getInt("GAME_RESULTS_LEVEL");

        TextView score=(TextView)findViewById(R.id.resultsScore);
        TextView capsCollected=(TextView)findViewById(R.id.resultsCapsCollected);
        TextView biggestCombo=(TextView)findViewById(R.id.resultsBestCombo);
        TextView bestComboCaption=(TextView)findViewById(R.id.resultsBestComboCaption);
        TextView capsCollectedCaption=(TextView)findViewById(R.id.resultsCapsCollectedCaption);

        /*
        score.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Pacifico.ttf"));
        capsCollected.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Pacifico.ttf"));
        biggestCombo.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Pacifico.ttf"));
        bestComboCaption.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Pacifico.ttf"));
        capsCollectedCaption.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Pacifico.ttf"));
          */

        score.setText(String.valueOf(scoreInt));
        capsCollected.setText(String.valueOf(capsCollectedInt));
        biggestCombo.setText(String.valueOf(biggestComboInt));

        player.reconcileCollectedCaps(GameResultsActivity.this);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        player.validateFacebookConnection();
         if(player.isConnectedToFacebook())
         {
             shareOnFacebook(null);
         }
    }

    public void shareOnFacebook(View v)
    {
        if(!player.isConnectedToFacebook())
        {
            Intent fbConnect=new Intent(this, ScoreboardActivity.class);
            fbConnect.putExtra("fbMode", 1);
            startActivityForResult(fbConnect, 133);
            return;
        }

        player.postScore(scoreInt);
        scorePosted=true;
    }

    public void onMenuButtonClick(View v)
    {
        if(!scorePosted)
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    player.postScore(scoreInt);
                }
            }).start();
        }
        finish();
    }

    public void onBoostsButtonClick(View v)
    {
        Intent boosts=new Intent(this, BoostsActivity.class);
        startActivity(boosts);
    }
    
    public void onRestartButtonClick(View v)
    {
        Intent playIntent = new Intent(this, GameBoardActivity.class);
        playIntent.putExtra("GAME_DIFFICULTY", level);
        this.startActivity(playIntent);
        
        finish();
    }
}
