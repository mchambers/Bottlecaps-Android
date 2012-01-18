package com.getbonkers.bottlecaps;

import android.content.Context;
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

    public static void initPersistentCookieStore(Context context)
    {
        cookieStore=new PersistentCookieStore(context);
        client.setCookieStore(cookieStore);
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
