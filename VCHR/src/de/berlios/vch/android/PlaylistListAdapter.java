package de.berlios.vch.android;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class PlaylistListAdapter extends BaseAdapter {

    private List<PlaylistEntry> entries;

    private LayoutInflater inflater;

    private ListView listView;

    public PlaylistListAdapter(Context context, List<PlaylistEntry> entries) {
        super();
        this.entries = entries;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public Object getItem(int i) {
        return entries.get(i);
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
        PlaylistEntry entry = entries.get(i);
        if (view == null) {
            view = (View) inflater.inflate(R.layout.drag_list_item, viewgroup, false);
        }
        // set the title
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(entry.title);
        return view;
    }

    public void setEntries(List<PlaylistEntry> entries) {
        this.entries = entries;
        if (listView != null) { // listView maybe unintialized at this moment (if it has not been opened before)
            listView.post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }
    
    public List<PlaylistEntry> getEntries() {
        return entries;
    }
}
