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

public class AddToPlaylist extends Action {

    private String playlistUri;
    private URI vchuri;
    
    public AddToPlaylist(String playlistUri, URI vchuri) {
        this.playlistUri = playlistUri;
        this.vchuri = vchuri;
    }
    
    @Override
    public void execute() throws ClientProtocolException, IOException {
        String request = playlistUri + "?action=add&uri=" + URLEncoder.encode(vchuri.toString(), "UTF-8"); 
        HttpGet get = new HttpGet(request);
        HttpClient client = new DefaultHttpClient();
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = client.execute(get, responseHandler);
        Log.v(BrowseActivity.TAG, "Server response: " + responseBody);
    }
}
