package com.getbonkers.bottlecaps;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import com.loopj.android.image.SmartImageView;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 2/20/12
 * Time: 6:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class CapMiniGalleryAdapter extends ArrayAdapter<JSONObject> {
    private Context mContext;
    private List<JSONObject> values;

    public CapMiniGalleryAdapter(Context context, List<JSONObject> values) {
        super(context, R.layout.list_capitem, values);
        this.mContext = context;
        this.values = values;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        BottlecapsDatabaseAdapter adapter=new BottlecapsDatabaseAdapter(mContext);
        
        adapter.open();

        SmartImageView imageView=new SmartImageView(mContext);

        JSONObject item=values.get(position);

        int capSize=75;

        try {
            DisplayMetrics metrics;
            metrics=mContext.getResources().getDisplayMetrics();
            String capURL="";
            
            switch(metrics.densityDpi){
                case DisplayMetrics.DENSITY_LOW:
                    capURL="http://data.getbonkers.com/bottlecaps/caps/75/"+item.getInt("cap_set_id")+"/"+item.getInt("id")+".png";
                    capSize=48;
                    break;
                case DisplayMetrics.DENSITY_MEDIUM:
                    capSize=75;
                    if(adapter.capIsCollected(item.getInt("id")))
                        capURL="http://data.getbonkers.com/bottlecaps/caps/75/"+item.getInt("id")+".png";
                    else
                        capURL="http://data.getbonkers.com/bottlecaps/caps/150_BW/"+item.getInt("id")+".png";
                    break;
                case DisplayMetrics.DENSITY_HIGH:
                    if(adapter.capIsCollected(item.getInt("id")))
                        capURL="http://data.getbonkers.com/bottlecaps/caps/150/"+item.getInt("id")+".png";
                    else
                        capURL="http://data.getbonkers.com/bottlecaps/caps/150_BW/"+item.getInt("id")+".png";
                    capSize=112;
                    break;
            }
            imageView.setImageUrl(capURL);
        } catch(JSONException e)
        {
            e.printStackTrace();
        }

        adapter.close();

        //imageView.setImageResource(R.drawable.boostfreeze);
        imageView.setLayoutParams(new Gallery.LayoutParams(capSize, capSize));
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        //imageView.setBackgroundResource(mGalleryItemBackground);

        return imageView;
    }
}
