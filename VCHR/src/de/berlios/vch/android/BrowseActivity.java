package de.berlios.vch.android;

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

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;

public class BrowseActivity extends ListActivity {

    public static final String TAG = "VCHR";

    private static final int MENU_SETTINGS = 0;
    private static final int MENU_PLAYLIST = 1;
    private static final int MENU_DOWNLOADS = 2;
    private static final int MENU_SEARCH = 3;

    public static final int DIALOG_LOADING = 1000;

    static BrowseActivity instance;

    private WebPageListAdapter listAdapter;

    public static final String VCHURI = "vchuri";
    public static final String PAGE_TITLE = "page_title";

    public BrowseActivity() {
        instance = this;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create the list
        ListView lv = getListView();
        lv.setFastScrollEnabled(true);
        listAdapter = new WebPageListAdapter(this, new ArrayList<IWebPage>());
        setListAdapter(listAdapter);
        listAdapter.setListView(lv);
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                IWebPage page = (IWebPage) listAdapter.getItem(position);
                if (page instanceof IOverviewPage) {
                    Intent browse = new Intent(BrowseActivity.this, BrowseActivity.class);
                    browse.putExtra(VCHURI, page.getVchUri().toString());
                    browse.putExtra(PAGE_TITLE, page.getTitle());
                    startActivity(browse);
                } else if (page instanceof IVideoPage) {
                    Intent intent = new Intent(BrowseActivity.this, VideoDetailsActivity.class);
                    intent.putExtra(VCHURI, page.getVchUri().toString());
                    startActivity(intent);
                }
            }
        });

        // fill the list
        if (getIntent() != null && getIntent().getExtras() != null) {
            String vchuri = getIntent().getExtras().getString(VCHURI);
            if (vchuri != null) {
                LoadOverviewPage task = new LoadOverviewPage(BrowseActivity.this);
                task.execute(vchuri);

                String title = getIntent().getExtras().getString(PAGE_TITLE);
                if (title != null) {
                    setTitle(title);
                }
            }
        } else {
            // execute the request
            LoadOverviewPage task = new LoadOverviewPage(BrowseActivity.this);
            task.execute();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_SETTINGS, 0, R.string.settings).setIcon(R.drawable.ic_menu_preferences);
        menu.add(0, MENU_PLAYLIST, 1, R.string.playlist).setIcon(R.drawable.ic_tab_playlists_unselected);
        menu.add(0, MENU_DOWNLOADS, 2, R.string.downloads).setIcon(R.drawable.ic_menu_download);
        menu.add(0, MENU_SEARCH, 3, R.string.search).setIcon(R.drawable.ic_menu_search);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
        case MENU_SETTINGS:
            Intent intent = new Intent(this, Settings.class);
            startActivity(intent);
            return true;
        case MENU_PLAYLIST:
            intent = new Intent(this, PlaylistActivity.class);
            startActivity(intent);
            return true;
        case MENU_DOWNLOADS:
            intent = new Intent(this, DownloadActivity.class);
            startActivity(intent);
            return true;
        case MENU_SEARCH:
            intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
            return true;
        }

        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_LOADING:
            return ProgressDialog.show(this, "", getString(R.string.loading), true);
        }
        return super.onCreateDialog(id);
    }

    private class LoadOverviewPage extends VchrAsyncTask<String, Void, List<IWebPage>> {
        public LoadOverviewPage(Context ctx) {
            super(ctx, R.string.loading);
        }

        @Override
        protected List<IWebPage> doTheWork(String... uris) throws Exception {
            String request;
            String parserUri = new Config(BrowseActivity.this).getVchParserUri();

            if (uris.length > 0) {
                URI vchuri = new URI(uris[0]);
                String parser = vchuri.getPath().substring(1);
                int slashPos = parser.indexOf('/');
                if (slashPos > 0) {
                    parser = parser.substring(0, slashPos);
                }
                request = parserUri + "?id=" + parser + "&uri=" + vchuri;
            } else {
                request = parserUri + "?listparsers";
            }

            Log.d(BrowseActivity.TAG, "Sending request " + request);
            HttpGet get = new HttpGet(request);
            get.addHeader("X-Requested-With", "XMLHttpRequest");
            HttpClient client = new DefaultHttpClient();
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = client.execute(get, responseHandler);
            Log.v(BrowseActivity.TAG, "Server response: " + responseBody);
            return parseOverviewPage(responseBody);
        }

        private List<IWebPage> parseOverviewPage(String responseBody) throws JSONException, URISyntaxException {
            List<IWebPage> result = new ArrayList<IWebPage>();
            JSONArray array = new JSONArray(responseBody);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.getJSONObject(i);
                String title = json.getJSONObject("data").getString("title");
                String id = json.getJSONObject("attributes").getString("id");
                Log.v(TAG, "Found page " + title + " " + id);
                IWebPage page;
                if (json.getJSONObject("attributes").has("vchisLeaf")) {
                    page = new VideoPage();
                } else {
                    page = new OverviewPage();
                }
                page.setTitle(title);
                page.setVchUri(new URI(id));
                result.add(page);
            }
            return result;
        }

        @Override
        protected void finished(List<IWebPage> result) {
            listAdapter.setPages(result);
        }

        @Override
        protected void handleException(Exception e) {
            Toast.makeText(ctx, ctx.getString(R.string.error_loading, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        }
    }
}