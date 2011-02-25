package de.berlios.vch.android;

import java.net.URI;
import java.net.URLEncoder;
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
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;

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

        // execute the search asynchronously
        new Search(this).execute(query.getText().toString());

        // hide the soft keyboard
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

    public static IWebPage parseJSON(JSONObject object) throws Exception {
        IWebPage page;
        if (object.has("isLeaf")) {
            page = new VideoPage();
        } else {
            IOverviewPage opage = new OverviewPage();
            page = opage;
            if (object.has("pages")) {
                JSONArray pages = object.getJSONArray("pages");
                List<IWebPage> subpages = parseJSONArray(pages);
                opage.getPages().addAll(subpages);
            }
        }

        if (object.has("uri") && object.getString("uri") != null) {
            page.setUri(new URI(object.getString("uri")));
        }
        page.setTitle(object.getString("title"));
        page.setParser(object.getString("parser"));
        page.getUserData().put("json", object);

        return page;
    }

    public static List<IWebPage> parseJSONArray(JSONArray pages) throws JSONException, Exception {
        List<IWebPage> list = new ArrayList<IWebPage>(pages.length());
        for (int i = 0; i < pages.length(); i++) {
            list.add(parseJSON(pages.getJSONObject(i)));
        }
        return list;
    }

    private class Search extends VchrAsyncTask<String, Void, List<IWebPage>> {

        public Search(Context ctx) {
            super(ctx);
        }

        @Override
        protected List<IWebPage> doTheWork(String... queries) throws Exception {
            String uri = new Config(SearchActivity.this).getVchSearchUri();
            String request = uri + "?action=search&q=" + URLEncoder.encode(queries[0], "UTF-8");
            HttpGet get = new HttpGet(request);
            get.addHeader("X-Requested-With", "XMLHttpRequest");
            HttpClient client = new DefaultHttpClient();
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = client.execute(get, responseHandler);
            Log.v(BrowseActivity.TAG, "Server response: " + responseBody);

            JSONObject result = new JSONObject(responseBody);
            JSONArray providers = result.getJSONArray("pages");
            List<IWebPage> pages = new ArrayList<IWebPage>();
            for (int i = 0; i < providers.length(); i++) {
                JSONObject provider = providers.getJSONObject(i);
                IWebPage page = parseJSON(provider);
                pages.add(page);
            }

            return pages;
        }

        @Override
        protected void finished(List<IWebPage> result) {
            adapter.setResults(result);
        }

        @Override
        protected void handleException(Exception e) {
            Log.e(BrowseActivity.TAG, "Couldn't execute search", e); // TODO toast
        }
    }
}
