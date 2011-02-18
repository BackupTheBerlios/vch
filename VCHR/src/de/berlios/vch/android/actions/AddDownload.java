package de.berlios.vch.android.actions;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;
import de.berlios.vch.android.BrowseActivity;

public class AddDownload extends Action {

    private String downloadsUri;
    private URI vchuri;
    
    public AddDownload(String downloadsUri, URI vchuri) {
        this.downloadsUri = downloadsUri;
        this.vchuri = vchuri;
    }
    
    @Override
    public void execute() throws ClientProtocolException, IOException {
        String request = downloadsUri + "?action=add&vchuri=" + URLEncoder.encode(vchuri.toString(), "UTF-8"); 
        HttpGet get = new HttpGet(request);
        HttpClient client = new DefaultHttpClient();
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = client.execute(get, responseHandler);
        Log.v(BrowseActivity.TAG, "Server response: " + responseBody);
    }
}
