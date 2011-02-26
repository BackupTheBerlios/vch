package de.berlios.vch.android;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Calendar;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import de.berlios.vch.android.VchrAsyncTask.TaskCallback;
import de.berlios.vch.android.actions.AddDownload;
import de.berlios.vch.android.actions.AddToPlaylist;
import de.berlios.vch.android.actions.ClearPlaylist;
import de.berlios.vch.android.actions.StartPlaylist;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.VideoPage;

public class VideoDetailsActivity extends Activity {

    private static final String TAG = "VCHR";

    private static final int MENU_ADD_TO_PLAYLIST = 0;
    private static final int MENU_PLAY = 1;
    private static final int MENU_START_DOWNLOAD = 2;

    private TextView title;
    private TextView desc;
    private TextView duration;
    private ImageView thumb;

    protected String vchuri;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.video_details);
        title = (TextView) findViewById(R.id.title);
        desc = (TextView) findViewById(R.id.desc);
        duration = (TextView) findViewById(R.id.duration);
        thumb = (ImageView) findViewById(R.id.thumb);

        vchuri = getIntent().getExtras().getString("vchuri");
        loadVideoPage();
    }

    protected void loadVideoPage() {
        new LoadVideoPage(this).execute(vchuri);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ADD_TO_PLAYLIST, 1, R.string.add_to_playlist).setIcon(R.drawable.ic_menu_add);
        menu.add(0, MENU_PLAY, 1, R.string.play).setIcon(R.drawable.ic_menu_play_clip);
        menu.add(0, MENU_START_DOWNLOAD, 2, R.string.do_download).setIcon(R.drawable.ic_menu_download);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
        case MENU_ADD_TO_PLAYLIST:
            AddToPlaylist add = new AddToPlaylist(this);
            add.execute(vchuri);
            return true;
        case MENU_PLAY:
            play(vchuri);
            return true;
        case MENU_START_DOWNLOAD:
            AddDownload download = new AddDownload(this);
            download.execute(vchuri);
            return true;
        }

        return false;
    }

    private void play(final String vchuri) {
        // 3 actions serialized with callbacks:
        // 1. clear the playlist
        // 2. if that was successful, add the video to the playlist
        // 3. if that was successful, too,
        // start the playback of the playlist
        ClearPlaylist cp = new ClearPlaylist(this);
        cp.setCallback(new TaskCallback<String>() {
            @Override
            public void failed(Exception e) {
            }

            @Override
            public void success(String r) {
                if ("ok".equalsIgnoreCase(r.trim())) {
                    AddToPlaylist atp = new AddToPlaylist(VideoDetailsActivity.this);
                    atp.setCallback(new TaskCallback<String>() {
                        @Override
                        public void failed(Exception e) {
                        }

                        @Override
                        public void success(String r) {
                            if ("ok".equalsIgnoreCase(r.trim())) {
                                new StartPlaylist(VideoDetailsActivity.this).execute();
                            }
                        }
                    });
                    atp.execute(vchuri);
                }
            }
        });

        cp.execute();
    }

    public void updateView(String title, String desc, long duration, URI thumb) {
        // title
        this.title.setText(title);

        // duration
        if (duration > 0) {
            String text = "";
            long secs = duration;
            if (secs < 60) {
                text += secs + " " + getString(R.string.seconds);
            } else {
                long minutes = (long) Math.floor(secs / 60);
                if (minutes < 10) {
                    text = "0";
                }
                text += minutes + ":";
                secs = secs % 60;
                if (secs < 10) {
                    text += "0";
                }
                text += secs + " ";
                text += getString(R.string.minutes);
            }
            this.duration.setText(text);
        } else {
            this.duration.setVisibility(View.GONE);
        }

        // load thumb if available
        if (thumb != null) {
            InputStream in;
            try {
                in = thumb.toURL().openStream();
                Drawable thumbnail = Drawable.createFromStream(in, thumb.getPath());
                this.thumb.setImageDrawable(thumbnail);
            } catch (Exception e) {
                Log.e(TAG, "Couldn't load thumbnail from web", e);
            }
        } else {
            this.thumb.setVisibility(View.GONE);
        }

        // description
        if (desc != null) {
            this.desc.setText(desc);
        } else {
            this.desc.setVisibility(View.GONE);
        }
    }

    private class LoadVideoPage extends VchrAsyncTask<String, Void, IVideoPage> {

        public LoadVideoPage(Context ctx) {
            super(ctx, R.string.loading);
        }

        @Override
        protected IVideoPage doTheWork(String... uris) throws Exception {
            String request;
            String parserUri = new Config(VideoDetailsActivity.this).getVchParserUri();

            if (uris.length > 0) {
                URI vchuri = new URI(uris[0]);
                String parser = vchuri.getPath().substring(1);
                int slashPos = parser.indexOf('/');
                if (slashPos > 0) {
                    parser = parser.substring(0, slashPos);
                }
                request = parserUri + "?id=" + parser + "&uri=" + URLEncoder.encode(vchuri.toString(), "UTF-8");

                Log.d(BrowseActivity.TAG, "Sending request " + request);
                HttpGet get = new HttpGet(request);
                get.addHeader("X-Requested-With", "XMLHttpRequest");
                HttpClient client = new DefaultHttpClient();
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = client.execute(get, responseHandler);
                Log.v(BrowseActivity.TAG, "Server response: " + responseBody);
                return parseVideoPage(responseBody);
            } else {
                throw new IllegalArgumentException("No VCH URI specified");
            }
        }

        private IVideoPage parseVideoPage(String responseBody) throws JSONException, URISyntaxException {
            JSONObject response = new JSONObject(responseBody);
            JSONObject video = response.getJSONObject("video");
            JSONObject attrs = video.getJSONObject("attributes");
            IVideoPage videoPage = new VideoPage();

            // title
            String title = video.getJSONObject("data").getString("title");
            videoPage.setTitle(title);

            // vchuri
            String id = video.getJSONObject("attributes").getString("id");
            videoPage.setVchUri(new URI(id));

            // video uri
            if (attrs.has("vchvideo")) {
                String vchvideo = video.getJSONObject("attributes").getString("vchvideo");
                videoPage.setVideoUri(new URI(vchvideo));
            }

            // publish date
            if (attrs.has("vchpubDate")) {
                String vchpubdate = video.getJSONObject("attributes").getString("vchpubDate");
                Calendar pubDate = Calendar.getInstance();
                pubDate.setTimeInMillis(Long.parseLong(vchpubdate));
                videoPage.setPublishDate(pubDate);
            }

            // webpage
            if (attrs.has("vchlink")) {
                String vchlink = video.getJSONObject("attributes").getString("vchlink");
                videoPage.setUri(new URI(vchlink));
            }

            // description
            if (attrs.has("vchdesc")) {
                String vchdesc = video.getJSONObject("attributes").getString("vchdesc");
                videoPage.setDescription(vchdesc);
            }

            // thumbnail
            if (attrs.has("vchthumb")) {
                String vchthumb = video.getJSONObject("attributes").getString("vchthumb");
                videoPage.setThumbnail(new URI(vchthumb));
            }

            // duration
            if (attrs.has("vchduration")) {
                String vchduration = video.getJSONObject("attributes").getString("vchduration");
                videoPage.setDuration(Long.parseLong(vchduration));
            }

            return videoPage;
        }

        @Override
        protected void finished(IVideoPage result) {
            updateView(result.getTitle(), result.getDescription(), result.getDuration(), result.getThumbnail());
        }

        @Override
        protected void handleException(Exception e) {
            Toast.makeText(ctx, ctx.getString(R.string.error_loading, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
            Log.e(TAG, getString(R.string.loading), e);
        }
    }

}