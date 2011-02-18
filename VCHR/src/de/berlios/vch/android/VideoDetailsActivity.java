package de.berlios.vch.android;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import de.berlios.vch.android.ExecuteActionAsyncTask.ExceptionHandler;
import de.berlios.vch.android.actions.Action;
import de.berlios.vch.android.actions.AddDownload;
import de.berlios.vch.android.actions.AddToPlaylist;
import de.berlios.vch.android.actions.PlayVideo;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;

public class VideoDetailsActivity extends Activity implements IWebpageActivity, ExceptionHandler {

    private static final String TAG = "VCHR";
    
    private static final int MENU_ADD_TO_PLAYLIST = 0;
    private static final int MENU_PLAY = 1;
    private static final int MENU_START_DOWNLOAD = 2;

    static VideoDetailsActivity instance;
    
    private TextView title;
    private TextView desc;
    private TextView duration;
    private ImageView thumb;
    
    private URI vchuri;
    
    public VideoDetailsActivity() {
        instance = this;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.video_details);
        title = (TextView) findViewById(R.id.title);
        desc = (TextView) findViewById(R.id.desc);
        duration = (TextView) findViewById(R.id.duration);
        thumb = (ImageView) findViewById(R.id.thumb);
        
        String _vchuri = getIntent().getExtras().getString(BrowseActivity.VCHURI);
        if (_vchuri != null) {
            try {
                this.vchuri = new URI(_vchuri);
                String parser = this.vchuri.getPath().substring(1);
                int slashPos = parser.indexOf('/');
                if (slashPos > 0) {
                    parser = parser.substring(0, slashPos);
                }
                String parserUri = new Config(this).getVchParserUri();
                URI requestUri = new URI(parserUri + "?id=" + parser + "&uri=" + _vchuri);
                LoadPagesAsyncTask task = new LoadPagesAsyncTask(this, title);
                task.execute(requestUri);
            } catch (URISyntaxException e) {
                Log.e(TAG, "Couldn't load details for video", e);
            }
        }
        
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ADD_TO_PLAYLIST, 1, R.string.add_to_playlist).setIcon(R.drawable.ic_menu_add);
        menu.add(0, MENU_PLAY, 1, R.string.play).setIcon(R.drawable.ic_menu_play_clip);
        menu.add(0, MENU_START_DOWNLOAD, 2, R.string.do_download).setIcon(R.drawable.ic_menu_download);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        String playlistUri = new Config(this).getVchPlaylistUri();

        switch (item.getItemId()) {
        case MENU_ADD_TO_PLAYLIST:
            Action add = new AddToPlaylist(playlistUri, vchuri);
            new ExecuteActionAsyncTask(this, this).execute(add);
            return true;
        case MENU_PLAY:
            Action play = new PlayVideo(playlistUri, vchuri);
            new ExecuteActionAsyncTask(this, this).execute(play);
            return true;
        case MENU_START_DOWNLOAD:
            String downloadsUri = new Config(this).getVchDownloadsUri();
            Action download = new AddDownload(downloadsUri, vchuri);
            new ExecuteActionAsyncTask(this, this).execute(download);
            return true;
        }

        return false;
    }
    
    public void updatePages(List<IWebPage> pages) {
        if(pages != null && pages.size() == 1 && pages.get(0) instanceof IVideoPage) {
            IVideoPage video = (IVideoPage) pages.get(0);
            
            // title
            title.setText(video.getTitle());
            
            // duration
            if(video.getDuration() > 0) {
                String text = "";
                long secs = video.getDuration();
                if(secs < 60) {
                    text += secs + " " +getString(R.string.seconds);                        
                } else {
                    long minutes = (long) Math.floor(secs / 60);
                    if(minutes < 10) text = "0";
                    text += minutes + ":";
                    secs = secs % 60;
                    if(secs < 10) text += "0";
                    text += secs + " ";
                    text += getString(R.string.minutes); 
                }
                duration.setText(text);
            } else {
                duration.setVisibility(View.GONE);
            }
            
            
            // load thumb if available
            if(video.getThumbnail() != null) {
                InputStream in;
                try {
                    in = video.getThumbnail().toURL().openStream();
                    Drawable thumbnail = Drawable.createFromStream(in, video.getThumbnail().getPath());
                    thumb.setImageDrawable(thumbnail);
                } catch (Exception e) {
                    Log.e(TAG, "Couldn't load thumbnail from web", e);
                } 
            } else {
                thumb.setVisibility(View.GONE);
            }
            
            // description
            if(video.getDescription() != null) {
                desc.setText(video.getDescription());
            } else {
                thumb.setVisibility(View.GONE);
            }
        }
    }
    
    @Override
    public void handleException(Exception e) {
        Log.e(BrowseActivity.TAG, "An error occured while communicating with VCH", e);
        Toast.makeText(this, getString(R.string.communication_error, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
    }
    
    /*
    private class ExecuteActionAsyncTask extends AsyncTask<Action, Integer, Void> {

        private ProgressDialog dialog;
        
        private Exception e;
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = ProgressDialog.show(VideoDetailsActivity.this, "", VideoDetailsActivity.this.getString(R.string.executing), true);
        }
        
        @Override
        protected Void doInBackground(Action... actions) {
            try {
                actions[0].execute();
            } catch (Exception e) {
                this.e = e;
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            
            Log.i(TAG, "Action finished");
            
            if(!isCancelled()) {
                if(dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
            
            if(e != null) {
                handleException(e);
            }
        }
    }*/
}