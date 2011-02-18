package de.berlios.vch.android.actions;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;
import android.widget.ListView;
import de.berlios.vch.android.BrowseActivity;
import de.berlios.vch.android.PlaylistEntry;
import de.berlios.vch.android.PlaylistListAdapter;

public class RemoveFromPlaylist extends Action {

    private String playlistUri;
    private PlaylistEntry entry;
    private PlaylistListAdapter adapter;
    private ListView view;
    
    public RemoveFromPlaylist(String playlistUri, ListView view, PlaylistEntry entry, PlaylistListAdapter adapter) {
        this.playlistUri = playlistUri;
        this.entry = entry;
        this.adapter = adapter;
        this.view = view;
    }

    @Override
    public void execute() throws ClientProtocolException, IOException {
        String request = playlistUri + "?action=remove&id=" + entry.id; 
        HttpGet get = new HttpGet(request);
        HttpClient client = new DefaultHttpClient();
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = client.execute(get, responseHandler);
        Log.v(BrowseActivity.TAG, "Server response: " + responseBody);
        if(!"ok".equalsIgnoreCase(responseBody.trim())) {
            throw new HttpResponseException(500, "Unexpected server response");
        } else {
            view.post(new Runnable() {
                @Override
                public void run() {
                    adapter.getEntries().remove(entry);
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }
}
