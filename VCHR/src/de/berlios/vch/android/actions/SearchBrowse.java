package de.berlios.vch.android.actions;

import java.net.URLEncoder;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import de.berlios.vch.android.BrowseActivity;
import de.berlios.vch.android.VideoDetailsActivity;

public class SearchBrowse extends Action {

    private Context ctx;
    private String requestUri;
    private String uri;
    private String id;

    public SearchBrowse(Context ctx, String requestUri, String uri, String id) {
        this.ctx = ctx;
        this.requestUri = requestUri;
        this.uri = uri;
        this.id = id;
    }

    @Override
    public void execute() throws Exception {
        String request = requestUri + "?action=parse&isVideoPage=true&id=" + id + "&uri=" + URLEncoder.encode(uri, "UTF-8");
        HttpGet get = new HttpGet(request);
        get.addHeader("X-Requested-With", "XMLHttpRequest");
        HttpClient client = new DefaultHttpClient();
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = client.execute(get, responseHandler);
        Log.v(BrowseActivity.TAG, "Server response: " + responseBody);

        Intent intent = new Intent(ctx, VideoDetailsActivity.class);
        JSONObject video = new JSONObject(responseBody).getJSONObject("video");

        if (video.has("title")) {
            intent.putExtra("title", video.getString("title"));
        }
        if (video.has("desc")) {
            intent.putExtra("desc", video.getString("desc"));
        }
        if (video.has("duration")) {
            intent.putExtra("duration", video.getLong("duration"));
        }
        if (video.has("thumb")) {
            intent.putExtra("thumb", video.getString("thumb"));
        }
        if (video.has("vchuri")) {
            intent.putExtra("vchuri", video.getString("vchuri"));
        }
        ctx.startActivity(intent);
    }
}
