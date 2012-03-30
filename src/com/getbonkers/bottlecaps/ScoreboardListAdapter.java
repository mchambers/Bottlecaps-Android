package com.getbonkers.bottlecaps;

/**
 * Created by IntelliJ IDEA.
 * User: owner2
 * Date: 2/16/12
 * Time: 1:07 PM
 * To change this template use File | Settings | File Templates.
 */

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.AdapterView;
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

public class ScoreboardListAdapter extends ArrayAdapter<JSONObject> {
    private final Context context;
    private List<JSONObject> values;

    public ScoreboardListAdapter(Context context, List<JSONObject> values) {
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
        View rowView = inflater.inflate(R.layout.list_scoreboarditem, parent, false);

        TextView playerName=(TextView)rowView.findViewById(R.id.scoreboardItemName);
        TextView playerScore=(TextView)rowView.findViewById(R.id.scoreboardItemScore);
        TextView playerRank=(TextView)rowView.findViewById(R.id.scoreboardItemRank);      
        SmartImageView playerAvatar=(SmartImageView)rowView.findViewById(R.id.scoreboardItemAvatar);
        
        //playerName.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Coolvetica.ttf"));
        //playerScore.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Coolvetica.ttf"));
        playerRank.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Coolvetica.ttf"));

        JSONObject item;

        item=this.values.get(position);

        try {
            playerRank.setText(String.valueOf(item.getInt("rank")));
            playerScore.setText(NumberFormat.getInstance().format(item.getLong("score")));
            
            if(item.getString("name")==null || item.getString("name").equals("null"))
                playerName.setText("Anonymous");
            else
                playerName.setText(item.getString("name"));

            playerAvatar.setImageUrl(item.getString("avatar"));
        } catch(JSONException e)
        {
            e.printStackTrace();
        }

        return rowView;

    }
}
