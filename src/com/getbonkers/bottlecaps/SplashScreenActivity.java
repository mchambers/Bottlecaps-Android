package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.JsonReader;
import android.view.View;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: marc
 * Date: 12/27/11
 * Time: 6:44 PM
 * To change this template use File | Settings | File Templates.
 */

public class SplashScreenActivity extends Activity {

    private SoundPool soundPool;
    private HashMap<Integer, Integer> soundPoolMap;

    public static final int SOUND_CLICK1 = 1;
    public static final int SOUND_CLICK2 = 2;
    public static final int SOUND_GAMESTART = 3;

    private void initSounds() {
        soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 100);
        soundPoolMap = new HashMap<Integer, Integer>();
        soundPoolMap.put(SOUND_CLICK1, soundPool.load(this, R.raw.button1click1, 1));
        soundPoolMap.put(SOUND_CLICK2, soundPool.load(this, R.raw.button1click2, 1));
        soundPoolMap.put(SOUND_GAMESTART, soundPool.load(this, R.raw.gamestart, 1));
    }

    public void playSound(int sound) {
        /* Updated: The next 4 lines calculate the current volume in a scale of 0.0 to 1.0 */
        AudioManager mgr = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
        float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float volume = streamVolumeCurrent / streamVolumeMax;

        /* Play the sound with the correct volume */
        soundPool.play(soundPoolMap.get(sound), volume, volume, 1, 0, 1f);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashscreen);

        initSounds();
        
        GetBonkersAPI.initPersistentCookieStore(getApplicationContext());

        RequestParams params=new RequestParams();
        params.put("user[email]", "android@getbonkers.com");
        params.put("user[password]", "Par1sH1lt0n");
        params.put("format", "json");

        GetBonkersAPI.post("/admin/login", params, this, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                Log.d("SplashScreen", response);

                RequestParams playerParams=new RequestParams();
                if(!GetBonkersAPI.havePlayerUUID(getApplicationContext()))
                {
                    //final String newUUID=java.util.UUID.randomUUID().toString();
                    TelephonyManager telMgr=(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
                    final String newUUID=telMgr.getDeviceId();

                    playerParams.put("player[guid]", newUUID);
                    playerParams.put("format", "json");
                    playerParams.put("player[device]", android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+" api "+android.os.Build.VERSION.SDK_INT);
                    GetBonkersAPI.post("/admin/players", playerParams, getApplicationContext(), new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(String response) {
                            Log.d("SplashScreen", response);
                            GetBonkersAPI.setPlayerUUID(newUUID, getApplicationContext());
                        }
                        
                        @Override
                        public void onFailure(Throwable t) {
                            Log.d("SplashScreen", "failure to make player: "+t.getMessage());
                        }
                    });
                }
                else
                    Log.d("SplashScreen", GetBonkersAPI.getPlayerUUID(getApplicationContext()));
            }
        });
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }
    
    public void onMenuButtonClick(View v)
    {
        playSound(SOUND_CLICK1);

        Intent menuIntent=new Intent(this, GameMenuActivity.class);
        this.startActivity(menuIntent);
    }

    public void onKidsModeClick(View v)
    {
        //if(!readyToGo) return;

        playSound(SOUND_CLICK1);
        //playSound(SOUND_GAMESTART);
        Intent kidsModeIntent=new Intent(this, GameBoardActivity.class);
        kidsModeIntent.putExtra("GAME_DIFFICULTY", 0);
        this.startActivity(kidsModeIntent);
    }

    public void onPlayButtonClick(View v)
    {
        //if(!readyToGo) return;

        playSound(SOUND_CLICK1);
        //playSound(SOUND_GAMESTART);

        Intent playIntent = new Intent(this, GameBoardActivity.class);
        playIntent.putExtra("GAME_DIFFICULTY", 1);
        this.startActivity(playIntent);
    }
}
