package de.berlios.vch.android;

import static de.berlios.vch.android.BrowseActivity.TAG;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import de.berlios.vch.android.ActiveDownloadsAdapter.Download;
import de.berlios.vch.android.actions.DeleteDownload;
import de.berlios.vch.android.actions.StartAllDownloads;
import de.berlios.vch.android.actions.StartDownload;
import de.berlios.vch.android.actions.StopAllDownloads;
import de.berlios.vch.android.actions.StopDownload;

public class ActiveDownloadsActivity extends ListActivity {

    private static final int MENU_START = 0;
    private static final int MENU_STOP = 1;
    private static final int MENU_DELETE = 2;
    private static final int MENU_START_ALL = 3;
    private static final int MENU_STOP_ALL = 4;

    private ActiveDownloadsAdapter listAdapter;
    private Timer updateTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create the list
        ListView lv = getListView();
        registerForContextMenu(lv);
        listAdapter = new ActiveDownloadsAdapter(this, new ArrayList<Download>());
        setListAdapter(listAdapter);
        listAdapter.setListView(lv);
    }

    @Override
    protected void onResume() {
        super.onResume();

        TimerTask updateTask = new TimerTask() {
            @Override
            public void run() {
                new ListActiveDownloads().execute();
            }
        };

        updateTimer = new Timer("Active Downloads Updater");
        updateTimer.scheduleAtFixedRate(updateTask, 0, TimeUnit.SECONDS.toMillis(2));
    }

    @Override
    protected void onPause() {
        updateTimer.cancel();
        super.onPause();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.download);
        menu.add(0, MENU_START, 0, R.string.start);
        menu.add(0, MENU_STOP, 1, R.string.stop);
        menu.add(0, MENU_DELETE, 2, R.string.delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        Download download = (Download) listAdapter.getItem(menuInfo.position);

        switch (item.getItemId()) {
        case MENU_START:
            new StartDownload(this).execute(download.id);
            return true;
        case MENU_STOP:
            new StopDownload(this).execute(download.id);
            return true;
        case MENU_DELETE:
            new DeleteDownload(this).execute(download.id);
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_START_ALL, 0, R.string.start_all).setIcon(R.drawable.ic_menu_download);
        menu.add(0, MENU_STOP_ALL, 1, R.string.stop_all).setIcon(R.drawable.ic_menu_stop);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
        case MENU_START_ALL:
            new StartAllDownloads(this).execute();
            return true;
        case MENU_STOP_ALL:
            new StopAllDownloads(this).execute();
            return true;
        }

        return false;
    }

    private class ListActiveDownloads extends AsyncTask<Void, Void, List<Download>> {

        private Exception e;

        @Override
        protected List<Download> doInBackground(Void... params) {
            String downloadsUri = new Config(ActiveDownloadsActivity.this).getVchDownloadsUri();
            String requestUri = downloadsUri + "?action=list_active";
            Log.d(BrowseActivity.TAG, "Sending request " + requestUri);
            final List<Download> result = new ArrayList<Download>();

            try {
                HttpGet get = new HttpGet(requestUri);
                get.addHeader("X-Requested-With", "XMLHttpRequest");
                HttpClient client = new DefaultHttpClient();
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = client.execute(get, responseHandler);
                Log.v(TAG, "Server response: " + responseBody);

                // parse the response
                JSONArray array = new JSONArray(responseBody);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject jo = array.getJSONObject(i);
                    Download download = new Download();
                    download.id = jo.getString("id");
                    download.title = jo.getString("title");
                    download.status = jo.getString("status");
                    download.progress = jo.getInt("progress");
                    download.throughput = (float) jo.getDouble("throughput");
                    result.add(download);
                }
            } catch (Exception e) {
                this.e = e;
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<Download> result) {
            Log.i(TAG, "Getting active downloads finished");

            if (!isCancelled()) {
                if (result != null) {
                    Log.d(TAG, result.size() + " active downloads");
                    listAdapter.setDownloads(result);
                }

                if (e != null) {
                    Log.e(BrowseActivity.TAG, "Updating active downloads list failed", e);
                }
            }
        }
    }
}