package de.berlios.vch.android;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FinishedDownloadsAdapter extends ActiveDownloadsAdapter {

    public FinishedDownloadsAdapter(Context context, List<Download> downloads) {
        super(context, downloads);
    }

    @Override
    public View getView(int i, View view, ViewGroup viewgroup) {
        Download download = downloads.get(i);
        if (view == null) {
            view = (View) inflater.inflate(R.layout.list_item, viewgroup, false);
        }
        
        // set the title
        TextView title = (TextView) view;
        title.setText(download.title);

        return view;
    }
}
