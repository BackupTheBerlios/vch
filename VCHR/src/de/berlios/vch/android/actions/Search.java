package de.berlios.vch.android.actions;

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

import android.util.Log;
import de.berlios.vch.android.BrowseActivity;
import de.berlios.vch.android.SearchResultAdapter;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;

public class Search extends Action {

    private String uri;
    private String query;
    private SearchResultAdapter adapter;
    
    public Search(String uri, String query, SearchResultAdapter adapter) {
        this.uri = uri;
        this.query = query;
        this.adapter = adapter;
    }
    
    @Override
    public void execute() throws Exception {
        String request = uri + "?action=search&q=" + URLEncoder.encode(query.toString(), "UTF-8"); 
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
        adapter.setResults(pages);
    }
    
    public static IWebPage parseJSON(JSONObject object) throws Exception {
        IWebPage page;
        if(object.has("isLeaf")) {
            page = new VideoPage();
        } else {
            IOverviewPage opage = new OverviewPage();
            page = opage;
            if(object.has("pages")) {
                JSONArray pages = object.getJSONArray("pages");
                List<IWebPage> subpages = parseJSONArray(pages);
                opage.getPages().addAll(subpages);
            }
        }
        
        if(object.has("uri") && object.getString("uri") != null) {
            page.setUri(new URI(object.getString("uri")));
        }
        page.setTitle(object.getString("title"));
        page.setParser(object.getString("parser"));
        page.getUserData().put("json", object);
        
        return page;
    }

    public static List<IWebPage> parseJSONArray(JSONArray pages) throws JSONException, Exception  {
        List<IWebPage> list = new ArrayList<IWebPage>(pages.length());
        for (int i = 0; i < pages.length(); i++) {
            list.add(parseJSON(pages.getJSONObject(i)));
        }
        return list;
    }
}
