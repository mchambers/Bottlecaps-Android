package com.getbonkers.bottlecaps;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 2/16/12
 * Time: 1:07 PM
 * To change this template use File | Settings | File Templates.
 */

import java.util.ArrayList;
import java.util.List;

import android.graphics.Typeface;
import android.util.Log;
import android.widget.Gallery;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONArray;
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
        final Gallery setIcons=(Gallery)rowView.findViewById(R.id.listCapSetItemGallery);

        setName.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Pacifico.ttf"));

        JSONObject item;

        item=this.values.get(position);
        try {
            //tagImage.setImageUrl(item.getString("image_url"));
            //tagName.setText(item.getString(itemNameKey));
            setName.setText(item.getString("name"));

            try {
                GetBonkersAPI.get("/sets/" + item.getInt("id"), new RequestParams(), context, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            Log.d("CapManager", response);

                            JSONObject set = new JSONObject(response).getJSONObject("cap_set");

                            //adapter.insertSet(setID, set.getString("name"), set.getString("artist"), set.getString("description"));

                            JSONArray caps=set.getJSONArray("cap");

                            List<JSONObject> capObjects=new ArrayList<JSONObject>();

                            for(int i=0; i<caps.length(); i++)
                            {
                                capObjects.add(caps.getJSONObject(i));
                                //adapter.insertCapIntoSet((long)setID, cap.getInt("id"), cap.getInt("available"), cap.getInt("issued"), cap.getString("name"), cap.getString("description"), cap.getInt("scarcity"));
                                Log.d("CapSetListAdapter", caps.getJSONObject(i).toString());
                            }

                            setIcons.setAdapter(new CapMiniGalleryAdapter(context, capObjects));
                            //Log.d("CapSetListAdapter", item.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }
                });
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
        catch (JSONException e)
        {

        }

        return rowView;

    }
}
