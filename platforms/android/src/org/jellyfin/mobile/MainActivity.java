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

package org.jellyfin.mobile;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.WebView;

import com.mb.android.api.ApiClientBridge;
import com.mb.android.media.RemotePlayerService;

import org.apache.cordova.BuildConfig;
import org.apache.cordova.CordovaActivity;
import com.mb.android.webviews.IWebView;
import com.mb.android.webviews.NativeWebView;

import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.engine.SystemWebView;
import org.apache.cordova.engine.SystemWebViewEngine;

import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.mediabrowser.Constants;
import mediabrowser.model.serialization.IJsonSerializer;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
import static android.view.View.SYSTEM_UI_FLAG_VISIBLE;

public class MainActivity extends CordovaActivity {
    public static IWebView webView;
    public static ApiClientBridge apiClientBridge;
    public IJsonSerializer jsonSerializer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadUrl(launchUrl);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_SHOW_PLAYER);

        addJavascriptInterfaces();
    }

    @Override
    protected CordovaWebViewEngine makeWebViewEngine() {
        Context context = getApplicationContext();
        CordovaWebViewEngine engine;

        engine =  new SystemWebViewEngine(new SystemWebView(this), preferences);
        WebView webkitView = (WebView) engine.getView();
        webkitView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView = new NativeWebView(webkitView);

        jsonSerializer = new GsonJsonSerializer();
        apiClientBridge = new ApiClientBridge(context, jsonSerializer);

        return engine;
    }

    public void addJavascriptInterfaces(){
        webView.addJavascriptInterface(apiClientBridge, "ApiClientBridge");
        webView.addJavascriptInterface(this, "MainActivity");
    }

    @android.webkit.JavascriptInterface
    public String getAppVersion() {
        return Integer.toString(BuildConfig.VERSION_CODE);
    }

    @android.webkit.JavascriptInterface
    public String getDeviceModel() {
        return android.os.Build.MODEL;
    }

    @android.webkit.JavascriptInterface
    public String getDeviceId() {
        // replace this later with a randomly generated persistent string stored in local settings
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @android.webkit.JavascriptInterface
    public void enableFullscreen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int visibility = SYSTEM_UI_FLAG_FULLSCREEN
                        | SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                getWindow().getDecorView().setSystemUiVisibility(visibility);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            }
        });
    }

    @android.webkit.JavascriptInterface
    public void disableFullscreen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getWindow().getDecorView().setSystemUiVisibility(SYSTEM_UI_FLAG_VISIBLE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        });
    }

    public static void RespondToWebView(final String js) {
        if (webView != null){
            webView.sendJavaScript(js);
        }
    }

    public static void sendCommand(String cmd) {
        String script = "require(['inputmanager'], function(inputmanager){inputmanager.trigger('" + cmd + "');});";
        RespondToWebView(script);
    }

    @android.webkit.JavascriptInterface
    public void hideMediaSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(this, RemotePlayerService.class);
            intent.setAction(Constants.ACTION_REPORT);
            intent.putExtra("playerAction", "playbackstop");
            startService(intent);
        }
    }

    @android.webkit.JavascriptInterface
    public void updateMediaSession(String action, boolean isLocalPlayer, String itemId, String title, String artist, String album, int duration, int position, String imageUrl, boolean canSeek, boolean isPaused) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.i(this.getClass().toString(), String.format("updateMediaSession isPaused: %s", isPaused));

            Intent intent = new Intent(this, RemotePlayerService.class);
            intent.setAction(Constants.ACTION_REPORT);
            intent.putExtra("playerAction", action);
            intent.putExtra("title", title);
            intent.putExtra("artist", artist);
            intent.putExtra("album", album);
            intent.putExtra("duration", duration);
            intent.putExtra("position", position);
            intent.putExtra("imageUrl", imageUrl);
            intent.putExtra("canSeek", canSeek);
            intent.putExtra("isPaused", isPaused);
            intent.putExtra("itemId", itemId);
            intent.putExtra("isLocalPlayer", isLocalPlayer);
            startService(intent);
        }
    }

    @android.webkit.JavascriptInterface
    public boolean enableVlcPlayer() {
        return false;
    }
}
