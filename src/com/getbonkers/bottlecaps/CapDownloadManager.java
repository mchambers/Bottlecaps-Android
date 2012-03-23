package com.getbonkers.bottlecaps;

import android.content.Context;
import android.util.Log;
import com.getbonkers.bottlecaps.CapManagerLoadingDelegate;
import com.getbonkers.bottlecaps.GetBonkersAPI;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CapDownloadManager implements Runnable {
    public class CapDownloadManagerQueueItem {
        public long setID;
        public boolean getAssets;
        public boolean getData;
    }

    public int requestsOutstanding=0;
    private Context _context;

    private BottlecapsDatabaseAdapter adapter;

    public final Stack<CapDownloadManagerQueueItem> queue=new Stack<CapDownloadManagerQueueItem>();
    public CapManagerLoadingDelegate delegate;

    public  CapDownloadManager(Context ctx, CapManagerLoadingDelegate dg) {
        delegate=dg;
        _context=ctx;

        adapter=new BottlecapsDatabaseAdapter(ctx);
    }

    private boolean loadCapSetFromZip(long setID, boolean storedInAssets)
    {
        Log.d("CapLoader", "Loading cap set " + setID + " from ZIP");
        try {
            ZipInputStream zipF;

            if(storedInAssets)
                zipF=new ZipInputStream(_context.getAssets().open(setID + ".zip"));
            else
            {
                //zipF=new ZipInputStream(new FileInputStream(Environment.getDownloadCacheDirectory().getPath()+"/"+setID+".zip"));
                zipF=new ZipInputStream(new URL("http://data.getbonkers.com/bottlecaps/zips/"+setID+".zip").openConnection().getInputStream());
            }

            ZipEntry entry;

            BufferedOutputStream outputStream=null;
            BufferedInputStream inputStream=null;

            while( (entry=zipF.getNextEntry()) !=null )
            {
                Log.d("CapManager", entry.getName());

                inputStream = new BufferedInputStream(zipF);
                try {
                    outputStream = new BufferedOutputStream(new FileOutputStream(_context.getFilesDir().getPath()+"/"+entry.getName()));
                } catch (FileNotFoundException e) {
                    // yeah;
                }

                if(outputStream!=null)
                {
                    try {
                        IOUtils.copy(inputStream, outputStream);

                    } catch (IOException e)
                    {
                        // blah?
                    }
                    finally {
                        outputStream.close();
                        //inputStream.close();
                    }
                }
            }
        }  catch(IOException e)
        {
            Log.d("CapLoader", "Caught exception "+e.getMessage()+" while loading cap set "+setID+" from ZIP");
            e.printStackTrace();
            return false;
        }
        return true;

    }

    private void storeCapSetInDatabase(final long setID)
    {
        requestsOutstanding++;

        Log.d("CapLoader", "Storing cap set "+setID+" into database");

        GetBonkersAPI.get("/sets/" + setID, new RequestParams(), _context, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    Log.d("CapManager", response);

                    JSONObject set = new JSONObject(response).getJSONObject("cap_set");

                    adapter.insertSet(setID, set.getString("name"), set.getString("artist"), set.getString("description"));

                    JSONArray caps = set.getJSONArray("caps");

                    for (int i = 0; i < caps.length(); i++) {
                        JSONObject cap = caps.getJSONObject(i);
                        adapter.insertCapIntoSet(setID, cap.getInt("id"), cap.getInt("available"), cap.getInt("issued"), cap.getString("name"), cap.getString("description"), cap.getInt("scarcity"));
                        Log.d("CapManager", "Cap: " + caps.getJSONObject(i).getString("name"));
                    }
                } catch (JSONException e) {

                } catch (Exception e) {

                }

                requestsOutstanding--;
            }
        });
    }

    private boolean capSetExistsInAssets(long setID)
    {
        InputStream i=null;
        try {
            i=_context.getAssets().open(setID+".zip");

        } catch (Exception e) {
            return false;
        } finally {
            try {
                if(i!=null) i.close();
            } catch(Exception e) {

            }

        }

        return true;
    }

    private void downloadCapSetAssets(long setID)
    {
        Log.d("CapLoader", "Downloading cap set "+setID);

        requestsOutstanding++;

        if(capSetExistsInAssets(setID))
        {
            loadCapSetFromZip(setID, true);
            requestsOutstanding--;
        }
        else
        {
            // download it from the server.
            /*
        URL url=new URL("");

        HttpURLConnection urlConn=(HttpURLConnection)url.openConnection();

        try {
            ZipInputStream in=new ZipInputStream(urlConn.getInputStream());



        } catch(Exception e)
        {

        }
        finally {
            urlConn.disconnect();
        }
            */
            requestsOutstanding--;
        }

        /*
      StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
      long sdBytesAvailable = (long)stat.getBlockSize() *(long)stat.getBlockCount();
      long sdCardSpaceAvailable = sdBytesAvailable / 1048576;

      StatFs phoneStat = new StatFs(Environment.getDataDirectory().getPath());
      long phoneBytesAvailable = (long)phoneStat.getBlockSize() * (long)phoneStat.getAvailableBlocks();
      long phoneSpaceAvailable = (long) (phoneBytesAvailable / (1024.f * 1024.f));
        */


    }

    public void queueSetAssetsForDownload(long setID)
    {
        synchronized (queue) {
            CapDownloadManagerQueueItem item=new CapDownloadManagerQueueItem();
            item.setID=setID;
            item.getAssets=true;
            item.getData=false;
            queue.push(item);
        }
    }

    public void queueSetDataForDownload(long setID)
    {
        synchronized (queue) {
            CapDownloadManagerQueueItem item=new CapDownloadManagerQueueItem();
            item.setID=setID;
            item.getAssets=false;
            item.getData=true;
            queue.push(item);
        }
    }

    public void run() {
        synchronized (queue) {
            if(queue.size()>0)
            {
                while(queue.size()>0)
                {
                    CapDownloadManagerQueueItem s=queue.pop();
                    if(s.getAssets)
                    {
                        downloadCapSetAssets(s.setID);
                    }

                    if(s.getData)
                    {
                        storeCapSetInDatabase(s.setID);
                    }
                }

                while(requestsOutstanding>0)
                {
                    try {
                        Thread.sleep(500);
                    } catch(InterruptedException e) {

                    }
                }
            }
            delegate.onCapSetLoadComplete();
        }
    }
}