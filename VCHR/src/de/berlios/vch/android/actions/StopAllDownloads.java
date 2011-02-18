package de.berlios.vch.android.actions;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;
import de.berlios.vch.android.BrowseActivity;

public class StopAllDownloads extends Action {

    private String downloadsUri;
    
    public StopAllDownloads(String downloadsUri) {
        this.downloadsUri = downloadsUri;
    }
    
    @Override
    public void execute() throws ClientProtocolException, IOException {
        String request = downloadsUri + "?action=stop_all"; 
        HttpGet get = new HttpGet(request);
        HttpClient client = new DefaultHttpClient();
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = client.execute(get, responseHandler);
        Log.v(BrowseActivity.TAG, "Server response: " + responseBody);
    }
}
