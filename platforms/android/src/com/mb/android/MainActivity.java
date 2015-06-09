/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.mb.android;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import com.mb.android.api.ApiClientBridge;
import com.mb.android.iap.IapManager;
import com.mb.android.io.NativeFileSystem;
import com.mb.android.media.Constants;
import com.mb.android.media.MediaPlayerService;
import com.mb.android.preferences.PreferencesProvider;
import com.mb.android.webviews.CrosswalkWebView;
import com.mb.android.webviews.IWebView;
import com.mb.android.webviews.NativeWebView;

import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaWebViewEngine;
import org.crosswalk.engine.XWalkCordovaView;
import org.xwalk.core.JavascriptInterface;

import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.logging.ConsoleLogger;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;
import tv.emby.iap.UnlockActivity;

public class MainActivity extends CordovaActivity
{
    private final int PURCHASE_UNLOCK_REQUEST = 999;
    private ILogger logger;
    private static IWebView webView;

    private ILogger getLogger(){
        if (logger == null){
            //logger = AppLogger.createLogger(this);
            logger = new ConsoleLogger();
        }

        return logger;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        try {
            // This is throwing an exception we can't catch and is crashing the app
            // URL.setURLStreamHandlerFactory(new OkUrlFactory(okHttpClient));
        }
        catch (Exception ex){
            // Occasionally seeing factory already set error
        }

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    @Override
    protected CordovaWebViewEngine makeWebViewEngine() {

        CordovaWebViewEngine engine =  super.makeWebViewEngine();

        View engineView = engine.getView();
        ILogger logger = getLogger();
        IJsonSerializer jsonSerializer = new GsonJsonSerializer();

        webView = null;

        if (engineView instanceof WebView){

            WebView webkitView = (WebView)engine.getView();
            webView = new NativeWebView(webkitView);
        }
        else{

            XWalkCordovaView xView = (XWalkCordovaView)engine.getView();
            webView = new CrosswalkWebView(xView);
        }

        Context context = getApplicationContext();

        webView.addJavascriptInterface(new IapManager(context, webView, logger), "NativeIapManager");
        webView.addJavascriptInterface(new ApiClientBridge(context, logger, webView, jsonSerializer), "ApiClientBridge");
        webView.addJavascriptInterface(new NativeFileSystem(logger), "NativeFileSystem");
        webView.addJavascriptInterface(this, "MainActivity");
        webView.addJavascriptInterface(new PreferencesProvider(context, logger), "AndroidSharedPreferences");

        return engine;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == PURCHASE_UNLOCK_REQUEST) {
            if (resultCode == RESULT_OK) {
                RespondToWebView(String.format("window.IapManager.onPurchaseComplete(true);"));
            } else {
                RespondToWebView(String.format("window.IapManager.onPurchaseComplete(false);"));
            }
        }
    }

    @JavascriptInterface
    public void beginPurchase(String id) {

        try {
            Intent purchaseIntent = new Intent(this, UnlockActivity.class);
            purchaseIntent.putExtra("googleKey", IapManager.GOOGLE_KEY);
            purchaseIntent.putExtra("sku", "com.mb.android.unlock");
            startActivityForResult(purchaseIntent, PURCHASE_UNLOCK_REQUEST);
        }
        catch (Exception ex) {
            logger.ErrorException("Error launching activity", ex);
            RespondToWebView(String.format("window.IapManager.onPurchaseComplete(false);"));
        }
    }

    public static void RespondToWebView(final String js) {

        //logger.Info("Sending url to webView: %s", js);
        webView.sendJavaScript(js);
    }

    @JavascriptInterface
    public void hideMediaSession() {

        //Intent i = new Intent("com.android.music.playstatechanged");
        //i.putExtra("id", "1");
        //i.putExtra("playing", "false");
        //context.sendBroadcast(i);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent( this, MediaPlayerService.class );
            intent.setAction( Constants.ACTION_REPORT );

            intent.putExtra("playerAction", "playbackstop");

            startService( intent );
        }
    }

    @JavascriptInterface
    public void updateMediaSession(String action, String title, String artist, String album, int duration, int position, String imageUrl, boolean canSeek, boolean isPaused) {

        //bluetoothNotifyChange(action, title, artist, album, duration, position, imageUrl, canSeek, isPaused);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent( this, MediaPlayerService.class );
            intent.setAction( Constants.ACTION_REPORT );

            intent.putExtra("playerAction", action);
            intent.putExtra("title", title);
            intent.putExtra("artist", artist);
            intent.putExtra("album", album);
            intent.putExtra("duration", duration);
            intent.putExtra("position", position);
            intent.putExtra("imageUrl", imageUrl);
            intent.putExtra("canSeek", canSeek);
            intent.putExtra("isPaused", isPaused);

            startService( intent );
        }
    }

    private void bluetoothNotifyChange(String action, String title, String artist, String album, long duration, long position, String imageUrl, boolean canSeek, boolean isPaused) {

        String intentName = action.equalsIgnoreCase("playbackstart") ?
                "com.android.music.metachanged" :
                "com.android.music.playstatechanged";

        Intent i = new Intent(intentName);
        i.putExtra("id", 1L);
        i.putExtra("artist", artist);
        i.putExtra("album",album);
        i.putExtra("track", title);
        i.putExtra("playing", !isPaused);
        i.putExtra("duration", duration);
        i.putExtra("position", position);
        //i.putExtra("ListSize", getQueue());
        sendBroadcast(i);
    }
}
