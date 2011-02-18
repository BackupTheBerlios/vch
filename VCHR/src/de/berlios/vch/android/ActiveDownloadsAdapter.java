package de.berlios.vch.android;

import java.text.DecimalFormat;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ActiveDownloadsAdapter extends BaseAdapter {

    protected List<Download> downloads;

    protected LayoutInflater inflater;

    protected ListView listView;

    public ActiveDownloadsAdapter(Context context, List<Download> downloads) {
        super();
        this.downloads = downloads;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return downloads.size();
    }

    @Override
    public Object getItem(int i) {
        return downloads.get(i);
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
        Download download = downloads.get(i);
        if (view == null) {
            view = (View) inflater.inflate(R.layout.active_download, viewgroup, false);
        }
        
        // set the progress
        ProgressBar progress = (ProgressBar) view.findViewById(R.id.progress);
        progress.setProgress(download.progress);
        
        // set the title
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(download.title);
        
        // set the throughput / status
        TextView throughput = (TextView) view.findViewById(R.id.throughput);
        if("DOWNLOADING".equals(download.status)) {
            DecimalFormat df = new DecimalFormat("0.##");
            float throughputValue = Math.max(download.throughput, 0f);
            throughput.setText(df.format(throughputValue) + " KiB/s");
        } else {
            throughput.setText(download.status);
        }
        
        return view;
    }
    
    public List<Download> getDownloads() {
        return downloads;
    }

    public void setDownloads(List<Download> downloads) {
        this.downloads = downloads;
        if (listView != null) { // listView maybe unintialized at this moment (if it has not been opened before)
            listView.post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    public static class Download {
        public String id;
        public String title;
        public int progress;
        public float throughput;
        public String status;
    }
}
