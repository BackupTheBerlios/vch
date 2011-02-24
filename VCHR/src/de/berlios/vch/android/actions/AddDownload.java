package de.berlios.vch.android.actions;

import java.net.URLEncoder;

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

public class AddDownload extends VchrAsyncTask<String, Void, String> {

    public AddDownload(Context ctx) {
        super(ctx);
    }

    @Override
    protected String doTheWork(String... uris) throws Exception {
        String downloadsUri = new Config(ctx).getVchDownloadsUri();
        String request = downloadsUri + "?action=add&vchuri=" + URLEncoder.encode(uris[0].toString(), "UTF-8");
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
        // TODO check the response
    }

    @Override
    protected void handleException(Exception e) {
        String msg = ctx.getString(R.string.communication_error, e.getLocalizedMessage());
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
        Log.e(BrowseActivity.TAG, msg, e);
    }
}
