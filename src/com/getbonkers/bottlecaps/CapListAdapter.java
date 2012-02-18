package com.getbonkers.bottlecaps;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 2/16/12
 * Time: 5:47 PM
 * To change this template use File | Settings | File Templates.
 */
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.loopj.android.image.SmartImageView;

public class CapListAdapter extends ArrayAdapter<JSONObject> {
    private final Context context;
    private List<JSONObject> values;

    public String itemNameKey="Name";

    public CapListAdapter(Context context, List<JSONObject> values) {
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
        View rowView = inflater.inflate(R.layout.list_capitem, parent, false);

        TextView tagName=(TextView)rowView.findViewById(R.id.capListItemName);
        SmartImageView tagImage=(SmartImageView)rowView.findViewById(R.id.capListItemImage);

        JSONObject item;

        item=this.values.get(position);
        try {
            tagImage.setImageUrl(item.getString("image_url"));
            tagName.setText(item.getString(itemNameKey));
        } catch (JSONException e) {
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
