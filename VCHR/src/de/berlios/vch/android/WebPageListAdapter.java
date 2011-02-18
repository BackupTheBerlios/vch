package de.berlios.vch.android;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import de.berlios.vch.parser.IWebPage;

public class WebPageListAdapter extends BaseAdapter{
    
    private List<IWebPage> pages;
    
    private LayoutInflater inflater;
    
    private ListView listView; 
    
    public WebPageListAdapter(Context context, List<IWebPage> pages) {
        super();
        this.pages = pages;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return pages.size();
    }

    @Override
    public Object getItem(int i) {
        return pages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }
    
    public void setListView(ListView listView) {
        this.listView = listView;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewgroup) {
        IWebPage page = pages.get(i);
        if(view == null) {
            view = (View) inflater.inflate(R.layout.list_item, viewgroup, false);
        }
        // set the title
        ((TextView)view).setText(page.getTitle());
        return view;
    }

    public void setPages(List<IWebPage> pages) {
        this.pages = pages;
        if(listView != null) { // listView maybe unintialized at this moment (if it has not been opened before)
            listView.post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();                
                }
            });
        }
    }
    
    public List<IWebPage> getPages() {
        return pages;
    }
}
