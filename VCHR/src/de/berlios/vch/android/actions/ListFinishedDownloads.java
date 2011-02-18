package de.berlios.vch.android.actions;

import static de.berlios.vch.android.BrowseActivity.TAG;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;
import de.berlios.vch.android.ActiveDownloadsAdapter;
import de.berlios.vch.android.FinishedDownloadsAdapter;
import de.berlios.vch.android.ActiveDownloadsAdapter.Download;

public class ListFinishedDownloads extends Action {

    private String downloadsUri;
    private ActiveDownloadsAdapter adapter;
    
    public ListFinishedDownloads(String downloadsUri, FinishedDownloadsAdapter adapter) {
        this.downloadsUri = downloadsUri;
        this.adapter = adapter;
    }
    
    @Override
    public void execute() throws Exception {
        String requestUri = downloadsUri + "?action=list&list=finished";
        final List<Download> result = new ArrayList<Download>();
        HttpGet get = new HttpGet(requestUri);
        get.addHeader("X-Requested-With", "XMLHttpRequest");
        HttpClient client = new DefaultHttpClient();
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = client.execute(get, responseHandler);
        Log.v(TAG, "Server response: " + responseBody);
        
        // parse the response
        JSONArray array = new JSONArray(responseBody);
        for (int i = 0; i < array.length(); i++) {
            JSONObject jo = array.getJSONObject(i);
            Download download = new Download();
            download.id = jo.getString("id");
            download.title = jo.getString("title");
            result.add(download);
        }
        
        // update the listview
        adapter.setDownloads(result);
    }
}
