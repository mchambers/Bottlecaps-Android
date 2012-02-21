package com.getbonkers.bottlecaps;

import android.content.Context;
import android.content.res.TypedArray;
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
        SmartImageView imageView=new SmartImageView(mContext);

        JSONObject item=values.get(position);

        try {
            imageView.setImageUrl("http://data.getbonkers.com/bottlecaps/150/"+item.getInt("cap_set_id")+"/"+item.getInt("id")+".png");
        } catch(JSONException e)
        {
            e.printStackTrace();
        }

        //imageView.setImageResource(R.drawable.boostfreeze);
        imageView.setLayoutParams(new Gallery.LayoutParams(112, 112));
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        //imageView.setBackgroundResource(mGalleryItemBackground);

        return imageView;
    }
}
