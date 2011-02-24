package de.berlios.vch.android;

import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;

public class SearchResultAdapter extends BaseAdapter {

    private List<IWebPage> results;
    protected LayoutInflater inflater;
    protected ListView listView;
    
    public SearchResultAdapter(Context context, List<IWebPage> results) {
        this.results = results;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int i, View view, ViewGroup viewgroup) {
        IWebPage page = results.get(i);
        if (view == null) {
            view = (View) inflater.inflate(R.layout.list_item, viewgroup, false);
        }
        
        // set the title
        TextView title = (TextView) view;
        String text = page.getTitle();

        if(page instanceof IOverviewPage) {
            try {
                int resultCount = ((IOverviewPage)page).getPages().size();   
                if(resultCount > 0) {
                    text += " (" + resultCount + ")";
                }
            } catch(Exception e) {
                Log.w(BrowseActivity.TAG, "Couldn't determine number of results for provider " + page.getTitle());
            }
        }
        
        title.setText(text);
        return view;
    }

    @Override
    public int getCount() {
        return (results != null) ? results.size() : 0;
    }

    @Override
    public Object getItem(int i) {
        return results.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }
    
    public void setListView(ListView listView) {
        this.listView = listView;
    }
    
    public List<IWebPage> getResults() {
        return results;
    }

    public void setResults(List<IWebPage> results) {
        this.results = results;
        if (listView != null) { // listView maybe unintialized at this moment (if it has not been opened before)
            listView.post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                    listView.requestFocus();
                }
            });
        }
    }
}
