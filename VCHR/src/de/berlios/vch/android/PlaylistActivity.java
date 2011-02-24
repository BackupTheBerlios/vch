package de.berlios.vch.android;

import static de.berlios.vch.android.BrowseActivity.TAG;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.android.music.TouchInterceptor;
import com.android.music.TouchInterceptor.DropListener;

import de.berlios.vch.android.actions.ReorderPlaylist;

public class PlaylistActivity extends ListActivity {

    private static final int MENU_PLAY = 0;
    private static final int MENU_REMOVE = 1;

    private PlaylistListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist);
        final String playlistUri = new Config(this).getVchPlaylistUri();

        // init the listview + adapter
        ListView listView = getListView();
        adapter = new PlaylistListAdapter(this, new ArrayList<PlaylistEntry>());
        adapter.setListView(listView);
        listView.setAdapter(adapter);
        registerForContextMenu(listView);

        // add a drop listener, so that drag and drop works
        ((TouchInterceptor) listView).setDropListener(new DropListener() {
            @Override
            public void drop(int from, int to) {
                if (from == to) {
                    return;
                }

                Log.d(TAG, "Entry moved from " + from + " to " + to);

                PlaylistEntry draggedEntry = adapter.getEntries().get(from);
                List<PlaylistEntry> oldOrder = adapter.getEntries();
                List<PlaylistEntry> newOrder = new ArrayList<PlaylistEntry>(adapter.getEntries());
                newOrder.remove(from);
                newOrder.add(to, draggedEntry);
                adapter.setEntries(newOrder);

                ReorderPlaylist rp = new ReorderPlaylist(playlistUri, oldOrder, newOrder);
                new ReorderAsyncTask().execute(rp);
            }
        });

        // fill the list
        try {
            URI requestUri = new URI(playlistUri + "?action=list");
            LoadPlaylistAsyncTask task = new LoadPlaylistAsyncTask();
            task.execute(requestUri);
        } catch (URISyntaxException e) {
            Log.e(getClass().getSimpleName(), "Couldn't load page", e);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.entry);
        menu.add(0, MENU_REMOVE, 0, R.string.remove);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        PlaylistEntry entry = (PlaylistEntry) adapter.getItem(menuInfo.position);

        switch (item.getItemId()) {
        case MENU_REMOVE:
            removeEntry(entry);
            return true;
        }
        return false;
    }

    private void removeEntry(PlaylistEntry entry) {
        // ExceptionHandler eh = new ExceptionHandler() {
        // @Override
        // public void handleException(Exception e) {
        // Toast.makeText(PlaylistActivity.this, getString(R.string.remove_failed, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        // }
        // };
        // String playlistUri = new Config(this).getVchPlaylistUri();
        // new ExecuteActionAsyncTask(this, eh).execute(new RemoveFromPlaylist(playlistUri, getListView(), entry, adapter));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_PLAY, 0, R.string.play).setIcon(R.drawable.ic_menu_play_clip);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        String playlistUri = new Config(this).getVchPlaylistUri();

        switch (item.getItemId()) {
        case MENU_PLAY:
            // Action play = new StartPlaylist(playlistUri);
            // ExceptionHandler eh = new ExceptionHandler() {
            // @Override
            // public void handleException(Exception e) {
            // Toast.makeText(PlaylistActivity.this, getString(R.string.playback_failed, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
            // }
            // };
            // new ExecuteActionAsyncTask(this, eh).execute(play);
            return true;
        }

        return false;
    }

    public void handleException(Exception e) {
        Toast.makeText(this, getString(R.string.reorder_failed, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
    }

    private class LoadPlaylistAsyncTask extends AsyncTask<URI, Integer, List<PlaylistEntry>> {

        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = ProgressDialog.show(PlaylistActivity.this, "", PlaylistActivity.this.getString(R.string.loading), true);
        }

        @Override
        protected List<PlaylistEntry> doInBackground(URI... requestUris) {
            Log.i(TAG, "PlaylistLoader running " + requestUris[0]);
            URI requestUri = requestUris[0];
            List<PlaylistEntry> result = new ArrayList<PlaylistEntry>();
            try {
                HttpGet get = new HttpGet(requestUri);
                get.addHeader("X-Requested-With", "XMLHttpRequest");
                HttpClient client = new DefaultHttpClient();
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = client.execute(get, responseHandler);
                Log.v(TAG, "Server response: " + responseBody);
                JSONArray array = new JSONArray(responseBody);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject jo = array.getJSONObject(i);
                    PlaylistEntry entry = new PlaylistEntry();
                    entry.id = jo.getString("id");
                    entry.title = jo.getString("title");
                    result.add(entry);
                }
            } catch (IOException e) {
                Log.e(TAG, "Couldn't load playlist " + requestUri, e);
            } catch (JSONException e) {
                Log.e(TAG, "Couldn't parse json response", e);
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<PlaylistEntry> result) {
            Log.i(TAG, "PlaylistLoader finished");

            if (!isCancelled()) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }

                if (result != null) {
                    Log.d(TAG, "Playlist contains " + result.size() + " entries");
                    adapter.setEntries(result);
                }
            }
        }
    }

    private class ReorderAsyncTask extends AsyncTask<ReorderPlaylist, Integer, Void> {

        private ProgressDialog dialog;

        private Exception e;

        private ReorderPlaylist action;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = ProgressDialog.show(PlaylistActivity.this, "", PlaylistActivity.this.getString(R.string.executing), true);
        }

        @Override
        protected Void doInBackground(ReorderPlaylist... actions) {
            try {
                action = actions[0];
                action.execute();
            } catch (Exception e) {
                this.e = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            Log.i(TAG, "Action finished");

            if (!isCancelled()) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            }

            if (e != null) {
                List<PlaylistEntry> newOrder = action.getOldOrder();
                adapter.setEntries(newOrder);
                handleException(e);
            }
        }
    }
}