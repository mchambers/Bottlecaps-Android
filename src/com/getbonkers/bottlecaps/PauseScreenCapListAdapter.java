package com.getbonkers.bottlecaps;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.loopj.android.image.SmartImageView;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 3/23/12
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class PauseScreenCapListAdapter extends ArrayAdapter<Long> {
    private final Context context;
    private List<Long> values;

    public PauseScreenCapListAdapter(Context context, List<Long> values) {
        super(context, R.layout.list_capitem, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public int getCount()
    {
        return values.size()/3;
    }
    
    public void setValues(List<Long> values) {
        this.values=values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_capsthreebang, parent, false);

        ImageView cap1=(ImageView)rowView.findViewById(R.id.capThreeBangListCap1);
        ImageView cap2=(ImageView)rowView.findViewById(R.id.capThreeBangListCap2);
        ImageView cap3=(ImageView)rowView.findViewById(R.id.capThreeBangListCap3);

        String cap1Fn=context.getFilesDir().getPath()+"/"+this.values.get(position*3)+".png";
        String cap2Fn=context.getFilesDir().getPath()+"/"+this.values.get((position*3)+1)+".png";
        String cap3Fn=context.getFilesDir().getPath()+"/"+this.values.get((position*3)+2)+".png";

        cap1.setImageBitmap(BitmapFactory.decodeFile(cap1Fn));
        cap2.setImageBitmap(BitmapFactory.decodeFile(cap2Fn));
        cap3.setImageBitmap(BitmapFactory.decodeFile(cap3Fn));

        return rowView;
    }
}
