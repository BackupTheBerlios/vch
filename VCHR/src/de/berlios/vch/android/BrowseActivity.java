package de.berlios.vch.android;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;

public class BrowseActivity extends ListActivity implements IWebpageActivity {

    public static final String TAG = "VCHR";
    
    private static final int MENU_SETTINGS = 0;
    private static final int MENU_PLAYLIST = 1;
    private static final int MENU_DOWNLOADS = 2;

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
                if(page instanceof IOverviewPage) {
                    Intent browse = new Intent(BrowseActivity.this, BrowseActivity.class);
                    browse.putExtra(VCHURI, page.getVchUri().toString());
                    browse.putExtra(PAGE_TITLE, page.getTitle());
                    startActivity(browse);
                } else if (page instanceof IVideoPage) {
                    Intent videoDetails = new Intent(BrowseActivity.this, VideoDetailsActivity.class);
                    videoDetails.putExtra(VCHURI, page.getVchUri().toString());
                    startActivity(videoDetails);
                }
            }
        });
        
        // fill the list
        try {
            String parserUri = new Config(this).getVchParserUri();
            URI requestUri = new URI(parserUri + "?listparsers");
            if (getIntent() != null && getIntent().getExtras() != null) {
                String vchuri = getIntent().getExtras().getString(VCHURI);
                if (vchuri != null) {
                    URI _vchuri = new URI(vchuri);
                    String parser = _vchuri.getPath().substring(1);
                    int slashPos = parser.indexOf('/');
                    if (slashPos > 0) {
                        parser = parser.substring(0, slashPos);
                    }
                    requestUri = new URI(parserUri + "?id=" + parser + "&uri=" + vchuri);
                }
                
                String title = getIntent().getExtras().getString(PAGE_TITLE);
                if(title != null) {
                    setTitle(title);
                }
            }
            LoadPagesAsyncTask task = new LoadPagesAsyncTask(this, getListView());
            task.execute(requestUri);
        } catch (URISyntaxException e) {
            Log.e(getClass().getSimpleName(), "Couldn't load page", e);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_SETTINGS, 0, R.string.settings).setIcon(R.drawable.ic_menu_preferences);
        menu.add(0, MENU_PLAYLIST, 1, R.string.playlist).setIcon(R.drawable.ic_tab_playlists_unselected);
        menu.add(0, MENU_DOWNLOADS, 2, R.string.downloads).setIcon(R.drawable.ic_menu_download);
        return true;
    }

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
    
    public void updatePages(List<IWebPage> pages) {
        listAdapter.setPages(pages);
    }
}