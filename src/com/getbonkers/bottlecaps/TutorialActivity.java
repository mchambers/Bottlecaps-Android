package com.getbonkers.bottlecaps;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.flurry.android.FlurryAgent;

public class TutorialActivity extends FragmentActivity {
    public static final int MODE_DEFAULT=0;
    public static final int MODE_CAPMANAGER=1;
    public static final int MODE_ALL=2;
    
    public class TutorialPagerAdapter extends FragmentPagerAdapter {
        public class TutorialPageFragment extends Fragment {
            private int imgResource;

            @Override
            public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                imgResource=getArguments().getInt("image_resource");
            }

            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                     Bundle savedInstanceState) {
                super.onCreateView(inflater, container, savedInstanceState);

                View v=inflater.inflate(R.layout.pager_tutorialslide, container, false);
                v.setBackgroundResource(imgResource);
                return v;
            }
        }

        private int mMode;
        private int[] mResources;

        public TutorialPagerAdapter(FragmentManager fm, int mode) {
            super(fm);

            mMode=mode;

            if(mode==MODE_DEFAULT)
            {
                mResources=new int[] { R.drawable.help1, R.drawable.help2, R.drawable.help3 };
            }
            else if(mode==MODE_CAPMANAGER)
            {
                mResources=new int[] { R.drawable.help4, R.drawable.help5 };
            }
            else if(mode==MODE_ALL)
            {
                mResources=new int[] { R.drawable.help1, R.drawable.help2, R.drawable.help3, R.drawable.help4, 
                                        R.drawable.help5 };
            }
        }

        @Override
        public int getCount() {
            return mResources.length;
        }

        @Override
        public Fragment getItem(int position) {
            Fragment helpPage=new TutorialPageFragment();

            Bundle args=new Bundle();
            args.putInt("image_resource", mResources[position]);
            helpPage.setArguments(args);

            return helpPage;
        }
    }

    private TutorialPagerAdapter mAdapter;
    private ViewPager mPager;
    
    int mode;
    int difficulty;
    
    public void onCloseButtonClick(View v)
    {
        Intent i;
        
        switch(mode)
        {
            case MODE_DEFAULT:
                i=new Intent(this, GameBoardActivity.class);
                i.putExtra("GAME_DIFFICULTY", difficulty);
                break;
            case MODE_CAPMANAGER:
                i=new Intent(this, CapSetsActivity.class);
                break;
            case MODE_ALL:
            default:
                i=null;
        }
        
        if(i!=null)
            startActivity(i);

        finish();
    }
    
    @Override
    public void onStop()
    {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FlurryAgent.onStartSession(this, "LG9MLAYBEKLAFWLBMDAJ");

        setContentView(R.layout.tutorial);

        mode=getIntent().getExtras().getInt("mode");
        difficulty=getIntent().getExtras().getInt("GAME_DIFFICULTY");

        mAdapter = new TutorialPagerAdapter(getSupportFragmentManager(), mode);

        mPager = (ViewPager)findViewById(R.id.tutorialPager);
        mPager.setAdapter(mAdapter);
    }


}
