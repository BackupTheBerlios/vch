package de.berlios.vch.android;

import android.app.TabActivity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.widget.TabHost;

public class DownloadActivity extends TabActivity {

    private AnimationDrawable activeDownloadsIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.downloads);

        TabHost tabHost = getTabHost(); // The activity TabHost
        TabHost.TabSpec spec; // Reusable TabSpec for each tab
        Intent intent; // Reusable Intent for each tab

        // create the intents and add them as tabs
        intent = new Intent().setClass(this, ActiveDownloadsActivity.class);
        activeDownloadsIcon = new AnimationDrawable();
        activeDownloadsIcon.addFrame(getResources().getDrawable(R.drawable.stat_sys_download_anim0), 50);
        activeDownloadsIcon.addFrame(getResources().getDrawable(R.drawable.stat_sys_download_anim1), 50);
        activeDownloadsIcon.addFrame(getResources().getDrawable(R.drawable.stat_sys_download_anim2), 50);
        activeDownloadsIcon.addFrame(getResources().getDrawable(R.drawable.stat_sys_download_anim3), 50);
        activeDownloadsIcon.addFrame(getResources().getDrawable(R.drawable.stat_sys_download_anim4), 50);
        activeDownloadsIcon.addFrame(getResources().getDrawable(R.drawable.stat_sys_download_anim5), 50);
        activeDownloadsIcon.setOneShot(false);
        activeDownloadsIcon.start();
        spec = tabHost.newTabSpec("active").setIndicator(getString(R.string.active_downloads), activeDownloadsIcon).setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, FinishedDownloadsActivity.class);
        spec = tabHost.newTabSpec("finished").setIndicator(getString(R.string.finished_downloads)).setContent(intent);
        tabHost.addTab(spec);
    }
}