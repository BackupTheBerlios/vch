package de.berlios.vch.android;

import static de.berlios.vch.android.BrowseActivity.TAG;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import de.berlios.vch.android.ActiveDownloadsAdapter.Download;

public class FinishedDownloadsActivity extends ListActivity {

    private static final int MENU_DELETE = 0;

    private FinishedDownloadsAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create the list
        ListView lv = getListView();
        registerForContextMenu(lv);
        listAdapter = new FinishedDownloadsAdapter(this, new ArrayList<Download>());
        setListAdapter(listAdapter);
        listAdapter.setListView(lv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new ListFinishedDownloads(this).execute();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.download);
        menu.add(0, MENU_DELETE, 0, R.string.delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        Download download = (Download) listAdapter.getItem(menuInfo.position);

        switch (item.getItemId()) {
        case MENU_DELETE:
            new DeleteFinishedDownload(this).execute(download);
            return true;
        }
        return false;
    }

    private class ListFinishedDownloads extends VchrAsyncTask<Void, Void, List<Download>> {

        public ListFinishedDownloads(Context ctx) {
            super(ctx);
        }

        @Override
        protected List<Download> doTheWork(Void... params) throws Exception {
            String downloadsUri = new Config(FinishedDownloadsActivity.this).getVchDownloadsUri();
            String requestUri = downloadsUri + "?action=list_finished";
            final List<Download> result = new ArrayList<Download>();
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
                result.add(download);
            }

            return result;
        }

        @Override
        protected void finished(List<Download> result) {
            // update the listview
            listAdapter.setDownloads(result);
        }

        @Override
        protected void handleException(Exception e) {
            Toast.makeText(FinishedDownloadsActivity.this, getString(R.string.error_loading, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        }
    }

    public class DeleteFinishedDownload extends VchrAsyncTask<Download, Void, String> {

        private Download download;

        public DeleteFinishedDownload(Context ctx) {
            super(ctx);
        }

        @Override
        protected String doTheWork(Download... downloads) throws Exception {
            this.download = downloads[0];
            String downloadsUri = new Config(FinishedDownloadsActivity.this).getVchDownloadsUri();
            String request = downloadsUri + "?action=delete_finished&id=" + download.id;
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
            if ("ok".equalsIgnoreCase(response.trim())) {
                listAdapter.getDownloads().remove(download);
                getListView().post(new Runnable() {
                    @Override
                    public void run() {
                        listAdapter.notifyDataSetChanged();
                    }
                });
            } else {
                Toast.makeText(FinishedDownloadsActivity.this, getString(R.string.remove_failed, response), Toast.LENGTH_LONG).show();
            }

        }

        @Override
        protected void handleException(Exception e) {
            Toast.makeText(FinishedDownloadsActivity.this, getString(R.string.remove_failed, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
            Log.e(BrowseActivity.TAG, getString(R.string.remove_failed, e.getLocalizedMessage()), e);
        }
    }
}