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

        setContentView(R.layout.pause_overlay);

        BottlecapsDatabaseAdapter db=new BottlecapsDatabaseAdapter(this);
        db.open();

        capsRemaining=db.getUncollectedCommonCaps();
        if(capsRemaining.size()<=0)
        {
            findViewById(R.id.pauseCollectCallout).setVisibility(View.GONE);
        }
        else
        {
            adapter=new PauseScreenCapListAdapter(this, capsRemaining);
            ((ListView)findViewById(R.id.pauseCapsList)).setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }

        db.close();
    }
    
    public void resumeClick(View v)
    {
        setResult(RESULT_OK);
        finish();
    }
    
    public void quitClick(View v)
    {
        setResult(RESULT_CANCELED);
        finish();
    }
}
