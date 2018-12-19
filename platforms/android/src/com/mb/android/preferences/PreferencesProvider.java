package com.mb.android.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import mediabrowser.apiinteraction.android.sync.MediaSyncAdapter;
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

    @android.webkit.JavascriptInterface
    public String get(String key) {
        return getSharedPreferences(context).getString(key, null);
    }

    @android.webkit.JavascriptInterface
    public void remove(String key) {
        set(key, null);
    }

    @android.webkit.JavascriptInterface
    public void set(String key, String value){

        SharedPreferences settings = getSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(key, value);

        // Commit the edits!
        boolean saved = editor.commit();

        if (!saved){
            logger.Error("SharedPreferences.Editor failed to save %s!", key);
        }

        if (key.equalsIgnoreCase("syncPath") || key.equalsIgnoreCase("syncOnlyOnWifi") || key.equalsIgnoreCase("cameraUploadServers")){
            updateSyncPreferences();
        }
    }

    private void updateSyncPreferences() {

        // Need to take the app settings and copy them to where the sync services will read them
        String syncPath = getSharedPreferences(context).getString("syncPath", null);

        String syncOnOnWifiSetting = getSharedPreferences(context).getString("syncOnlyOnWifi", "true");
        boolean syncOnOnWifi = !syncOnOnWifiSetting.equalsIgnoreCase("false");

        String cameraUploadServersString = getSharedPreferences(context).getString("cameraUploadServers", "");
        String[] cameraUploadServers = cameraUploadServersString == null || cameraUploadServersString.length() == 0 ? new String[]{} : cameraUploadServersString.split(",");

        logger.Debug("Calling MediaSyncAdapter.updateSyncPreferences. syncPath: %s", syncPath);
        MediaSyncAdapter.updateSyncPreferences(context, syncPath, syncOnOnWifi, cameraUploadServers);
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
