package de.berlios.vch.android;

import java.util.ArrayList;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import de.berlios.vch.android.ActiveDownloadsAdapter.Download;
import de.berlios.vch.android.actions.CompositeAction;
import de.berlios.vch.android.actions.DeleteFinishedDownload;
import de.berlios.vch.android.actions.ListFinishedDownloads;

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

        // ExceptionHandler eh = new ExceptionHandler() {
        // @Override
        // public void handleException(Exception e) {
        // Toast.makeText(FinishedDownloadsActivity.this, getString(R.string.error_loading, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        // }
        // };
        // String downloadsUri = new Config(this).getVchDownloadsUri();
        // new ExecuteActionAsyncTask(this, eh).execute(new ListFinishedDownloads(downloadsUri, listAdapter));
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
            String downloadsUri = new Config(this).getVchDownloadsUri();
            CompositeAction action = new CompositeAction();
            action.addAction(new DeleteFinishedDownload(downloadsUri, download.id));
            action.addAction(new ListFinishedDownloads(downloadsUri, listAdapter));
            // ExceptionHandler eh = new ExceptionHandler() {
            // @Override
            // public void handleException(Exception e) {
            // Toast.makeText(FinishedDownloadsActivity.this, getString(R.string.remove_failed, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
            // }
            // };
            // new ExecuteActionAsyncTask(this, eh).execute(action);
            return true;
        }
        return false;
    }
}