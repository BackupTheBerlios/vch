package de.berlios.vch.android;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;

public class SearchBrowseActivity extends ListActivity {

    private SearchResultAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<IWebPage> results = new ArrayList<IWebPage>();

        // parse json if passed by the parent activity
        if (getIntent().getStringExtra("pages") != null) {
            try {
                JSONArray pages = new JSONArray(getIntent().getStringExtra("pages"));
                Log.d(BrowseActivity.TAG, pages.toString());
                results = SearchActivity.parseJSONArray(pages);
            } catch (JSONException e) {
                Log.e(BrowseActivity.TAG, "Couldn't parse JSON array", e); // TODO show toast
            } catch (Exception e) {
                Log.e(BrowseActivity.TAG, "Couldn't create list from JSON array", e); // TODO show toast
            }
        }

        // create the list
        ListView lv = getListView();
        registerForContextMenu(lv);
        listAdapter = new SearchResultAdapter(this, results);
        setListAdapter(listAdapter);
        listAdapter.setListView(lv);

        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                IWebPage page = listAdapter.getResults().get(position);
                if (page instanceof IVideoPage) {
                    Log.i(BrowseActivity.TAG, "Parse search result " + page.getTitle() + " " + page.getUri());
                    Intent intent = new Intent(SearchBrowseActivity.this, SearchVideoDetailsActivity.class);
                    intent.putExtra("parser", page.getParser());
                    intent.putExtra("uri", page.getUri().toString());
                    SearchBrowseActivity.this.startActivity(intent);
                } else {
                    // TODO toast nested search results not implemented
                }
            }
        });
    }
}
