package com.getbonkers.bottlecaps;

import android.content.Context;
import android.content.SharedPreferences;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;

/**
 * Created by IntelliJ IDEA.
 * User: marc
 * Date: 1/17/12
 * Time: 6:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetBonkersAPI {
    private static final String BASE_URL = "http://getbonkers.heroku.com";

    private static AsyncHttpClient client = new AsyncHttpClient();
    private static PersistentCookieStore cookieStore;
    
    private static String playerUUID;

    public static void initPersistentCookieStore(Context context)
    {
        cookieStore=new PersistentCookieStore(context);
        client.setCookieStore(cookieStore);
    }

    public static boolean havePlayerUUID(Context context)
    {
        loadPlayerUUID(context);
        return (playerUUID!=null);
    }
    
    public static String getPlayerUUID(Context context)
    {
        if(havePlayerUUID(context))
            return playerUUID;
        else
            return null;
    }

    public static void setPlayerUUID(String token, Context context)
    {
        playerUUID=null;
        playerUUID=token;
        SharedPreferences preferences=context.getSharedPreferences("getbonkersplayer", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=preferences.edit();
        editor.putString("playeruuid", token);
        editor.commit();
    }

    public static void loadPlayerUUID(Context context)
    {
        if(playerUUID==null)
        {
            SharedPreferences preferences=context.getSharedPreferences("getbonkersplayer", Context.MODE_PRIVATE);
            playerUUID=preferences.getString("playeruuid", null);
        }
    }

    public static void clearPlayerUUID()
    {
        playerUUID=null;
    }

    public static void get(String url, RequestParams params, Context context, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, Context context, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
