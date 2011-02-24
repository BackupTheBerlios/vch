package de.berlios.vch.android;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;

public class SearchActivity extends Activity implements OnItemClickListener {

    private EditText query;
    private SearchResultAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search);
        ListView lv = (ListView) findViewById(R.id.results);
        adapter = new SearchResultAdapter(this, new ArrayList<IWebPage>());
        adapter.setListView(lv);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);

        query = (EditText) findViewById(R.id.query);
        query.setText("Google");
        query.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent key) {
                search();
                return true;
            }

        });

        Button search = (Button) findViewById(R.id.search);
        search.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                search();
            }
        });
    }

    private void search() {
        Log.i(BrowseActivity.TAG, "Searching for " + query.getText());

        // ExceptionHandler eh = new ExceptionHandler() {
        // @Override
        // public void handleException(Exception e) {
        // // TODO show toast
        // Log.e(BrowseActivity.TAG, "Couldn't execute search", e);
        // }
        // };
        //        
        // String uri = new Config(this).getVchSearchUri();
        // new ExecuteActionAsyncTask(this, eh).execute(new Search(uri, query.getText().toString(), adapter));

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(query.getApplicationWindowToken(), 0);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        IWebPage page = adapter.getResults().get(position);
        if (page instanceof IOverviewPage) {
            try {
                JSONArray subpages = ((JSONObject) page.getUserData().get("json")).getJSONArray("pages");
                Intent intent = new Intent(this, SearchBrowseActivity.class);
                intent.putExtra("pages", subpages.toString());
                startActivity(intent);
            } catch (JSONException e) {
                Log.e(BrowseActivity.TAG, "Couldn't open subpages", e); // TODO show toast
            }
        }
    }
}
