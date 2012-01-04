package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.*;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class GameBoardActivity extends Activity
{
    class BrainThread extends Thread {
        private SurfaceHolder _surfaceHolder;
        private GameBoard _board;
        private boolean _run = false;

        private final static int    MAX_FPS = 50;
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
                                try {
                                    Thread.sleep(sleepTime);
                                } catch (InterruptedException e) {}
                            }

                            while (sleepTime < 0 && framesSkipped < MAX_FRAME_SKIPS) {
                                _board.updateGameState();
                                sleepTime += FRAME_PERIOD;
                                framesSkipped++;
                            }
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

        public static final int GAME_DIFFICULTY_EASY=0;
        public static final int GAME_DIFFICULTY_NORMAL=1;

        public static final int GAME_LENGTH=60000;
        public static final int GAME_COMBO_DELAY=2000;

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

            public void setDefaultState()
            {
                Random rand=new Random();
                int max=7;   // between 2 and 7 seconds
                int min=2;   //

                int time=rand.nextInt((max+1) - min) + min;
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

            public void setFadingState()
            {
                this.remainingLife=PIECE_FADEOUT_ANIM_SPEED;
                this.state=PIECE_STATE_FADING;
                this.terminalState=false;
            }

            public void setTerminalFadingState()
            {
                this.remainingLife=PIECE_FADEOUT_ANIM_SPEED;
                this.state=PIECE_STATE_FADING;
                this.terminalState=true;
            }
        }

        private BrainThread _thread;
        private ArrayList<GamePiece> gamePieces;
        private final ArrayList<GamePiece> currentCombo=new ArrayList<GamePiece>();
        private int boardSize;
        private int itemsPerRow;
        private int pieceWidth;
        private int pieceHeight;
        private int boardMargins;
        private long lastTick;

        private int[] comboAmounts;
        private int currentScore;
        private double currentMomentum=1;
        private double highestMomentum=0;
        private int highestComboScore;
        private int currentLevel;

        private long timeRemaining;
        private long nextCombo;
        private long currentComboInterval;

        public CapManager capManager;

        private static final int PIECE_FADEOUT_ANIM_SPEED=1000;

        public GameBoard(Context context, CapManager capMgr)
        {
            super(context);
            getHolder().addCallback(this);
            _thread=new BrainThread(getHolder(), this);
            setFocusable(true);

            capManager=capMgr;
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
            currentLevel=difficulty;

            timeRemaining=GAME_LENGTH;
            currentComboInterval=GAME_COMBO_DELAY;
            nextCombo=GAME_COMBO_DELAY;

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
                    break;
                case GAME_DIFFICULTY_NORMAL:
                    boardSize=20;
                    itemsPerRow=4;
                    break;
            }

            //width-=boardMargins*2;

            pieceWidth=width/itemsPerRow;

            for(int i=0; i<boardSize; i++)
            {
                GamePiece newPiece=new GamePiece();
                newPiece.cap=capManager.getNextCap();
                newPiece.setDefaultState();
                newPiece.cap.putCapInPlay(getApplicationContext());
                gamePieces.add(newPiece);
            }

            comboAmounts=new int[10];
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                //if (event.getY() > getHeight() - 50) {
                //    //_thread.setRunning(false);
                //    //((Activity)getContext()).finish();
                //} else {
                    float whichPiece=event.getX()/pieceWidth;
                    float whichRow=event.getY()/pieceWidth;

                    whichPiece=(float)Math.ceil(whichPiece);
                    whichRow=(float)Math.ceil(whichRow);

                    //Log.d("GameBoard", "Piece "+whichPiece+" in row "+whichRow+" at coords: x=" + event.getX() + ",y=" + event.getY());
                    int pieceIndex=(int)whichRow*itemsPerRow;
                    pieceIndex-=itemsPerRow-whichPiece;
                    pieceIndex--;

                    if(pieceIndex<gamePieces.size() && !gamePieces.get(pieceIndex).terminalState)
                    {
                        playSound(SOUND_TAP);
                        gamePieces.get(pieceIndex).setTappedState();

                        synchronized (currentCombo)
                        {
                            if(currentCombo.isEmpty() || currentCombo.get(0).cap.resourceId!=gamePieces.get(pieceIndex).cap.resourceId)
                            {
                                for(int i=0; i<currentCombo.size(); i++)
                                {
                                    //currentCombo.get(i).setTerminalFadingState();
                                    currentCombo.get(i).cap.removeCapFromPlay();
                                    currentCombo.get(i).cap=capManager.getNextCap();
                                    currentCombo.get(i).cap.putCapInPlay(getApplicationContext());
                                    currentCombo.get(i).setDefaultState();
                                }

                                int deltaScore=0;

                                if(currentCombo.size()>1)
                                {
                                    Toast toast=Toast.makeText(getApplicationContext(), String.valueOf(currentCombo.size())+" combo!", Toast.LENGTH_SHORT);
                                    toast.show();

                                    if(currentMomentum<=0) currentMomentum=1;

                                    deltaScore=(int)(Math.pow(currentCombo.size(), 2)+Math.pow(currentCombo.get(0).cap.rarityClass, 2) * currentMomentum);// * (currentLevel/2));
                                    currentMomentum+=1/Math.log10(deltaScore)*10;
                                    highestMomentum=Math.max(currentMomentum, highestMomentum);
                                    currentScore+=deltaScore;

                                    highestComboScore=Math.max(highestComboScore, deltaScore);

                                    playSound(SOUND_GOOD);
                                    Log.d("GameBoard", "Score up by "+deltaScore+" (rarity "+currentCombo.get(0).cap.rarityClass+"), New score: "+currentScore+" at momentum "+currentMomentum);
                                }
                                else
                                {
                                    playSound(SOUND_BAD);
                                    currentMomentum*=(1-currentMomentum/100);
                                }

                                comboAmounts[currentCombo.size()]++;

                                currentCombo.clear();
                                currentCombo.add(gamePieces.get(pieceIndex));
                                //Log.d("GameBoard", "Started a new combo");
                            }
                            else
                            {
                                currentCombo.add(gamePieces.get(pieceIndex));
                                //Log.d("GameBoard", "Added a piece to the current combo");
                            }
                        }
                    }
            }
            return super.onTouchEvent(event);
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // TODO Auto-generated method stub
        }

        public void surfaceCreated(SurfaceHolder holder) {
            this.initSounds();
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

        private void updateGameState()
        {
            long deltaTick;
            if(lastTick==0) lastTick=System.currentTimeMillis();

            deltaTick=System.currentTimeMillis()-lastTick;

            currentMomentum-=Math.log10(currentMomentum)/60;
            //Log.d("GameBoard", "Dropping momentum to "+currentMomentum);

            if(currentMomentum<=0) currentMomentum=1;
            if(currentMomentum>100) currentMomentum=100;

            currentComboInterval=(long)Math.max(1000, (currentComboInterval*(1-currentMomentum/100))*1000);

            timeRemaining-=deltaTick;
            nextCombo-=deltaTick;

            if(timeRemaining<=0)
            {
                Intent resultsIntent=new Intent(getBaseContext(), GameResultsActivity.class);

                resultsIntent.putExtra("GAME_RESULTS_SCORE", currentScore);
                resultsIntent.putExtra("GAME_RESULTS_MOMENTUM", (int)highestMomentum);
                resultsIntent.putExtra("GAME_RESULTS_BIGGESTCOMBO", (int)highestComboScore);
                startActivity(resultsIntent);

                boolean retry=true;
                _thread.setRunning(false);

                timeRemaining=999999;

                finish();
            }

            if(nextCombo<=0)
            {
                capManager.prepNextCombo();
                nextCombo=GAME_COMBO_DELAY;
            }

            for(int i=0; i<gamePieces.size(); i++)
            {
                gamePieces.get(i).remainingLife-=deltaTick;

                switch(gamePieces.get(i).state)
                {
                    case PIECE_STATE_NORMAL:
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
                            currentCombo.remove(gamePieces.get(i));

                            // replace the piece with a new piece
                            gamePieces.get(i).cap.removeCapFromPlay();
                            gamePieces.get(i).cap=capManager.getNextCap();
                            gamePieces.get(i).cap.putCapInPlay(getApplicationContext());
                            gamePieces.get(i).setDefaultState();
                            //Log.d("GameBoard", "Piece state change: PIECE_STATE_NORMAL");
                        }
                        else
                        {
                            double newOpacity1=gamePieces.get(i).remainingLife/PIECE_FADEOUT_ANIM_SPEED;
                            double newOpacity2=(150*newOpacity1)+75;
                            gamePieces.get(i).opacity=(int)newOpacity2;
                        }
                        break;
                    case PIECE_STATE_TAPPED:
                        if(gamePieces.get(i).remainingLife<=0)
                        {
                            gamePieces.get(i).state=PIECE_STATE_FADING;
                            gamePieces.get(i).remainingLife=1000*(1-currentMomentum/100);
                            //Log.d("GameBoard", "Piece state change: PIECE_STATE_FADING");

                        }
                        break;
                }
            }

            lastTick=System.currentTimeMillis();
        }

        public void drawGameState(Canvas canvas)
        {
            //Bitmap _scratch = BitmapFactory.decodeResource(getResources(), R.drawable.set1_1);
            canvas.drawColor(Color.BLACK);

            BitmapDrawable cap;//=new BitmapDrawable(getResources(), _scratch);

            Paint tp=new Paint();

            tp.setColor(Color.WHITE);
            tp.setTextAlign(Paint.Align.LEFT);

            int x=0;
            int y=0;
            int curRow=0;
            int itemsThisRow=0;

            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int height = display.getHeight();

            //canvas.drawText("Score: "+currentScore, 5, height-30, tp);

            for(int i=0; i<gamePieces.size(); i++)
            {
                if(gamePieces.get(i).cap.isCurrentlyDrawable())
                {
                    cap=gamePieces.get(i).cap.image;
                    x=(pieceWidth)*(i%itemsPerRow);//+(pieceWidth/2);
                    y=(pieceWidth)*curRow;

                    /*if(gamePieces.get(i).state==PIECE_STATE_NORMAL)
                        cap.setColorFilter(null);
                    else if(gamePieces.get(i).state==PIECE_STATE_FADING)
                        cap.setColorFilter(null);
                    else*/ if(gamePieces.get(i).state==PIECE_STATE_TAPPED)
                        canvas.drawRect(x, y, x+pieceWidth, y+pieceWidth, tp);
                        //cap.setColorFilter(null);
                        //cap.setColorFilter(Color.BLUE, PorterDuff.Mode.MULTIPLY);

                    cap.setAlpha(gamePieces.get(i).opacity);

                    cap.setBounds(x, y, x+pieceWidth, y+pieceWidth);
                    cap.draw(canvas);

                    itemsThisRow++;
                    if(itemsThisRow==itemsPerRow)
                    {
                        curRow++;
                        itemsThisRow=0;
                    }
                }
            }
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        CapManager capMgr=new CapManager(getApplicationContext());
        GameBoard board=new GameBoard(this, capMgr);

        int difficulty=getIntent().getExtras().getInt("GAME_DIFFICULTY", 1);
        board.startNewGame(difficulty);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(board);
    }
}
