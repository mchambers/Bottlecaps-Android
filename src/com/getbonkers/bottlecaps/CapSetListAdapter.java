package com.getbonkers.bottlecaps;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 2/16/12
 * Time: 1:07 PM
 * To change this template use File | Settings | File Templates.
 */

import java.util.List;

import android.graphics.Typeface;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.loopj.android.image.SmartImageView;

public class CapSetListAdapter extends ArrayAdapter<JSONObject> {
    private final Context context;
    private List<JSONObject> values;

    public CapSetListAdapter(Context context, List<JSONObject> values) {
        super(context, R.layout.list_capitem, values);
        this.context = context;
        this.values = values;
    }

    public void setValues(List<JSONObject> values) {
        this.values=values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_capsetitem, parent, false);

        TextView setName=(TextView)rowView.findViewById(R.id.listCapSetItemHeader);
        //SmartImageView tagImage=(SmartImageView)rowView.findViewById(R.id.capListItemImage);

        setName.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Pacifico.ttf"));

        JSONObject item;

        item=this.values.get(position);
        try {
            //tagImage.setImageUrl(item.getString("image_url"));
            //tagName.setText(item.getString(itemNameKey));
            setName.setText(item.getString("name"));

            Log.d("CapSetListAdapter", item.toString());
        } catch (/*JSONException e*/ Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // Change the icon for Windows and iPhone

        /*
		String s = values[position];
		if (s.startsWith("Windows7") || s.startsWith("iPhone")
				|| s.startsWith("Solaris")) {
			imageView.setImageResource(R.drawable.no);
		} else {
			imageView.setImageResource(R.drawable.ok);
		}                       */

        return rowView;
    }
}
