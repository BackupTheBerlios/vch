package de.berlios.vch.android;

import java.net.URI;
import java.net.URLEncoder;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.VideoPage;

public class SearchVideoDetailsActivity extends VideoDetailsActivity {

    @Override
    protected void loadVideoPage() {
        String id = getIntent().getExtras().getString("parser");
        String uri = getIntent().getExtras().getString("uri");
        new SearchBrowse(this).execute(id, uri);
    }

    private class SearchBrowse extends VchrAsyncTask<String, Void, IVideoPage> {

        public SearchBrowse(Context ctx) {
            super(ctx);
        }

        @Override
        protected IVideoPage doTheWork(String... params) throws Exception {
            String id = params[0];
            String uri = params[1];
            String requestUri = new Config(SearchVideoDetailsActivity.this).getVchSearchUri();
            String request = requestUri + "?action=parse&isVideoPage=true&id=" + id + "&uri=" + URLEncoder.encode(uri, "UTF-8");
            HttpGet get = new HttpGet(request);
            get.addHeader("X-Requested-With", "XMLHttpRequest");
            HttpClient client = new DefaultHttpClient();
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = client.execute(get, responseHandler);
            Log.v(BrowseActivity.TAG, "Server response: " + responseBody);

            IVideoPage video = new VideoPage();
            JSONObject json = new JSONObject(responseBody).getJSONObject("video");

            if (json.has("title")) {
                video.setTitle(json.getString("title"));
            }
            if (json.has("desc")) {
                video.setDescription(json.getString("desc"));
            }
            if (json.has("duration")) {
                video.setDuration(json.getLong("duration"));
            }
            if (json.has("thumb")) {
                video.setThumbnail(new URI(json.getString("thumb")));
            }
            if (json.has("vchuri")) {
                vchuri = json.getString("vchuri");
                video.setVchUri(new URI(vchuri));
            }
            return video;
        }

        @Override
        protected void finished(IVideoPage video) {
            updateView(video.getTitle(), video.getDescription(), video.getDuration(), video.getThumbnail());
        }

        @Override
        protected void handleException(Exception e) {
            String msg = getString(R.string.search_failed, e.getLocalizedMessage());
            Toast.makeText(SearchVideoDetailsActivity.this, msg, Toast.LENGTH_LONG).show();
            Log.e(BrowseActivity.TAG, "Couldn't open load page", e);
        }
    }
}