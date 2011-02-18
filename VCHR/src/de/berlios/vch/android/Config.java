package de.berlios.vch.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Config {

    private SharedPreferences prefs;
    
    public Config(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    public String getVchBaseUri() {
        return "http://" + prefs.getString(Settings.VCH_HOST, "192.168.0.1") + ":"+ prefs.getString(Settings.VCH_PORT, "8080");
    }
    
    public String getVchParserUri() {
        return getVchBaseUri() + "/parser";
    }
    
    public String getVchPlaylistUri() {
        return getVchBaseUri() + "/playlist";
    }
    
    public String getVchDownloadsUri() {
        return getVchBaseUri() + "/downloads";
    }
}
