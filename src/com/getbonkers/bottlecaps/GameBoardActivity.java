package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.ArcShape;
import android.graphics.drawable.shapes.OvalShape;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.os.Bundle;
import android.view.*;
import android.view.MotionEvent.PointerCoords;

import android.util.Log;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.flurry.android.FlurryAgent;
import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.TimelinePropertyBuilder;
import org.pushingpixels.trident.callback.TimelineCallback;
import org.pushingpixels.trident.ease.Linear;
import org.pushingpixels.trident.ease.Spline;
import org.pushingpixels.trident.ease.TimelineEase;
import org.pushingpixels.trident.interpolator.CorePropertyInterpolators;
import org.pushingpixels.trident.interpolator.PropertyInterpolator;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

public class GameBoardActivity extends Activity implements CapManager.CapManagerDelegate
{
    ProgressDialog dialog;
    CapManager capMgr;
    GameBoard board;

    View pauseOverlay;

    private boolean _paused=false;
    
    public static final int DIALOG_CAPMANAGER_FAILURE=1;

    protected Dialog onCreateDialog(int id, Bundle bundle) {

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
                board.currentLevel=difficulty;

                  /*
                RelativeLayout frame=new RelativeLayout(getApplicationContext());
                frame.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                frame.addView(board);

                LayoutInflater inflater = (LayoutInflater) getApplicationContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                pauseOverlay = inflater.inflate(R.layout.pause_overlay, null, false);
                pauseOverlay.setVisibility(View.INVISIBLE);

                frame.addView(pauseOverlay);
                                */

                setContentView(board);
            }
        });
    }

    public void onCapSetsLoadFailure() {
        dialog.dismiss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(resultCode==RESULT_CANCELED)
        {
            try {
                board._thread.setRunning(false);
            } catch(Exception e)
            {

            }
            finish();
        }
    }
    
    public void togglePause(View v) {
        _paused=!_paused;
        board.togglePause();

        if(_paused)
        {
            Intent pause=new Intent(this, PauseDialog.class);
            startActivityForResult(pause, 0);
            //startActivity(pause);
        }
        /*else
        {
            pauseOverlay.setVisibility(View.INVISIBLE);
        } */
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
            
            _surfaceHolder.setFormat(PixelFormat.RGBA_8888);
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
        class GameOverlayAnimation
        {
            public long animationLength;

            private Display display;

            public Timeline timeline;

            public boolean done;
            
            protected volatile int top;
            protected volatile int left;
            protected volatile int bottom;
            protected volatile int right;

            protected int width;
            protected int height;

            public float alpha;
            
            //private BitmapDrawable drawable;
            private Bitmap bitmap;
            private String text;
            private Paint textPaint;
            private Paint textStrokePaint;

            protected TimelineCallback _cb;

            protected void disableDefaultCallback()
            {
                timeline.removeCallback(_cb);
            }

            public int getTop()
            {
                return top;
            }
            
            public void setTop(int v)
            {
                top=v;
            }
            
            public int getLeft()
            {
                return left;
            }
            
            public void setLeft(int v)
            {
                left=v;
            }
            
            public synchronized int getBottom()
            {
                return bottom;
            }
            
            public void setBottom(int v)
            {
                bottom=v;
            }

            public int getRight()
            {
                return right;
            }

            public void setRight(int v)
            {
                right=v;
            }

            public GameOverlayAnimation()
            {
                display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                timeline=new Timeline(this);

                _cb=new TimelineCallback() {
                    @Override
                    public void onTimelineStateChanged(Timeline.TimelineState timelineState, Timeline.TimelineState timelineState1, float v, float v1) {
                        if(timelineState == Timeline.TimelineState.DONE)
                            done=true;
                    }

                    @Override
                    public void onTimelinePulse(float v, float v1) {
                        //Log.d("GameOverlayAnimation", "updated top="+top);
                        //To change body of implemented methods use File | Settings | File Templates.
                    }
                };

                timeline.addCallback(_cb);
            }

            public void start()
            {
                timeline.setDuration(animationLength);
                timeline.play();
            }
            
            public void setText(String v)
            {
                text=v;
            }
            
            public String getText()
            {
                return text;
            }

            public void setTextPaint(Paint v)
            {
                textPaint=v;
            }

            public Paint getTextPaint()
            {
                return textPaint;
            }
            
            public void setTextStrokePaint(Paint v)
            {
                textStrokePaint=v;
            }
            
            public Paint getTextStrokePaint()
            {
                return textStrokePaint;
            }
            
            public void setBitmap(Bitmap bmp)
            {
                width=bmp.getWidth();
                height=bmp.getHeight();

                right=width;
                left=0;

                bottom=height;
                top=0;

                //drawable=new BitmapDrawable(getResources(), bmp);
                bitmap=bmp;
            }
            
            public void setTopLeftCorner(int t, int l)
            {
                top=t;
                left=l;

                bottom=t+height;
                right=l+width;
            }
            
            private void drawBitmap(Canvas canvas)
            {
                canvas.drawBitmap(bitmap, left, top, null);

                //drawable.setBounds(new Rect(getLeft(), getTop(), getRight(), getTop()+height));
                //Log.d("GameOverlayAnimation", "Drawing at "+drawable.getBounds().toString());
                //drawable.draw(canvas);
            }
            
            private void drawText(Canvas canvas)
            {
                canvas.drawText(text, left, top, textPaint);
                if(textStrokePaint!=null) // strike the text if a stroke has been provided
                    canvas.drawText(text, left, top, textStrokePaint);
            }
            
            public void draw(Canvas canvas)
            {
                if(bitmap!=null)
                    drawBitmap(canvas);
                if(text!=null)
                    drawText(canvas);
            }
        }

        class SlideUpFromBottomAnimation extends GameOverlayAnimation
        {
            PropertyInterpolator<Integer> intInterp=null;

            private boolean runSecondStage;

            public long holdAtMiddleDuration;

            public SlideUpFromBottomAnimation()
            {
                timeline.setEase(new Spline(0.9f));

                CorePropertyInterpolators core=new CorePropertyInterpolators();
                for(Iterator it=core.getPropertyInterpolators().iterator(); it.hasNext(); )
                {
                    PropertyInterpolator<?> interpolator=(PropertyInterpolator<?>)it.next();
                    if(interpolator.getBasePropertyClass().equals(Integer.class))
                    {
                        intInterp=(PropertyInterpolator<Integer>)interpolator;
                    }
                }

                this.disableDefaultCallback();

                timeline.addCallback(new TimelineCallback() {
                    @Override
                    public void onTimelineStateChanged(Timeline.TimelineState timelineState, Timeline.TimelineState timelineState1, float v, float v1) {
                        if(timelineState== Timeline.TimelineState.DONE)
                        {
                            if(!runSecondStage)
                            {
                                runSecondStage=true;
                                done=false;
                                timeline.resetDoneFlag();

                                timeline.setInitialDelay(holdAtMiddleDuration);
                                timeline.setDuration(animationLength/2);
                                timeline.setEase(new Spline(0.5f));

                                timeline.addPropertyToInterpolate(
                                        Timeline.<Integer> property("top").on(SlideUpFromBottomAnimation.this).from(top).to(-height).interpolatedWith(intInterp));

                                timeline.play();
                            }
                            else
                                done=true;
                        }
                    }

                    @Override
                    public void onTimelinePulse(float v, float v1) {
                    }
                });
            }

            public void start()
            {
                timeline.setDuration(animationLength/2);
                
                //if(holdAtMiddleDuration==0)
                //    holdAtMiddleDuration=200;

                timeline.addPropertyToInterpolate(
                        Timeline.<Integer> property("top").on(this).from(this.top).to(((this.top)/2)-(height/2)).interpolatedWith(intInterp));

                //timeline.addPropertyToInterpolate(
                  //      Timeline.<Integer> property("bottom").on(this).from(this.top).to((this.top-this.bottom)*2).interpolatedWith(intInterp));

                super.start();
            }
        }

        class GamePiece
        {
            public int state;
            public boolean terminalState;
            public double remainingLife;  // when this hits zero, we switch to STATE_FADING and fade it out
            public int opacity;
            public CapManager.Cap cap;
            
            public int x;
            public int y;

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

        public static final int PIECE_STATE_NORMAL=0;
        public static final int PIECE_STATE_FADING=1;
        public static final int PIECE_STATE_TAPPED=2;
        public static final int PIECE_STATE_HIGHLIGHTED=3;

        public static final int GAME_DIFFICULTY_EASY=0;
        public static final int GAME_DIFFICULTY_NORMAL=1;

        public static final int GAME_STATE_NORMAL=0;
        public static final int GAME_STATE_PAUSED=1;
        public static final int GAME_STATE_STARTING=2;
        public static final int GAME_STATE_OVER=3;

        public static final int GAME_LENGTH=60000;
        public static final int GAME_COMBO_DELAY=2000;

        public static final int GAME_TIMER_REMAINING=0;
        public static final int GAME_TIMER_COMBO=1;
        public static final int GAME_TIMER_BOOST=2;
        public static final int GAME_TIMER_BOMB=3;
        public static final int GAME_TIMER_PAUSE=4;

        private MediaPlayer mp;
        private SoundPool soundPool;
        private HashMap<Integer, Integer> soundPoolMap;
        private boolean soundIsEnabled;

        public static final int SOUND_GOOD = 1;
        public static final int SOUND_BAD = 2;
        public static final int SOUND_TAP = 3;
        public static final int SOUND_READY = 4;
        public static final int SOUND_GO = 5;
        public static final int SOUND_FRENZY = 6;
        public static final int SOUND_BONUSTIME = 7;
        public static final int SOUND_NITRO = 8;
        public static final int SOUND_JOKER = 9;
        public static final int SOUND_END = 10;

        private BrainThread _thread;
        public ArrayList<GamePiece> gamePieces;
        private final ArrayList<GamePiece> currentCombo=new ArrayList<GamePiece>();
        private final ArrayList<CapManager.Cap> capsCollected=new ArrayList<CapManager.Cap>();
        private CapManager.Cap currentComboType;
        private ArrayList<CapManager.Boost> boostsInEffect=new ArrayList<CapManager.Boost>();
        
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
        public int currentGameState;
        public int currentGameSubState;

        /*public long timeRemaining;
        public long nextCombo;           */
        public long[] gameTimers;
        public long[] timerIntervals;
        
        private int[] multiplierResIds;
        private BitmapDrawable[] multiplierGfx;
        private Bitmap capGlow;
        private Bitmap[] boostCueGfx;
        
        private ArrayList<GameOverlayAnimation> currentOverlays=new ArrayList<GameOverlayAnimation>();

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
        BitmapDrawable timerBg;
        Rect timerRect;
        Rect multGfxRect;

        Paint tp=new Paint();
        Paint text=new Paint();
        Paint textStroke=new Paint();
        Paint capPaint=new Paint();
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        Paint pointsPaint=new Paint();
        Paint pointsStroke=new Paint();

        Bitmap bg;
        BitmapDrawable cap;
        ShapeDrawable timerShape=new ShapeDrawable();

        GameBoardActivity _activity;

        public GameBoard(Context context, GameBoardActivity activity, CapManager capMgr)
        {
            super(context);
            getHolder().addCallback(this);
            _thread=new BrainThread(getHolder(), this);
            setFocusable(true);
            capManager=capMgr;

            _activity=activity;

            Player p=new Player(context);
            soundIsEnabled=p.hasAudioEnabled();

            multiplierGfx=new BitmapDrawable[] {
                    null,
                    new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.mult1)),
                    new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.mult2)),
                    new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.mult3)),
                    new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.mult4)),
                    new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.mult5)),
                    new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.mult6)),
                    new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.mult7)),
                    new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.mult8)),
                    new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.mult9)),
                    new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.mult10))
            };
            
            boostCueGfx=new Bitmap[] {
                    BitmapFactory.decodeResource(getResources(), R.drawable.captionnitro),
                    BitmapFactory.decodeResource(getResources(), R.drawable.captionjokers),
                    BitmapFactory.decodeResource(getResources(), R.drawable.captionfrenzy),
                    BitmapFactory.decodeResource(getResources(), R.drawable.caption10sec)
            };

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

        private void initSounds()
        {
            soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
            soundPoolMap = new HashMap<Integer, Integer>();
            //soundPoolMap.put(SOUND_GOOD, soundPool.load(getContext(), R.raw.cuecombomade, 2));
            soundPoolMap.put(SOUND_BAD, soundPool.load(getContext(), R.raw.cuenuts, 2));
            soundPoolMap.put(SOUND_TAP, soundPool.load(getContext(), R.raw.tap, 2));
            soundPoolMap.put(SOUND_READY, soundPool.load(getContext(), R.raw.startready, 2));
            soundPoolMap.put(SOUND_GO, soundPool.load(getContext(), R.raw.startgo1, 2));
            soundPoolMap.put(SOUND_FRENZY, soundPool.load(getContext(), R.raw.cuefrenzy, 2));
            soundPoolMap.put(SOUND_BONUSTIME, soundPool.load(getContext(), R.raw.cuebonustime, 2));
            soundPoolMap.put(SOUND_NITRO, soundPool.load(getContext(), R.raw.cuenitrovo, 2));
            soundPoolMap.put(SOUND_JOKER, soundPool.load(getContext(), R.raw.cuejoker, 2));
            soundPoolMap.put(SOUND_END, soundPool.load(getContext(), R.raw.end, 2));
        }

        private void initMusic()
        {
           mp=new MediaPlayer();

            AssetFileDescriptor yaayyyySong;

            try {
                AudioManager mgr = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
                float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
                float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                float volume = streamVolumeCurrent / streamVolumeMax;
                volume=(float)(volume*0.60);

                yaayyyySong=getAssets().openFd("musicgame.mp3");
                mp.setDataSource(yaayyyySong.getFileDescriptor(), yaayyyySong.getStartOffset(), yaayyyySong.getLength());
                mp.prepare();
                mp.setLooping(true);
                mp.setVolume(volume, volume);
            } catch(IOException e)
            {
                //Log.d("GameBoardActivity", "Sadface, couldn't initialize the in-game music");
                //e.printStackTrace();
                mp=null;
            }

        }
        
        public void startMusic()
        {
            if(!soundIsEnabled) return;

            if(mp!=null)
                mp.start();
        }
        
        public void stopMusic()
        {
            if(!soundIsEnabled) return;

            if(mp!=null)
                mp.stop();
        }
        
        public void pauseMusic()
        {
            if(!soundIsEnabled) return;

            if(mp!=null)
                mp.pause();
        }

        public void playSound(int sound)
        {
            if(!soundIsEnabled) return;

            /* Updated: The next 4 lines calculate the current volume in a scale of 0.0 to 1.0 */
            AudioManager mgr = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
            float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
            float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            float volume = streamVolumeCurrent / streamVolumeMax;

            if(sound==SOUND_TAP) volume=(float)(volume*0.75);

            /* Play the sound with the correct volume */
            soundPool.play(soundPoolMap.get(sound), volume, volume, 1, 0, 1f);
        }

        public void startNewGame(int difficulty)
        {
            Random rand=new Random();

            currentLevel=difficulty;

            //timeRemaining=GAME_LENGTH;

            //currentComboInterval=GAME_COMBO_DELAY;

            gamePieces=new ArrayList<GamePiece>();
            //currentCombo=new ArrayList<GamePiece>();
            currentCombo.clear();

            int x;
            int y;
            int itemsThisRow=0;
            int curRow=0;

            // Prepare the game board.
            for(int i=0; i<boardSize; i++)
            {
                GamePiece newPiece=new GamePiece();
                newPiece.cap=capManager.getNextCap(false);
                newPiece.setDefaultState(currentLevel);
                capManager.putCapInPlay(getApplicationContext(), newPiece.cap);
                gamePieces.add(newPiece);

                newPiece.x=((pieceWidth)*(i%itemsPerRow))+boardMarginWidth;//+(pieceWidth/2);
                newPiece.y=((pieceWidth)*curRow)+boardMarginHeight;

                itemsThisRow++;
                if(itemsThisRow==itemsPerRow)
                {
                    curRow++;
                    itemsThisRow=0;
                }
            }

            comboAmounts=new int[10];

            changeGameState(GAME_STATE_STARTING);

            _thread.setRunning(true);
            _thread.start();
        }

        public void togglePause()
        {
            gameIsPaused=!gameIsPaused;

            try {
                if(gameIsPaused)
                {
                    currentGameState=GAME_STATE_PAUSED;
                    pauseMusic();
                }
                else
                {
                    currentGameState=GAME_STATE_NORMAL;
                    startMusic();
                }
            } catch(Exception e)
            {
            }
        }

        private void doPointsCue(GamePiece piece, int amount)
        {
            SlideUpFromBottomAnimation points=new SlideUpFromBottomAnimation();
            points.setText(String.valueOf(amount));
            points.setTextPaint(pointsPaint);
            points.setTextStrokePaint(pointsStroke);

            points.setTopLeftCorner(piece.y+(pieceWidth/2), piece.x+(pieceWidth/2));
            points.animationLength=200;
            points.holdAtMiddleDuration=0;

            currentOverlays.add(points);
            points.start();
        }

        private void doBoostCue(int type)
        {
            SlideUpFromBottomAnimation boostAnim;

            boostAnim=new SlideUpFromBottomAnimation();

            switch(type)
            {
                case Player.PLAYER_BOOST_TYPE_FRENZY:
                    playSound(SOUND_FRENZY);
                    break;
                case Player.PLAYER_BOOST_TYPE_JOKER:
                    playSound(SOUND_JOKER);
                    break;
                case Player.PLAYER_BOOST_TYPE_MORETIME:
                    playSound(SOUND_BONUSTIME);
                    
                    if(currentGameSubState>0) // winding down
                        currentGameSubState=0;

                    break;
                case Player.PLAYER_BOOST_TYPE_NITRO:
                    playSound(SOUND_NITRO);
                    break;
            }

            boostAnim.setBitmap(boostCueGfx[type]);

            boostAnim.setTopLeftCorner(display.getHeight(), (display.getWidth() / 2) - (boostAnim.getRight() / 2));
            boostAnim.animationLength=200;
            boostAnim.holdAtMiddleDuration=150;
            currentOverlays.add(boostAnim);
            boostAnim.start();
        }

        private boolean handleTouch(float x, float y)
        {
            if(currentGameState==GAME_STATE_STARTING || currentGameState==GAME_STATE_OVER)
                return true;

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
                    //playSound(SOUND_TAP);
                    gamePieces.get(pieceIndex).setTappedState();

                    if((gamePieces.get(pieceIndex).cap instanceof CapManager.Boost) && gamePieces.get(pieceIndex).cap.index!=Player.PLAYER_BOOST_TYPE_JOKER)
                    {
                        boostsInEffect.add((CapManager.Boost)gamePieces.get(pieceIndex).cap);

                        // audio and visual RAZZLE DAZZLE!
                        doBoostCue(gamePieces.get(pieceIndex).cap.index);

                        capManager.removeBoostFromAvailability((CapManager.Boost)gamePieces.get(pieceIndex).cap);
                        gamePieces.get(pieceIndex).setTerminalFadingState();
                    }
                    else
                    {
                        synchronized (currentCombo)
                        {
                            if(gamePieces.get(pieceIndex).cap instanceof CapManager.Boost && gamePieces.get(pieceIndex).cap.index==Player.PLAYER_BOOST_TYPE_JOKER)
                                doBoostCue(Player.PLAYER_BOOST_TYPE_JOKER);

                            if(currentCombo.isEmpty() || !gamePieces.get(pieceIndex).cap.equals(currentComboType))
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

                                    deltaScore=(int)((Math.pow(currentCombo.size(), 2)+Math.pow(currentComboType.rarityClass, 2)) * currentMomentum * (tweakLevel/2));
                                    currentMomentum+=1/Math.log10(deltaScore)*8;

                                    highestMomentum=Math.max(currentMomentum, highestMomentum);
                                    currentScore+=deltaScore;

                                    highestComboScore=Math.max(highestComboScore, deltaScore);

                                    capsCollected.add(currentComboType);
                                    
                                    doPointsCue(gamePieces.get(pieceIndex), deltaScore);

                                    //playSound(SOUND_GOOD);
                                    //Log.d("GameBoard", "Score up by "+deltaScore+" (rarity "+currentCombo.get(0).cap.rarityClass+"), New score: "+currentScore+" at momentum "+currentMomentum);
                                }
                                else if(currentCombo.size()==1)
                                {
                                    playSound(SOUND_BAD);
                                    currentMomentum*=(1-currentMomentum/100);
                                }

                                comboAmounts[currentCombo.size()]++;

                                currentCombo.clear();
                                currentCombo.add(gamePieces.get(pieceIndex));
                                
                                if(!(gamePieces.get(pieceIndex).cap instanceof CapManager.Boost)) // don't let boosts become the current combo type
                                    currentComboType=gamePieces.get(pieceIndex).cap;
                            }
                            else
                            {
                                currentCombo.add(gamePieces.get(pieceIndex));

                                currentComboType=gamePieces.get(pieceIndex).cap;

                                for(int i=0; i<currentCombo.size(); i++)
                                    currentCombo.get(i).remainingLife+=1000;
                            }
                        }
                    }
                }  
            } catch(ArrayIndexOutOfBoundsException e)
            {
               // Log.d("GameBoard", "TOUCHED IN A NO NO PLACE");
            }

            return true;
        }

        public boolean onTouchEvent(MotionEvent event) {
              return super.onTouchEvent(event);
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // TODO Auto-generated method stub
        }
        
        private Rect bgRect;
        double scaleFactor;

        public void surfaceCreated(SurfaceHolder holder) {
            Random rand=new Random();

            this.initSounds();
            this.initMusic();

            // pick a random background image.
            int[] bgResources={R.drawable.gamebg1, R.drawable.gamebg2, R.drawable.gamebg3};
            
            Bitmap tempBg=BitmapFactory.decodeResource(getResources(), bgResources[rand.nextInt(2)]);
            bg=Bitmap.createScaledBitmap(tempBg, display.getWidth(), display.getHeight(), false);
            tempBg.recycle();

            //bg=BitmapFactory.decodeResource(getResources(), bgResources[rand.nextInt(2)]);
            bgRect=new Rect(0, 0, display.getWidth(), display.getHeight());
            
            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int width = display.getWidth();

            switch(currentLevel)
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

            DisplayMetrics metrics;
            metrics=this.getResources().getDisplayMetrics();
            scaleFactor=metrics.density;

            pieceWidth=width/itemsPerRow;
            pieceHeight=width/itemsPerRow;

            Bitmap tempGlow=BitmapFactory.decodeResource(getResources(), R.drawable.glow);
            capGlow=Bitmap.createScaledBitmap(tempGlow, (int)(pieceWidth+(12*scaleFactor)), (int)(pieceHeight+(12*scaleFactor)), false);
            tempGlow.recycle();

            //capGlow=BitmapFactory.decodeResource(getResources(), R.drawable.glow);

            // set up the static paints
            text.setColor(Color.BLACK);
            text.setStyle(Paint.Style.FILL);
            text.setTextAlign(Paint.Align.CENTER);
            text.setShadowLayer((float)0.5, 0, 1, Color.BLACK);
            text.setTextSize(64 * getApplicationContext().getResources().getDisplayMetrics().density);
            text.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Pacifico.ttf"));

            tp.setColor(Color.GRAY);
            tp.setTextAlign(Paint.Align.LEFT);

            textStroke.setColor(Color.WHITE);
            textStroke.setStyle(Paint.Style.STROKE);
            textStroke.setTextAlign(Paint.Align.CENTER);
            //textStroke.setShadowLayer((float)0.5, 0, 1, Color.BLACK);
            textStroke.setTextSize(64 * getApplicationContext().getResources().getDisplayMetrics().density);
            textStroke.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Pacifico.ttf"));
            textStroke.setStrokeWidth((float)(2.0f * scaleFactor));
            textStroke.setAntiAlias(true);

            pointsPaint.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Coolvetica.ttf"));
            pointsPaint.setTextSize(50.0f*getResources().getDisplayMetrics().density);
            pointsPaint.setColor(Color.WHITE);

            pointsStroke.setTypeface(pointsPaint.getTypeface());
            pointsStroke.setTextSize(pointsPaint.getTextSize());
            pointsStroke.setStrokeWidth((float)(2.0f * scaleFactor));
            pointsStroke.setColor(Color.BLACK);
            pointsStroke.setStyle(Paint.Style.STROKE);

            timerCover=new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.timerpausefix));
            timerBlue=new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.timerbluelayer));
            timerBg=new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.timerbg));

            timerRect=new Rect(display.getWidth()-(int)(60*scaleFactor), (int)(10*scaleFactor), display.getWidth()-(int)(10*scaleFactor), (int)(60*scaleFactor));
            multGfxRect=new Rect((int)(10*scaleFactor), (int)(10*scaleFactor), (int)((10+70)*scaleFactor), (int)((10+51)*scaleFactor));

            timerBg.setBounds(timerRect);
            timerShape.setBounds(timerRect.left+6, timerRect.top+5, timerRect.right-6, timerRect.bottom-5);
            timerShape.getPaint().setColor(Color.BLACK);
            timerShape.getPaint().setAntiAlias(true);

            boardMarginHeight*=scaleFactor;

            // set the bounds for all the multipler graphics
            for(int i=0; i<multiplierGfx.length; i++)
            {
                if(multiplierGfx[i]!=null)
                    multiplierGfx[i].setBounds(multGfxRect);
            }

            double totalVerticalPixelsUsed=(pieceHeight*((boardSize/itemsPerRow)))+boardMarginHeight;
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

            //timerCover.setBounds(timerRect);
            //int pauseBtnWidth=timerCover.getBitmap().getWidth();
            //int pauseBtnHeight=timerCover.getBitmap().getHeight();

            timerCover.setBounds(timerRect);
            timerBlue.setBounds(timerRect.left+6, timerRect.top+5, timerRect.right-6, timerRect.bottom-5);

            scorePosition=(float)(50*scaleFactor);

            capManager.pieceSize=pieceWidth;

            startNewGame(currentLevel);
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

        private void fireTimeBasedCues()
        {
            if(gameTimers[GAME_TIMER_REMAINING]<=2200 && currentGameSubState==0)
            {
                currentGameSubState++;
                playSound(SOUND_END);
            }
        }

        private void updateGameBoard()
        {
            Random rand=new Random();

            if(gameIsPaused) return;

            fireTimeBasedCues();

            if(gameTimers[GAME_TIMER_REMAINING]<=0)
            {
                changeGameState(GAME_STATE_OVER);
                stopMusic();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        BottlecapsDatabaseAdapter adapter=new BottlecapsDatabaseAdapter(getApplicationContext());

                        adapter.open();
                        for(int x=0; x<capsCollected.size(); x++)
                        {
                            adapter.addCapSettlement(capsCollected.get(x).index);
                        }
                        adapter.close();
                    }
                }).start();

                // queue the collected caps for reconciliation

                Intent resultsIntent=new Intent(getBaseContext(), GameResultsActivity.class);

                resultsIntent.putExtra("GAME_RESULTS_SCORE", currentScore);
                resultsIntent.putExtra("GAME_RESULTS_CAPSCOLLECTED", capsCollected.size());
                resultsIntent.putExtra("GAME_RESULTS_BIGGESTCOMBO", highestComboScore);
                resultsIntent.putExtra("GAME_RESULTS_LEVEL", currentLevel);

                startActivityForResult(resultsIntent, 0);
                //finish();

                boolean retry=true;
                _thread.setRunning(false);
//                _thread.stop();

                gameTimers[GAME_TIMER_REMAINING]=999999;
            }

            if(gameTimers[GAME_TIMER_BOOST]<=0)
            {
                capManager.prepNextBoost(this);
                gameTimers[GAME_TIMER_BOOST]=(Math.max(rand.nextInt(11)+3, 11)) * 1000;
            }

            if(gameTimers[GAME_TIMER_COMBO]<=0)
            {
                capManager.prepNextCombo(currentMomentum);
                gameTimers[GAME_TIMER_COMBO]=GAME_COMBO_DELAY;//timerIntervals[GAME_TIMER_COMBO];
            }

            /*
            Here's how the boosts thing works:

                    Most boosts are one-and-done.
                    Their effect fires, their timers are all zero, they get removed.

                    Some boosts have lifespans, of say 5 seconds.
                    They also have heartbeat timers of say half a second.
                    In that case, every half a second, the boost effects will be performed.

                    When a boost "in effect" leaves the game, it has a last gasp, the
                    "boost expiration effect," so it can do any housekeeping. A boost could
                    generate another of itself, clear the caps buffer, award more points,
                    etc.
             */

            // run the "boosts in effect" queue.
            if(!boostsInEffect.isEmpty())
            {
                for(Iterator it=boostsInEffect.iterator(); it.hasNext() ;)
                {
                    CapManager.Boost boost=(CapManager.Boost)it.next();
                    boost.timeRemaining-=deltaTick;          // decrement the lifecycle timer...
                    boost.intervalTimer-=deltaTick;          // ...and the heartbeat timer.
                    
                    if(boost.intervalTimer<=0)               // if this boost is due to run:
                    {
                        boost.performBoostEffects(this);     // perform the boost effect.
                        boost.intervalTimer=boost.interval;  // reset the interval timer.
                    }
    
                    if(boost.timeRemaining<=0)  // if this boost has reached end-of-life
                    {
                        boost.performExpirationEffect(this);   // one last gasp, if applicable.
                        it.remove();                           // remove it from the array.
                    }
                }
            }
            
            if(!currentOverlays.isEmpty())
            {
                for(Iterator it=currentOverlays.iterator(); it.hasNext() ;)
                {
                    GameOverlayAnimation anim=(GameOverlayAnimation)it.next();
                    if(anim.done)
                        it.remove();
                }
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

        private void changeGameState(int nextState)
        {
            Random rand=new Random();

            currentGameState=nextState;

            switch(nextState)
            {
                case GAME_STATE_STARTING:
                    gameTimers[GAME_TIMER_REMAINING]=2000;
                    gameTimers[GAME_TIMER_COMBO]=0;
                    gameTimers[GAME_TIMER_BOOST]=0;
                    timerIntervals[GAME_TIMER_COMBO]=0;
                    
                    SlideUpFromBottomAnimation ready=new SlideUpFromBottomAnimation();
                    ready.setBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ready));
                    ready.setTopLeftCorner(display.getHeight(), (display.getWidth() / 2) - (ready.getRight() / 2));
                    ready.animationLength=1000;
                    ready.holdAtMiddleDuration=500;
                    
                    currentOverlays.add(ready);

                    playSound(SOUND_READY);
                    ready.start();

                    break;
                case GAME_STATE_NORMAL:
                    currentGameSubState=0;

                    startMusic();

                    lastTick=0;

                    gameTimers[GAME_TIMER_REMAINING]=GAME_LENGTH;
                    gameTimers[GAME_TIMER_COMBO]=GAME_COMBO_DELAY;
                    gameTimers[GAME_TIMER_BOOST]=(Math.max(rand.nextInt(11)+3, 11)) * 1000;
                    timerIntervals[GAME_TIMER_COMBO]=GAME_COMBO_DELAY;

                    currentMomentum=1.0;

                    break;
                case GAME_STATE_PAUSED:
                    break;
                case GAME_STATE_OVER:
                    break;
            }
        }

        private void updateGameState()
        {
            this.runTimers();

            switch(currentGameState)
            {
                case GAME_STATE_NORMAL:
                    this.updateGameBoard();
                    break;
                case GAME_STATE_STARTING:
                    if(gameTimers[GAME_TIMER_REMAINING]<500 && currentGameSubState==0)
                    {
                        SlideUpFromBottomAnimation go=new SlideUpFromBottomAnimation();
                        go.setBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.go));
                        go.setTopLeftCorner(display.getHeight(), (display.getWidth() / 2) - (go.getRight() / 2));
                        go.animationLength=300;
                        go.holdAtMiddleDuration=200;

                        currentOverlays.add(go);

                        go.start();

                        playSound(SOUND_GO);

                        currentGameSubState++;
                    }
                    
                    if(gameTimers[GAME_TIMER_REMAINING]<=0)
                    {
                        changeGameState(GAME_STATE_NORMAL);
                    }
                    
                    break;
                case GAME_STATE_PAUSED:
                    break;
            }

            lastTick=System.currentTimeMillis();
        }

        public void drawGameState(Canvas canvas)
        {
            //Bitmap _scratch = BitmapFactory.decodeResource(getResources(), R.drawable.set1_1);
            canvas.drawColor(Color.WHITE);

            canvas.drawBitmap(bg, bgRect.left, bgRect.top, null);

            // draw the timer.
            double timerArc=(double)gameTimers[GAME_TIMER_REMAINING]/60000;
            timerArc=(1-timerArc)*360;
            ArcShape timer=new ArcShape(0, (float)timerArc);

            timerShape.setShape(timer);

            timerBg.draw(canvas);
            timerBlue.draw(canvas);
            timerShape.draw(canvas);
            timerCover.draw(canvas);

            // draw the game board.
            int x=0;
            int y=0;
            //int curRow=0;
            //int itemsThisRow=0;
            int i;
            
            for(i=0; i<gamePieces.size(); i++)
            {
                if(gamePieces.get(i).cap.isCurrentlyDrawable())
                {
                    x=gamePieces.get(i).x;
                    y=gamePieces.get(i).y;

                    if(gamePieces.get(i).state==PIECE_STATE_TAPPED)
                    {
                        canvas.drawBitmap(capGlow, (int)(x-(6*scaleFactor)), (int)(y-(6*scaleFactor)), null);
                    }
                    else if(gamePieces.get(i).state==PIECE_STATE_HIGHLIGHTED)
                    {
                        //cap.setColorFilter(Color.BLUE, PorterDuff.Mode.MULTIPLY);
                    }

                    capPaint.setAlpha(gamePieces.get(i).opacity);

                    canvas.drawBitmap(gamePieces.get(i).cap.image, x, y, capPaint);
                }
            }

            canvas.drawText(String.valueOf(currentScore), display.getWidth()/2, scorePosition, text);
            canvas.drawText(String.valueOf(currentScore), display.getWidth()/2, scorePosition, textStroke);
            
            int multiplier=0;
            
            if(currentMomentum>10)
                multiplier=(int)Math.ceil(currentMomentum/10);

            if(multiplier<=0) multiplier=1;
            if(multiplier>10) multiplier=10;

            if(multiplierGfx[multiplier]!=null)
            {
                multiplierGfx[multiplier].draw(canvas);
            }

            for(int z=0; z<currentOverlays.size(); z++)
            {
                currentOverlays.get(z).draw(canvas);
            }
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if(board!=null)
            board.pauseMusic();
    }
    
    @Override
    public void onResume()
    {
        super.onResume();

        if(board!=null)
        {
            if(board.gameIsPaused)
            {
                togglePause(null);
            }
            else
            {
                board.startMusic();
            }
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        FlurryAgent.onStartSession(this, "LG9MLAYBEKLAFWLBMDAJ");
        
        dialog = ProgressDialog.show(this, "",
                "Updating...", true);

        int difficulty=getIntent().getExtras().getInt("GAME_DIFFICULTY", 1);

        capMgr=new CapManager(getApplicationContext(), difficulty, this);
        capMgr.ensureCapSetAssetsExist();
        //capMgr.fillCapsBuffer();

    }
}
