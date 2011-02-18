package de.berlios.vch.android;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import android.app.ListActivity;
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
import de.berlios.vch.android.ActiveDownloadsAdapter.Download;
import de.berlios.vch.android.ExecuteActionAsyncTask.ExceptionHandler;
import de.berlios.vch.android.actions.Action;
import de.berlios.vch.android.actions.DeleteDownload;
import de.berlios.vch.android.actions.ListActiveDownloads;
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
        
        final String downloadsUri = new Config(this).getVchDownloadsUri();
        TimerTask updateTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    new ListActiveDownloads(downloadsUri, listAdapter).execute();
                } catch (Exception e) {
                    Log.e(BrowseActivity.TAG, "Updating active downloads list failed", e);
                }
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
        AdapterContextMenuInfo menuInfo =  (AdapterContextMenuInfo) item.getMenuInfo();
        Download download = (Download) listAdapter.getItem(menuInfo.position);
        
        switch(item.getItemId()) {
            case MENU_START:
                startDownload(download);
                return true;
            case MENU_STOP:
                stopDownload(download);
                return true;
            case MENU_DELETE:
                deleteDownload(download);
                return true;
        }
        return false;
    }

    private void startDownload(Download download) {
        final String downloadsUri = new Config(this).getVchDownloadsUri();
        StartDownload action = new StartDownload(downloadsUri, download.id);
        ExceptionHandler eh = new ExceptionHandler() {
            @Override
            public void handleException(Exception e) {
                Toast.makeText(ActiveDownloadsActivity.this, getString(R.string.start_failed, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
            }
        };
        new ExecuteActionAsyncTask(this, eh).execute(action);
    }
    
    private void stopDownload(Download download) {
        final String downloadsUri = new Config(this).getVchDownloadsUri();
        StopDownload action = new StopDownload(downloadsUri, download.id);
        ExceptionHandler eh = new ExceptionHandler() {
            @Override
            public void handleException(Exception e) {
                Toast.makeText(ActiveDownloadsActivity.this, getString(R.string.stop_failed, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
            }
        };
        new ExecuteActionAsyncTask(this, eh).execute(action);
    }
    
    private void deleteDownload(Download download) {
        final String downloadsUri = new Config(this).getVchDownloadsUri();
        DeleteDownload action = new DeleteDownload(downloadsUri, download.id);
        ExceptionHandler eh = new ExceptionHandler() {
            @Override
            public void handleException(Exception e) {
                Toast.makeText(ActiveDownloadsActivity.this, getString(R.string.remove_failed, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
            }
        };
        new ExecuteActionAsyncTask(this, eh).execute(action);
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
        String downloadsUri = new Config(this).getVchDownloadsUri();

        switch (item.getItemId()) {
        case MENU_START_ALL:
            Action startAll = new StartAllDownloads(downloadsUri);
            ExceptionHandler eh = new ExceptionHandler() {
                @Override
                public void handleException(Exception e) {
                    Toast.makeText(ActiveDownloadsActivity.this, getString(R.string.start_failed, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                }
            };
            new ExecuteActionAsyncTask(this, eh).execute(startAll);
            return true;
        case MENU_STOP_ALL:
            Action stopAll = new StopAllDownloads(downloadsUri);
            eh = new ExceptionHandler() {
                @Override
                public void handleException(Exception e) {
                    Toast.makeText(ActiveDownloadsActivity.this, getString(R.string.start_failed, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                }
            };
            new ExecuteActionAsyncTask(this, eh).execute(stopAll);
            return true;
        }

        return false;
    }
}