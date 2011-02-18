package de.berlios.vch.android;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

public class Settings extends PreferenceActivity {

    private static final String TAG = "BrowseActivity " + Settings.class.getSimpleName();
    
    public static final String VCH_HOST = "vch_host";
    public static final String VCH_PORT = "vch_port";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPreferenceScreen(createPreferenceHierarchy());
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                Log.d(TAG, "Changing connection details");
            }
        });
    }

    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        
        // vch preferences 
        PreferenceCategory vchCat = new PreferenceCategory(this);
        vchCat.setTitle(R.string.vch);
        root.addPreference(vchCat);
        {
            // host preference
            EditTextPreference vchHost = new EditTextPreference(this);
            vchHost.setDialogTitle(R.string.dialog_title_vch_host);
            vchHost.setKey(VCH_HOST);
            vchHost.setTitle(R.string.title_vch_host);
            vchHost.setSummary(R.string.summary_vch_host);
            vchHost.setDefaultValue("192.168.0.1");
            vchCat.addPreference(vchHost);
            
            // port preference
            EditTextPreference vchPort = new EditTextPreference(this);
            vchPort.setDialogTitle(R.string.dialog_title_vch_port);
            vchPort.setKey(VCH_PORT);
            vchPort.setTitle(R.string.title_vch_port);
            vchPort.setSummary(R.string.summary_vch_port);
            vchPort.setDefaultValue("8080");
            vchCat.addPreference(vchPort);
        }
        
        return root;
    }
}
