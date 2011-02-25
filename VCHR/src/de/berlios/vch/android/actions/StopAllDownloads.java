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

public class StopAllDownloads extends VchrAsyncTask<Void, Void, String> {

    public StopAllDownloads(Context ctx) {
        super(ctx);
    }

    @Override
    protected String doTheWork(Void... params) throws Exception {
        String downloadsUri = new Config(ctx).getVchDownloadsUri();
        String request = downloadsUri + "?action=stop_all";
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
            Toast.makeText(ctx, ctx.getString(R.string.stop_failed, response), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void handleException(Exception e) {
        Toast.makeText(ctx, ctx.getString(R.string.stop_failed, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
    }
}