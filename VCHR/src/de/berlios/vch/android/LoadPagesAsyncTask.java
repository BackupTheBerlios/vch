package de.berlios.vch.android;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;

public class LoadPagesAsyncTask extends AsyncTask<URI, Integer, List<IWebPage>> {

    private static transient final String TAG = "VCHR";
    
    private ProgressDialog dialog;
    
    private Context context;
    
    private View view;
    
    private Exception e;
    
    public LoadPagesAsyncTask(Context context, View view) {
        this.context = context;
        this.view = view;
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
    }
    
    @Override
    protected List<IWebPage> doInBackground(URI... vchuris) {
        Log.i(TAG, "PageLoader running " + vchuris[0]);
        URI vchuri = vchuris[0];
        List<IWebPage> result = new ArrayList<IWebPage>();
        try {
            HttpGet get = new HttpGet(vchuri);
            get.addHeader("X-Requested-With", "XMLHttpRequest");
            HttpClient client = new DefaultHttpClient();
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = client.execute(get, responseHandler);
            Log.v(TAG, "Server response: " + responseBody);
            if(responseBody.startsWith("[")) {
                parseOverviewPage(result, responseBody);
            } else {
                parseVideoPage(result, responseBody);
            }
        } catch (IOException e) {
            Log.e(TAG, "Couldn't load page " + vchuri, e);
            this.e = e;
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse json response", e);
            this.e = e;
        } catch (URISyntaxException e) {
            Log.e(TAG, "VCH URI is invalid", e);
            this.e = e;
        }
        return result;
    }

    private void parseVideoPage(List<IWebPage> result, String responseBody) throws JSONException, URISyntaxException {
        JSONObject video = new JSONObject(responseBody).getJSONObject("video");
        JSONObject attrs = video.getJSONObject("attributes");
        IVideoPage videoPage = new VideoPage();

        // title
        String title = video.getJSONObject("data").getString("title");
        videoPage.setTitle(title);

        // vchuri
        String id = video.getJSONObject("attributes").getString("id");
        videoPage.setVchUri(new URI(id));
        
        // video uri
        if(attrs.has("vchvideo")) {
            String vchvideo = video.getJSONObject("attributes").getString("vchvideo");
            videoPage.setVideoUri(new URI(vchvideo));
        }

        // publish date
        if(attrs.has("vchpubDate")) {
            String vchpubdate = video.getJSONObject("attributes").getString("vchpubDate");
            Calendar pubDate = Calendar.getInstance();
            pubDate.setTimeInMillis(Long.parseLong(vchpubdate));
            videoPage.setPublishDate(pubDate);
        }
        
        // webpage
        if(attrs.has("vchlink")) {
            String vchlink = video.getJSONObject("attributes").getString("vchlink");
            videoPage.setUri(new URI(vchlink));
        }
        
        // description
        if(attrs.has("vchdesc")) {
            String vchdesc = video.getJSONObject("attributes").getString("vchdesc");
            videoPage.setDescription(vchdesc);
        }
        
        // thumbnail
        if(attrs.has("vchthumb")) {
            String vchthumb = video.getJSONObject("attributes").getString("vchthumb");
            videoPage.setThumbnail(new URI(vchthumb));
        }
        
        // duration
        if(attrs.has("vchduration")) {
            String vchduration = video.getJSONObject("attributes").getString("vchduration");
            videoPage.setDuration(Long.parseLong(vchduration));
        }
        
        result.add(videoPage);
    }

    private void parseOverviewPage(List<IWebPage> result, String responseBody) throws JSONException, URISyntaxException {
        JSONArray array = new JSONArray(responseBody);
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = array.getJSONObject(i);
            String title = json.getJSONObject("data").getString("title");
            String id = json.getJSONObject("attributes").getString("id");
            Log.v(TAG, "Found page " + title + " " + id);
            IWebPage page;
            if(json.getJSONObject("attributes").has("vchisLeaf")) {
                page = new VideoPage();
            } else {
                page = new OverviewPage();
            }
            page.setTitle(title);
            page.setVchUri(new URI(id));
            result.add(page);
        }        
    }

    protected void onPostExecute(List<IWebPage> result) {
        Log.i(TAG, "PageLoader finished");
        
        if(!isCancelled()) {
            if(dialog.isShowing()) {
                dialog.dismiss();
            }
            
            if(e != null) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.error_loading, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                    }
                });
            }
            
            if(result != null) {
                Log.d(TAG, "Found " + result.size() + " pages");
                ((IWebpageActivity)context).updatePages(result);
            }
        }
    }
}
