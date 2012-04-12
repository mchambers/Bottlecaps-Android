package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.flurry.android.FlurryAgent;

import java.util.HashMap;
import java.util.Random;

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

    private SoundPool soundPool;
    //private HashMap<Integer, Integer> soundPoolMap;

    private void playClosingSting()
    {
        int[] soundIds=new int[] { R.raw.afterbuttermybiscuits, R.raw.afterdills, R.raw.afterget, R.raw.aftergigantic, 
        R.raw.aftergravy, R.raw.afterpocket };

        soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);

        Random rand=new Random();

        AudioManager mgr = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        final float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
        final float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        final float volume = streamVolumeCurrent / streamVolumeMax;

        final Integer sound=soundPool.load(this, soundIds[rand.nextInt(soundIds.length-1)], 1);
        
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            public void onLoadComplete(android.media.SoundPool soundPool, int i, int i1)
            {
                soundPool.play(sound, volume, volume, 1, 0, 1f);
            }
        });
    }

    @Override
    public void onStop()
    {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        //player.reconcileCollectedCaps(GameResultsActivity.this);
        playClosingSting();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        int capsCollectedInt;
        int biggestComboInt;

        player=new Player(this);

        super.onCreate(savedInstanceState);
        FlurryAgent.onStartSession(this, "LG9MLAYBEKLAFWLBMDAJ");

        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.gameresults);

        scoreInt=getIntent().getExtras().getInt("GAME_RESULTS_SCORE");
        capsCollectedInt=getIntent().getExtras().getInt("GAME_RESULTS_CAPSCOLLECTED");
        biggestComboInt=getIntent().getExtras().getInt("GAME_RESULTS_BIGGESTCOMBO");
        level=getIntent().getExtras().getInt("GAME_RESULTS_LEVEL");

        player.postBiggestCombo(biggestComboInt);

        ((TextView)findViewById(R.id.resultsTitle)).setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Pacifico.ttf"));

        TextView score=(TextView)findViewById(R.id.resultsScore);
        TextView capsCollected=(TextView)findViewById(R.id.resultsCapsCollected);
        TextView biggestCombo=(TextView)findViewById(R.id.resultsBestCombo);
        TextView bestComboCaption=(TextView)findViewById(R.id.resultsBestComboCaption);
        TextView capsCollectedCaption=(TextView)findViewById(R.id.resultsCapsCollectedCaption);

        score.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Coolvetica.ttf"));
        /*capsCollected.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Pacifico.ttf"));
        biggestCombo.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Pacifico.ttf"));
        bestComboCaption.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Pacifico.ttf"));
        capsCollectedCaption.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/Pacifico.ttf"));
          */

        score.setText(String.valueOf(scoreInt));
        capsCollected.setText(String.valueOf(capsCollectedInt));
        biggestCombo.setText(String.valueOf(biggestComboInt));
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
        player.invokeScorePostDialog(scoreInt);
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
        setResult(RESULT_CANCELED);
        finish();
    }

    public void onBoostsButtonClick(View v)
    {
        Intent boosts=new Intent(this, BoostsActivity.class);
        startActivity(boosts);
    }
    
    public void onRestartButtonClick(View v)
    {
        setResult(RESULT_CANCELED);
        finish();

        Intent playIntent = new Intent(this, GameBoardActivity.class);
        playIntent.putExtra("GAME_DIFFICULTY", level);
        this.startActivity(playIntent);
    }
}
