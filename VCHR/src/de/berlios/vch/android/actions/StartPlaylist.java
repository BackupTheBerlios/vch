package de.berlios.vch.android.actions;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import de.berlios.vch.android.BrowseActivity;
import de.berlios.vch.android.Config;
import de.berlios.vch.android.R;
import de.berlios.vch.android.VchrAsyncTask;

public class StartPlaylist extends VchrAsyncTask<Void, Void, String> {

    public StartPlaylist(Context ctx) {
        super(ctx);
    }

    @Override
    protected String doTheWork(Void... params) throws Exception {
        String playlistUri = new Config(ctx).getVchPlaylistUri();
        String request = playlistUri + "?action=play";
        Log.d(BrowseActivity.TAG, "Sending request " + request);
        HttpGet get = new HttpGet(request);
        get.addHeader("X-Requested-With", "XMLHttpRequest");
        HttpClient client = new DefaultHttpClient();
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = client.execute(get, responseHandler);
        Log.v(BrowseActivity.TAG, "Server response: " + responseBody);
        return responseBody;
    }

    @Override
    protected void finished(String response) {
        if (!"ok".equalsIgnoreCase(response.trim())) {
            Toast.makeText(ctx, response, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void handleException(Exception e) {
        String msg = ctx.getString(R.string.playback_failed, e.getLocalizedMessage());
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
        Log.e(BrowseActivity.TAG, msg, e);
    }
}
