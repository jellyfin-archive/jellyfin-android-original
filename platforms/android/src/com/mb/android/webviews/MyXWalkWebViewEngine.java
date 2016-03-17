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
    public void loadUrlOnReady(String url) {
        AddJavascriptInterfaces();
        webView.load(url, null);
    }

    private void AddJavascriptInterfaces(){
        activity.addJavascriptInterfaces();
    }
}
