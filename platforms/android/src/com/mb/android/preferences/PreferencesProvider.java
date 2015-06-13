package com.mb.android.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.xwalk.core.JavascriptInterface;

import mediabrowser.apiinteraction.android.sync.MediaSyncAdapter;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 6/8/2015.
 */
public class PreferencesProvider {

    private Context context;
    private ILogger logger;

    public PreferencesProvider(Context context, ILogger logger) {
        this.context = context;
        this.logger = logger;

        updateSyncPreferences();
    }

    @JavascriptInterface
    public String get(String key) {


        return getSharedPreferences(context).getString(key, null);
    }

    @JavascriptInterface
    public void remove(String key) {

        set(key, null);
    }

    @JavascriptInterface
    public void set(String key, String value){

        SharedPreferences settings = getSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(key, value);

        // Commit the edits!
        boolean saved = editor.commit();

        if (!saved){
            logger.Error("SharedPreferences.Editor failed to save %s!", key);
        }

        if (key.equalsIgnoreCase("syncPath")){
            updateSyncPreferences();
        }
    }

    private void updateSyncPreferences() {

        // Need to take the app settings and copy them to where the sync services will read them
        String syncPath = get("syncPath");

        MediaSyncAdapter.updateSyncPreferences(context, syncPath);
    }

    private static SharedPreferences getSharedPreferences(Context context) {

        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
