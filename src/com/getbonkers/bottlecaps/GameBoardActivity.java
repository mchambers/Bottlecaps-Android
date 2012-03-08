package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.ArcShape;
import android.graphics.drawable.shapes.OvalShape;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.os.Bundle;
import android.view.*;
import android.view.MotionEvent.PointerCoords;

import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Stack;

public class GameBoardActivity extends Activity implements CapManager.CapManagerDelegate
{
    ProgressDialog dialog;
    CapManager capMgr;
    GameBoard board;

    View pauseOverlay;

    private boolean _paused=false;
    
    public static final int DIALOG_CAPMANAGER_FAILURE=1;

    protected Dialog onCreateDialog(int id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch(id)
        {
            case DIALOG_CAPMANAGER_FAILURE:
                builder.setMessage("There was a problem communicating with the BottleCaps server. Make sure you're connected to Wi-Fi or 3G and try again.")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                            dialog.cancel();
                        }
                    });
                break;
        }

        return builder.create();
    }

    public void onCapManagerLoadFailure(int error)
    {
        dialog.dismiss();
        showDialog(DIALOG_CAPMANAGER_FAILURE);
    }

    public void capManagerProgressUpdate(int code)
    {

    }

    public void onCapManagerReady() {
        dialog.dismiss();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int difficulty=getIntent().getExtras().getInt("GAME_DIFFICULTY", 1);

                board=new GameBoard(getApplicationContext(), GameBoardActivity.this, capMgr);
                board.startNewGame(difficulty);

                FrameLayout frame=new FrameLayout(getApplicationContext());

                frame.addView(board);

                LayoutInflater inflater = (LayoutInflater) getApplicationContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                pauseOverlay = inflater.inflate(R.layout.pause_overlay, null, false);
                pauseOverlay.setVisibility(View.INVISIBLE);

                frame.addView(pauseOverlay);

                //requestWindowFeature(Window.FEATURE_NO_TITLE);
                setContentView(frame);
            }
        });
    }

    public void onCapSetsLoadFailure() {
        dialog.dismiss();
    }
    
    public void togglePause(View v) {
        _paused=!_paused;
        board.togglePause();

        if(_paused)
        {
            pauseOverlay.setVisibility(View.VISIBLE);
        }
        else
        {
            pauseOverlay.setVisibility(View.INVISIBLE);
        }
    }

    class BrainThread extends Thread {
        private SurfaceHolder _surfaceHolder;
        private GameBoard _board;
        private boolean _run = false;

        private final static int    MAX_FPS = 60;
        private final static int    MAX_FRAME_SKIPS = 5;
        private final static int    FRAME_PERIOD = 1000 / MAX_FPS;

        public BrainThread(SurfaceHolder surfaceHolder, GameBoard board) {
            _surfaceHolder = surfaceHolder;
            _board = board;
        }

        public void setRunning(boolean run) {
            _run = run;
        }

        @Override
        public void run() {
            Canvas canvas;

            long beginTime;     // the time when the cycle begun
            long timeDiff;      // the time it took for the cycle to execute
            int sleepTime;      // ms to sleep (<0 if we're behind)
            int framesSkipped;  // number of frames being skipped

            sleepTime = 0;

            while (_run) {
                    canvas = null;
                    try {
                        canvas = _surfaceHolder.lockCanvas();
                        synchronized (_surfaceHolder) {
                            beginTime = System.currentTimeMillis();
                            framesSkipped = 0;  // resetting the frames skipped
                            // update game state
                            _board.updateGameState();
                            _board.drawGameState(canvas);
                            timeDiff = System.currentTimeMillis() - beginTime;
                            sleepTime = (int)(FRAME_PERIOD - timeDiff);

                            if (sleepTime > 0) {
                                // if sleepTime > 0 we're OK
                                /* try {
                                    Thread.sleep(sleepTime);
                                } catch (InterruptedException e) {}    */
                            }

                            /*while (sleepTime < 0 && framesSkipped < MAX_FRAME_SKIPS) {
                                _board.updateGameState();
                                sleepTime += FRAME_PERIOD;
                                framesSkipped++;
                            } */
                        }
                    } finally {
                        if (canvas != null) {
                            _surfaceHolder.unlockCanvasAndPost(canvas);
                        }
                    }
            }
        }

            /*
            while (_run) {
                c = null;
                _board.updateGameState();
                try {
                    c = _surfaceHolder.lockCanvas(null);
                    synchronized (_surfaceHolder) {
                        _board.drawGameState(c);
                    }
                 } finally {
                    if (c != null) {
                        _surfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        } */
    }

    class GameBoard extends SurfaceView implements SurfaceHolder.Callback
    {
        public static final int PIECE_STATE_NORMAL=0;
        public static final int PIECE_STATE_FADING=1;
        public static final int PIECE_STATE_TAPPED=2;
        public static final int PIECE_STATE_HIGHLIGHTED=3;

        public static final int GAME_DIFFICULTY_EASY=0;
        public static final int GAME_DIFFICULTY_NORMAL=1;

        public static final int GAME_LENGTH=60000;
        public static final int GAME_COMBO_DELAY=2000;

        public static final int GAME_TIMER_REMAINING=0;
        public static final int GAME_TIMER_COMBO=1;
        public static final int GAME_TIMER_BOOST=2;
        public static final int GAME_TIMER_BOMB=3;
        public static final int GAME_TIMER_PAUSE=4;

        class GameOngoingEffect
        {
            public double remainingLife;
            public int state;

            public CapManager.Boost boost;

            public GameOngoingEffect()
            {

            }
        }

        class GamePiece
        {
            public int state;
            public boolean terminalState;
            public double remainingLife;  // when this hits zero, we switch to STATE_FADING and fade it out
            public int opacity;
            public CapManager.Cap cap;

            public GamePiece()
            {
                Random rand=new Random();
                this.remainingLife=rand.nextInt(6000);
                this.opacity=255;
                this.terminalState=false;
            }

            public void setDefaultState(int difficulty)
            {
                Random rand=new Random();
                int max=8;   // between 2 and 7 seconds
                int min=2;   //

                double time=rand.nextInt((max+1) - min) + min;

                if(difficulty==GAME_DIFFICULTY_EASY)
                    time=time*1.5;

                this.remainingLife=time*1000;
                this.state=PIECE_STATE_NORMAL;
                this.terminalState=false;
                this.opacity=255;
            }

            public void setTappedState()
            {
                this.state=PIECE_STATE_TAPPED;
                this.opacity=255;
                this.remainingLife=5000;
                this.terminalState=false;
            }

            public void setHighlightedState()
            {
                this.state=PIECE_STATE_HIGHLIGHTED;
                // don't change any other properties, just highlight the piece
            }

            public void setFadingState()
            {
                this.remainingLife=PIECE_FADEOUT_ANIM_SPEED;
                this.state=PIECE_STATE_FADING;
                this.terminalState=false;
            }

            public void setTerminalFadingState()
            {
                this.remainingLife=COMBO_FADEOUT_ANIM_SPEED;
                this.state=PIECE_STATE_FADING;
                this.terminalState=true;
            }
        }

        private BrainThread _thread;
        public ArrayList<GamePiece> gamePieces;
        private ArrayList<GamePiece> gameEffects;
        private final ArrayList<GamePiece> currentCombo=new ArrayList<GamePiece>();
        private final ArrayList<CapManager.Cap> capsCollected=new ArrayList<CapManager.Cap>();

        private int boardSize;
        private int itemsPerRow;
        private int pieceWidth;
        private int pieceHeight;
        private int boardMargins;

        private int boardMarginHeight;
        private int boardMarginWidth;
        private long lastTick;

        private int[] comboAmounts;
        public int currentScore;
        public double currentMomentum=1;
        public double highestMomentum=0;
        public int highestComboScore;
        public int currentLevel;

        /*public long timeRemaining;
        public long nextCombo;           */
        public long[] gameTimers;
        public long[] timerIntervals;
        
        private int[] multiplierResIds;
        private BitmapDrawable[] multiplierGfx;
        private BitmapDrawable capGlow;

        private float scorePosition;

        /*public long currentComboInterval;
        public long currentBoostInterval; */
        public int boostsDisplayed;
        public int maxBoosts;
        long deltaTick;

        private boolean gameIsPaused;

        public CapManager capManager;

        private static final int PIECE_FADEOUT_ANIM_SPEED=1000;
        private static final int COMBO_FADEOUT_ANIM_SPEED=250;

        BitmapDrawable timerCover;
        BitmapDrawable timerBlue;
        Rect timerRect;
        Rect multGfxRect;

        GameBoardActivity _activity;

        public GameBoard(Context context, GameBoardActivity activity, CapManager capMgr)
        {
            super(context);
            getHolder().addCallback(this);
            _thread=new BrainThread(getHolder(), this);
            setFocusable(true);
            capManager=capMgr;

            _activity=activity;

            multiplierGfx=new BitmapDrawable[] {
                    null,
                    new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.mult1)),
                    new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.mult2)),
                    new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.mult3)),
                    new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.mult4)),
                    new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.mult5)),
                    new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.mult6)),
                    new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.mult7)),
                    new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.mult8)),
                    new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.mult9)),
                    new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.mult10))
            };

            capGlow=new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.glow));

            gameTimers=new long[] { 0, 0, 0, 0, 0 };
            timerIntervals=new long[] {0, 0, 0, 0, 0 };

            setOnTouchListener(new OnTouchListener() {
                public boolean onTouch(View view, MotionEvent event) {
                    //dumpEvent(event);

                    switch(event.getActionMasked())
                    {
                        case MotionEvent.ACTION_DOWN:
                            return handleTouch(event.getX(), event.getY());
                        case MotionEvent.ACTION_POINTER_DOWN :
                            return handleTouch(event.getX(event.getActionIndex()), event.getY(event.getActionIndex()));
                    }
                    return true;
                }
            });
        }

        private SoundPool soundPool;
        private HashMap<Integer, Integer> soundPoolMap;

        public static final int SOUND_GOOD = 1;
        public static final int SOUND_BAD = 2;
        public static final int SOUND_TAP = 3;

        private void initSounds() {
             soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
             soundPoolMap = new HashMap<Integer, Integer>();
             soundPoolMap.put(SOUND_GOOD, soundPool.load(getContext(), R.raw.good, 1));
            soundPoolMap.put(SOUND_BAD, soundPool.load(getContext(), R.raw.bad, 1));
            soundPoolMap.put(SOUND_TAP, soundPool.load(getContext(), R.raw.click, 1));
        }

        public void playSound(int sound) {
            /* Updated: The next 4 lines calculate the current volume in a scale of 0.0 to 1.0 */
            AudioManager mgr = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
            float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
            float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            float volume = streamVolumeCurrent / streamVolumeMax;

            /* Play the sound with the correct volume */
            soundPool.play(soundPoolMap.get(sound), volume, volume, 1, 0, 1f);
        }

        public void startNewGame(int difficulty)
        {
            Random rand=new Random();

            // pick a random background image.
            int[] bgResources={R.drawable.gamebg1, R.drawable.gamebg2, R.drawable.gamebg3};
            bg=new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), bgResources[rand.nextInt(2)]));
            
            currentLevel=difficulty;

            //timeRemaining=GAME_LENGTH;
            gameTimers[GAME_TIMER_REMAINING]=GAME_LENGTH;
            gameTimers[GAME_TIMER_COMBO]=GAME_COMBO_DELAY;
            gameTimers[GAME_TIMER_BOOST]=(Math.max(rand.nextInt(11)+3, 11)) * 1000;
            timerIntervals[GAME_TIMER_COMBO]=GAME_COMBO_DELAY;

            //currentComboInterval=GAME_COMBO_DELAY;

            gamePieces=new ArrayList<GamePiece>();
            //currentCombo=new ArrayList<GamePiece>();
            currentCombo.clear();

            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int width = display.getWidth();

            switch(difficulty)
            {
                case GAME_DIFFICULTY_EASY:
                    boardSize=12;
                    itemsPerRow=3;
                    boardMarginHeight=70;
                    break;
                case GAME_DIFFICULTY_NORMAL:
                    boardSize=20;
                    itemsPerRow=4;
                    boardMarginHeight=70;
                    break;
            }

            //width-=boardMargins*2;

            pieceWidth=width/itemsPerRow;

            for(int i=0; i<boardSize; i++)
            {
                GamePiece newPiece=new GamePiece();
                newPiece.cap=capManager.getNextCap(false);
                newPiece.setDefaultState(currentLevel);
                capManager.putCapInPlay(getApplicationContext(), newPiece.cap);
                gamePieces.add(newPiece);
            }

            comboAmounts=new int[10];
        }

        public void togglePause()
        {
            gameIsPaused=!gameIsPaused;
        }

        private boolean handleTouch(float x, float y)
        {
            // check for button intersection
            if(x>timerRect.left && x<timerRect.right && y>timerRect.top && y<timerRect.bottom)
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //GameBoard.this.setOnTouchListener(null);
                        _activity.togglePause(null);
                    }
                });
                return false;
            }

            // check for piece intersection

            if(gameIsPaused) return true;

            float whichPiece=(x-boardMarginWidth)/pieceWidth;//event.getX()/pieceWidth;
            float whichRow=(y-boardMarginHeight)/pieceWidth;//event.getY()/pieceWidth;

            whichPiece=(float)Math.ceil(whichPiece);
            whichRow=(float)Math.ceil(whichRow);

            int pieceIndex=(int)whichRow*itemsPerRow;
            pieceIndex-=itemsPerRow-whichPiece;
            pieceIndex--;

            try {
                if(pieceIndex<gamePieces.size() && !gamePieces.get(pieceIndex).terminalState)
                {
                    playSound(SOUND_TAP);
                    gamePieces.get(pieceIndex).setTappedState();

                    if(gamePieces.get(pieceIndex).cap instanceof CapManager.Boost)
                    {
                        //Log.d("GameBoard", "Boost tapped");
                        ((CapManager.Boost)gamePieces.get(pieceIndex).cap).performBoostEffects(this);
                        capManager.removeBoostFromAvailability((CapManager.Boost)gamePieces.get(pieceIndex).cap);
                        gamePieces.get(pieceIndex).setTerminalFadingState();
                    }
                    else
                    {
                        synchronized (currentCombo)
                        {
                            if(currentCombo.isEmpty() || !currentCombo.get(0).cap.equals(gamePieces.get(pieceIndex).cap) /*currentCombo.get(0).cap.resourceId!=gamePieces.get(pieceIndex).cap.resourceId*/)
                            {
                                for(int i=0; i<currentCombo.size(); i++)
                                {
                                    currentCombo.get(i).setTerminalFadingState();
                                }

                                int deltaScore;

                                if(currentCombo.size()>1)
                                {
                                    // collect the current combo

                                    if(currentMomentum<=0) currentMomentum=1;

                                    double tweakLevel=currentLevel+1.0;

                                    deltaScore=(int)(Math.pow(currentCombo.size(), 2)+Math.pow(currentCombo.get(0).cap.rarityClass, 2) * currentMomentum * (tweakLevel/2));
                                    currentMomentum+=1/Math.log10(deltaScore)*8;
                                    highestMomentum=Math.max(currentMomentum, highestMomentum);
                                    currentScore+=deltaScore;

                                    highestComboScore=Math.max(highestComboScore, deltaScore);

                                    capsCollected.add(currentCombo.get(0).cap);

                                    //playSound(SOUND_GOOD);
                                    //Log.d("GameBoard", "Score up by "+deltaScore+" (rarity "+currentCombo.get(0).cap.rarityClass+"), New score: "+currentScore+" at momentum "+currentMomentum);
                                }
                                else
                                {
                                    //playSound(SOUND_BAD);
                                    currentMomentum*=(1-currentMomentum/100);
                                }

                                comboAmounts[currentCombo.size()]++;

                                currentCombo.clear();
                                currentCombo.add(gamePieces.get(pieceIndex));
                            }
                            else
                            {
                                currentCombo.add(gamePieces.get(pieceIndex));

                                for(int i=0; i<currentCombo.size(); i++)
                                    currentCombo.get(i).remainingLife+=1000;
                            }
                        }
                    }
                }  
            } catch(ArrayIndexOutOfBoundsException e)
            {
                Log.d("GameBoard", "TOUCHED IN A NO NO PLACE");
            }

            return true;
        }

        private void dumpEvent(MotionEvent event) {
   String names[] = { "DOWN" , "UP" , "MOVE" , "CANCEL" , "OUTSIDE" ,
      "POINTER_DOWN" , "POINTER_UP" , "7?" , "8?" , "9?" };
   StringBuilder sb = new StringBuilder();
   int action = event.getAction();
   int actionCode = action & MotionEvent.ACTION_MASK;
   sb.append("event ACTION_" ).append(names[actionCode]);
   if (actionCode == MotionEvent.ACTION_POINTER_DOWN
         || actionCode == MotionEvent.ACTION_POINTER_UP) {
      sb.append("(pid " ).append(
      action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
      sb.append(")" );
   }
   sb.append("[" );
   for (int i = 0; i < event.getPointerCount(); i++) {
      sb.append("#" ).append(i);
      sb.append("(pid " ).append(event.getPointerId(i));
      sb.append(")=" ).append((int) event.getX(i));
      sb.append("," ).append((int) event.getY(i));
      if (i + 1 < event.getPointerCount())
         sb.append(";" );
   }
   sb.append("]" );
   Log.d("GameBoard", sb.toString());
}

        public boolean onTouchEvent(MotionEvent event) {
              return super.onTouchEvent(event);
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // TODO Auto-generated method stub
        }

        public void surfaceCreated(SurfaceHolder holder) {
            this.initSounds();

            // set up the static paints
            text.setColor(Color.BLACK);
            text.setStyle(Paint.Style.FILL);
            text.setTextAlign(Paint.Align.CENTER);
            text.setShadowLayer((float)0.5, 0, 1, Color.BLACK);
            text.setTextSize(64 * getApplicationContext().getResources().getDisplayMetrics().density);
            text.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Pacifico.ttf"));

            textStroke.setColor(Color.WHITE);
            textStroke.setStyle(Paint.Style.STROKE);
            textStroke.setTextAlign(Paint.Align.CENTER);
            //textStroke.setShadowLayer((float)0.5, 0, 1, Color.BLACK);
            textStroke.setTextSize(64 * getApplicationContext().getResources().getDisplayMetrics().density);
            textStroke.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Pacifico.ttf"));
            textStroke.setStrokeWidth(1.0f);
            textStroke.setAntiAlias(true);

            timerCover=new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.timerpauselayer));
            timerBlue=new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.timerbluelayer));

            DisplayMetrics metrics;
            metrics=this.getResources().getDisplayMetrics();
            double scaleFactor=metrics.density;

            timerRect=new Rect(display.getWidth()-(int)(60*scaleFactor), (int)(10*scaleFactor), display.getWidth()-(int)(10*scaleFactor), (int)(60*scaleFactor));
            multGfxRect=new Rect((int)(10*scaleFactor), (int)(10*scaleFactor), (int)((10+55)*scaleFactor), (int)((10+49)*scaleFactor));
            boardMarginHeight*=scaleFactor;

            double totalVerticalPixelsUsed=(pieceWidth*((boardSize/itemsPerRow)))+boardMarginHeight;
            if(totalVerticalPixelsUsed<metrics.heightPixels)
            {
                // vertically center board underneath the header
                boardMarginHeight+=((metrics.heightPixels-totalVerticalPixelsUsed)/4);
            }
            else if(totalVerticalPixelsUsed>metrics.heightPixels)
            {
                // shrink pieces to fit
                double verticalOverage=totalVerticalPixelsUsed-metrics.heightPixels;
                int pieceAdjustment=(int)(verticalOverage/(boardSize/itemsPerRow));
                pieceWidth-=pieceAdjustment;
                boardMarginWidth+=pieceAdjustment;
            }

            timerCover.setBounds(timerRect);
            timerBlue.setBounds(timerRect);

            scorePosition=(float)(40*scaleFactor);

            _thread.setRunning(true);
            _thread.start();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            boolean retry = true;
            _thread.setRunning(false);
            while (retry) {
                try {
                    _thread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    // we will try it again and again...
                }
            }
        }

        private void runTimers()
        {
            if(lastTick==0) lastTick=System.currentTimeMillis();

            deltaTick=System.currentTimeMillis()-lastTick;

            //
            // if we need to change the speed at which things are happening
            // we can just play with deltaTick.
            //
            // for this reason we should never use deltaTick to update
            // "actual time" timers, only timers that are counting down
            //
            if(gameIsPaused)
            {
                //
                deltaTick=0;
            }
            else if(gameTimers[GAME_TIMER_PAUSE]>0)
            {
                gameTimers[GAME_TIMER_PAUSE]-=deltaTick;
                deltaTick=0;
            }
            else
            {
                currentMomentum-=Math.log10(currentMomentum)/60;
                //Log.d("GameBoard", "Dropping momentum to "+currentMomentum);

                if(currentMomentum<=0) currentMomentum=1;
                if(currentMomentum>100) currentMomentum=100;

                timerIntervals[GAME_TIMER_COMBO]=(long)Math.max(1000, (GAME_COMBO_DELAY*(1-currentMomentum/100)));

                gameTimers[GAME_TIMER_REMAINING]-=deltaTick;
                gameTimers[GAME_TIMER_COMBO]-=deltaTick;
                gameTimers[GAME_TIMER_BOOST]-=deltaTick;
            }
        }

        private void updateGameBoard()
        {
            Random rand=new Random();

            if(gameIsPaused) return;

            if(gameTimers[GAME_TIMER_REMAINING]<=0)
            {
                // queue the collected caps for reconciliation
                BottlecapsDatabaseAdapter adapter=new BottlecapsDatabaseAdapter(getApplicationContext());

                adapter.open();
                for(int x=0; x<capsCollected.size(); x++)
                {
                    adapter.addCapSettlement(capsCollected.get(x).index);
                }
                adapter.close();

                Intent resultsIntent=new Intent(getBaseContext(), GameResultsActivity.class);

                resultsIntent.putExtra("GAME_RESULTS_SCORE", currentScore);
                resultsIntent.putExtra("GAME_RESULTS_CAPSCOLLECTED", capsCollected.size());
                resultsIntent.putExtra("GAME_RESULTS_BIGGESTCOMBO", highestComboScore);
                resultsIntent.putExtra("GAME_RESULTS_LEVEL", currentLevel);

                startActivity(resultsIntent);

                boolean retry=true;
                _thread.setRunning(false);

                gameTimers[GAME_TIMER_REMAINING]=999999;

                finish();
            }

            if(gameTimers[GAME_TIMER_BOOST]<=0)
            {
                capManager.prepNextBoost();
                gameTimers[GAME_TIMER_BOOST]=(Math.max(rand.nextInt(11)+3, 11)) * 1000;
            }

            if(gameTimers[GAME_TIMER_COMBO]<=0)
            {
                capManager.prepNextCombo(currentMomentum);
                gameTimers[GAME_TIMER_COMBO]=GAME_COMBO_DELAY;//timerIntervals[GAME_TIMER_COMBO];
            }

            for(int i=0; i<gamePieces.size(); i++)
            {
                gamePieces.get(i).remainingLife-=deltaTick;

                switch(gamePieces.get(i).state)
                {
                    case PIECE_STATE_NORMAL:
                    case PIECE_STATE_HIGHLIGHTED:
                        if(gamePieces.get(i).remainingLife<=0)
                        {
                            gamePieces.get(i).state=PIECE_STATE_FADING;
                            gamePieces.get(i).remainingLife=PIECE_FADEOUT_ANIM_SPEED;
                            //Log.d("GameBoard", "Piece state change: PIECE_STATE_FADING");
                        }
                        break;
                    case PIECE_STATE_FADING:
                        if(gamePieces.get(i).remainingLife<=0)
                        {
                            synchronized (currentCombo)
                            {
                                currentCombo.remove(gamePieces.get(i));
                            }

                            // replace the piece with a new piece
                            gamePieces.get(i).cap.removeCapFromPlay();
                            gamePieces.get(i).cap=capManager.getNextCap(false);
                            capManager.putCapInPlay(getApplicationContext(), gamePieces.get(i).cap);
                            gamePieces.get(i).setDefaultState(currentLevel);
                            //Log.d("GameBoard", "Piece state change: PIECE_STATE_NORMAL");
                        }
                        else
                        {
                            double newOpacity1=gamePieces.get(i).remainingLife/PIECE_FADEOUT_ANIM_SPEED;
                            double newOpacity2=(175*newOpacity1)+80;
                            gamePieces.get(i).opacity=(int)newOpacity2;
                            //gamePieces.get(i).opacity-=1;
                        }
                        break;
                    case PIECE_STATE_TAPPED:
                        if(gamePieces.get(i).remainingLife<=0)
                        {
                            gamePieces.get(i).state=PIECE_STATE_FADING;
                            gamePieces.get(i).remainingLife=PIECE_FADEOUT_ANIM_SPEED;//1000*(1-currentMomentum/100);
                        }
                        break;
                }
            }
        }

        private void updateGameState()
        {
            this.runTimers();
            this.updateGameBoard();

            lastTick=System.currentTimeMillis();
        }
        
        Paint tp=new Paint();
        Paint text=new Paint();
        Paint textStroke=new Paint();
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        BitmapDrawable bg;
        BitmapDrawable multiplier;

        public void drawGameState(Canvas canvas)
        {
            //Bitmap _scratch = BitmapFactory.decodeResource(getResources(), R.drawable.set1_1);
            canvas.drawColor(Color.WHITE);

            bg.setBounds(new Rect(0, 0, display.getWidth(), display.getHeight()));
            bg.draw(canvas);

            BitmapDrawable cap;//=new BitmapDrawable(getResources(), _scratch);

            tp.setColor(Color.GRAY);
            tp.setTextAlign(Paint.Align.LEFT);

            // draw the timer.
            double timerArc=(double)gameTimers[GAME_TIMER_REMAINING]/60000;
            timerArc=(1-timerArc)*360;
            ArcShape timer=new ArcShape(0, (float)timerArc);

            ShapeDrawable timerShape=new ShapeDrawable(timer);
            timerShape.setBounds(timerRect.left+6, timerRect.top+5, timerRect.right-6, timerRect.bottom-5);
            timerShape.getPaint().setColor(Color.BLACK);
            timerShape.getPaint().setAntiAlias(true);

            timerBlue.draw(canvas);
            timerShape.draw(canvas);
            timerCover.draw(canvas);

            // draw the game board.
            int x=0;
            int y=0;
            int curRow=0;
            int itemsThisRow=0;
            int i;

            for(i=0; i<gamePieces.size(); i++)
            {
                if(gamePieces.get(i).cap.isCurrentlyDrawable())
                {
                    cap=gamePieces.get(i).cap.image;
                    x=((pieceWidth)*(i%itemsPerRow))+boardMarginWidth;//+(pieceWidth/2);
                    y=((pieceWidth)*curRow)+boardMarginHeight;

                    /*if(gamePieces.get(i).state==PIECE_STATE_NORMAL)
                        cap.setColorFilter(null);
                    else if(gamePieces.get(i).state==PIECE_STATE_FADING)
                        cap.setColorFilter(null);
                    else*/
                    if(gamePieces.get(i).state==PIECE_STATE_TAPPED)
                    {
                        capGlow.setBounds(x-7, y-7, x+pieceWidth+7, y+pieceWidth+7);
                        capGlow.draw(canvas);
                        //canvas.drawRect(x, y, x+pieceWidth, y+pieceWidth, tp);
                    }
                    else if(gamePieces.get(i).state==PIECE_STATE_HIGHLIGHTED)
                    {
                        cap.setColorFilter(Color.BLUE, PorterDuff.Mode.MULTIPLY);
                    }

                    cap.setAlpha(gamePieces.get(i).opacity);

                    cap.setBounds(x+3, y+3, x+pieceWidth-3, y+pieceWidth-3);
                    cap.draw(canvas);

                    itemsThisRow++;
                    if(itemsThisRow==itemsPerRow)
                    {
                        curRow++;
                        itemsThisRow=0;
                    }
                }
            }

            x=(pieceWidth)*(i%itemsPerRow);//+(pieceWidth/2);
            y=(pieceWidth)*curRow;

            canvas.drawText(String.valueOf(currentScore), display.getWidth()/2, scorePosition, text);
            canvas.drawText(String.valueOf(currentScore), display.getWidth()/2, scorePosition, textStroke);
            
            int multiplier=0;
            
            if(currentMomentum>10)
                multiplier=(int)Math.ceil(currentMomentum/10);

            if(multiplier<=0) multiplier=1;

            if(multiplierGfx[multiplier]!=null)
            {
                multiplierGfx[multiplier].setBounds(multGfxRect);
                multiplierGfx[multiplier].draw(canvas);
            }
            //canvas.drawText(String.valueOf(multiplier)+"X", 50, 50, text);
            //canvas.drawText(String.valueOf(multiplier)+"X", 50, 50, textStroke);
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        dialog = ProgressDialog.show(this, "",
                "Updating...", true);

        int difficulty=getIntent().getExtras().getInt("GAME_DIFFICULTY", 1);

        capMgr=new CapManager(getApplicationContext(), difficulty, this);
        //capMgr.fillCapsBuffer();

    }
}
