package com.mb.android.webviews;

import android.content.Context;

import com.mb.android.MainActivity;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.PluginEntry;
import org.crosswalk.engine.XWalkWebViewEngine;

/**
 * Created by Luke on 11/5/2015.
 */
public class MyXWalkWebViewEngine extends XWalkWebViewEngine {

    private MainActivity activity;
    /**
     * Used when created via reflection.
     *  @param context
     * @param preferences
     * @param activity
     */
    public MyXWalkWebViewEngine(Context context, CordovaPreferences preferences, MainActivity activity) {
        super(context, preferences);
        this.activity = activity;
    }

    @Override
    public void loadUrl(String url, boolean clearNavigationStack) {
        if (!activityDelegate.isXWalkReady()) {
            startUrl = url;

            CordovaPlugin initPlugin = new CordovaPlugin() {
                @Override
                public void onResume(boolean multitasking) {
                    activityDelegate.onResume();
                }
            };
            pluginManager.addService(new PluginEntry("XWalkInit", initPlugin));
            return;
        }

        AddJavascriptInterfaces();
        webView.load(url, null);
    }

    private void AddJavascriptInterfaces(){
        activity.addJavascriptInterfaces();
    }
}
