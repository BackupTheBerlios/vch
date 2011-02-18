package de.berlios.vch.android.actions;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;
import de.berlios.vch.android.BrowseActivity;
import de.berlios.vch.android.PlaylistEntry;

public class ReorderPlaylist extends Action {

    private String playlistUri;
    private List<PlaylistEntry> oldOrder;
    private List<PlaylistEntry> newOrder;
    
    public ReorderPlaylist(String playlistUri, List<PlaylistEntry> oldOrder, List<PlaylistEntry> newOrder) {
        this.playlistUri = playlistUri;
        this.newOrder = newOrder;
        this.oldOrder = oldOrder;
    }
    
    @Override
    public void execute() throws ClientProtocolException, IOException  {
        String request = playlistUri + "?action=reorder";
        for (PlaylistEntry entry : newOrder) {
            request += "&pe[]=" + entry.id;
        }
        HttpGet get = new HttpGet(request);
        HttpClient client = new DefaultHttpClient();
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = client.execute(get, responseHandler);
        Log.v(BrowseActivity.TAG, "Server response: " + responseBody);
        if(!"ok".equalsIgnoreCase(responseBody.trim())) {
            throw new HttpResponseException(500, "Unexpected server response");
        }
    }

    public List<PlaylistEntry> getNewOrder() {
        return newOrder;
    }
    
    public List<PlaylistEntry> getOldOrder() {
        return oldOrder;
    }
}
