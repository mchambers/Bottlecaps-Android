package com.getbonkers.bottlecaps;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class PauseDialog extends Activity {

    PauseScreenCapListAdapter adapter;
    ArrayList<Long> capsRemaining;

    @Override
    public void onCreate(Bundle savedInstance)
    {
        super.onCreate(savedInstance);

        BottlecapsDatabaseAdapter db=new BottlecapsDatabaseAdapter(this);
        db.open();

        capsRemaining=db.getUncollectedCommonCaps();

        adapter=new PauseScreenCapListAdapter(this, capsRemaining);

        setContentView(R.layout.pause_overlay);
        
        ((ListView)findViewById(R.id.pauseCapsList)).setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }
    
    public void resumeClick(View v)
    {
        finish();
    }
    
    public void quitClick(View v)
    {

    }
}
