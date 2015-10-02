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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;

import com.mb.android.api.ApiClientBridge;
import com.mb.android.iap.IapManager;
import com.mb.android.io.NativeFileSystem;
import com.mb.android.logging.AppLogger;
import com.mb.android.logging.LoggingBridge;
import com.mb.android.media.MediaService;
import com.mb.android.media.VideoPlayerActivity;
import com.mb.android.media.legacy.KitKatMediaService;
import com.mb.android.media.RemotePlayerService;
import com.mb.android.preferences.PreferencesProvider;
import com.mb.android.webviews.CrosswalkWebView;
import com.mb.android.webviews.IWebView;
import com.mb.android.webviews.NativeWebView;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaWebViewEngine;
import org.crosswalk.engine.XWalkCordovaView;
import org.crosswalk.engine.XWalkWebViewEngine;
import org.xwalk.core.JavascriptInterface;

import java.util.List;

import mediabrowser.apiinteraction.QueryStringDictionary;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.mediabrowser.Constants;
import mediabrowser.apiinteraction.android.sync.MediaSyncAdapter;
import mediabrowser.apiinteraction.android.sync.OnDemandSync;
import mediabrowser.apiinteraction.http.HttpRequest;
import mediabrowser.apiinteraction.http.IAsyncHttpClient;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;
import tv.emby.iap.InAppProduct;
import tv.emby.iap.UnlockActivity;

public class MainActivity extends CordovaActivity
{
    private final int PURCHASE_REQUEST = 999;
    private final int REQUEST_DIRECTORY = 998;
    public static final int VIDEO_PLAYBACK = 997;
    private final String embyAdminUrl = "http://mb3admin.com/test/admin/service/";
    private static IWebView webView;
    private IAsyncHttpClient httpClient;
    private IJsonSerializer jsonSerializer;
    private IapManager iapManager;

    private String purchaseEmail;

    private ILogger getLogger(){
        return AppLogger.getLogger(this);
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
            // This is causing a crash on some devices and is not needed anyway when using Crosswalk
            //WebView.setWebContentsDebuggingEnabled(true);
        }

        /* Prepare the progressBar */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_SHOW_PLAYER);
        registerReceiver(messageReceiver, filter);
    }

    @Override
    protected CordovaWebViewEngine makeWebViewEngine() {

        CordovaWebViewEngine engine =  new XWalkWebViewEngine(this, preferences);

        View engineView = engine.getView();
        final ILogger logger = getLogger();
        jsonSerializer = new GsonJsonSerializer();

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

        iapManager = new IapManager(context, webView, logger);
        webView.addJavascriptInterface(iapManager, "NativeIapManager");
        ApiClientBridge apiClientBridge = new ApiClientBridge(context, logger, webView, jsonSerializer);
        webView.addJavascriptInterface(apiClientBridge, "ApiClientBridge");
        httpClient = apiClientBridge.httpClient;
        webView.addJavascriptInterface(new NativeFileSystem(logger), "NativeFileSystem");
        webView.addJavascriptInterface(this, "MainActivity");
        webView.addJavascriptInterface(this, "AndroidDirectoryChooser");
        webView.addJavascriptInterface(this, "AndroidVlcPlayer");
        webView.addJavascriptInterface(this, "AndroidSync");
        webView.addJavascriptInterface(new LoggingBridge(getLogger()), "LoggingBridge");

        PreferencesProvider preferencesProvider = new PreferencesProvider(context, logger);

        webView.addJavascriptInterface(preferencesProvider, "AndroidSharedPreferences");

        return engine;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == PURCHASE_REQUEST) {
            if (resultCode == RESULT_OK) {
                InAppProduct product = jsonSerializer.DeserializeFromString(intent.getStringExtra("product"), InAppProduct.class);
                if (product.getEmbyFeatureCode() != null) {
                    //Need to register this with the back end
                    HttpRequest request = new HttpRequest();
                    request.setUrl(embyAdminUrl + "appstore/register");
                    request.setMethod("POST");
                    QueryStringDictionary data = new QueryStringDictionary();
                    data.Add("store", intent.getStringExtra("store"));
                    data.Add("application", "com.mb.android");
                    data.Add("product", product.getSku());
                    data.Add("type", product.getProductType().toString());
                    data.Add("storeId", intent.getStringExtra("storeId"));
                    data.Add("token", intent.getStringExtra("storeToken"));
                    data.Add("feature", product.getEmbyFeatureCode());
                    data.Add("email", purchaseEmail);
                    data.Add("amt", product.getPrice());
                    request.setPostData(data);

                    httpClient.Send(request, new Response<String>() {
                        @Override
                        public void onResponse(String response) {
                            //The response will be a json representation of regRecord e.g.
                            //{
                            //    "featId": "MBSupporter",
                            //    "expDate": "2015-10-09",
                            //    "key": "cce907328832cea7a09dd49d197c93bd"
                            //}

                            // call the API to fill in the key and then re-fresh the supporter status in this app
                            RespondToWebView(String.format("window.IapManager.onPurchaseComplete(true);"));
                        }

                        @Override
                        public void onError(Exception exception) {
                            //This would be really bad.  We need to record all the information about this purchase somewhere
                            // so we can try and rectify it
                            RespondToWebView(String.format("window.IapManager.onPurchaseComplete(false);"));
                        }
                    });
                } else {
                    // no emby feature - just report success
                    RespondToWebView(String.format("window.IapManager.onPurchaseComplete(true);"));
                }
            } else {
                RespondToWebView(String.format("window.IapManager.onPurchaseComplete(false);"));
            }
        }

        else if (requestCode == REQUEST_DIRECTORY) {

            if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {

                String path = intent.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);

                RespondToWebView(String.format("window.NativeDirectoryChooser.onChosen('%s');", path));
            }
            else{
                RespondToWebView("window.NativeDirectoryChooser.onChosen(null);");
            }
        }

        else if (requestCode == VIDEO_PLAYBACK) {

            /*boolean completed = resultCode == RESULT_OK;
            boolean error = resultCode == RESULT_OK ? false : (intent == null ? true : intent.getBooleanExtra("error", false));

            long positionMs = intent == null || completed ? 0 : intent.getLongExtra("position", 0);
            String currentSrc = intent == null ? null : intent.getStringExtra(VideoPlayerActivity.PLAY_EXTRA_ITEM_LOCATION);

            if (currentSrc == null) {
                currentSrc = "";
            }

            RespondToWebView(String.format("VideoRenderer.Current.onActivityClosed(%s, %s, %s, '%s');", !completed, error, positionMs, currentSrc));*/
        }
    }

    @org.xwalk.core.JavascriptInterface
    public void beginPurchase(final String id, final String purchaseEmail) {

        this.purchaseEmail = purchaseEmail;

        iapManager.getAvailableProducts(jsonSerializer, new Response<List<InAppProduct>>(){

            @Override
            public void onResponse(List<InAppProduct> inAppProducts) {

                InAppProduct product = null;
                for (InAppProduct prd : inAppProducts){
                    if (StringHelper.EqualsIgnoreCase(id, prd.getSku())){
                        product = prd;
                        break;
                    }
                }

                final String productJson = jsonSerializer.SerializeToString(product);

                if (product.requiresEmail() && (purchaseEmail == null || purchaseEmail.length() == 0)) {
                    //Todo Obtain the email address for purchase - then re-call this method
                    return;
                }

                if (product.getEmbyFeatureCode() != null) {
                    //Test connectivity to our back-end before purchase because we need this to complete it
                    HttpRequest request = new HttpRequest();
                    request.setUrl(embyAdminUrl+"appstore/check");
                    httpClient.Send(request, new Response<String>() {
                        @Override
                        public void onResponse(String response) {
                            //ok, continue with purchase
                            purchaseInternal(productJson);
                        }

                        @Override
                        public void onError(Exception exception) {
                            //Unable to connect - display appropriate message
                        }
                    });
                } else {
                    //Just initiate the purchase
                    purchaseInternal(productJson);
                }
            }
        });
    }

    private void purchaseInternal(String productJson) {
        try {
            Intent purchaseIntent = new Intent(this, UnlockActivity.class);
            purchaseIntent.putExtra("googleKey", IapManager.GOOGLE_KEY);
            purchaseIntent.putExtra("product", productJson);
            startActivityForResult(purchaseIntent, PURCHASE_REQUEST);
        }
        catch (Exception ex) {
            getLogger().ErrorException("Error launching activity", ex);
            RespondToWebView(String.format("window.IapManager.onPurchaseComplete(false);"));
        }

    }

    public static void RespondToWebView(final String js) {

        //logger.Info("Sending url to webView: %s", js);
        if (webView != null){
            webView.sendJavaScript(js);
        }
    }

    @JavascriptInterface
    public void hideMediaSession() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent( this, RemotePlayerService.class );
            intent.setAction( Constants.ACTION_REPORT );

            intent.putExtra("playerAction", "playbackstop");

            startService( intent );
        }
    }

    @JavascriptInterface
    public void updateMediaSession(String action, boolean isLocalPlayer, String itemId, String title, String artist, String album, int duration, int position, String imageUrl, boolean canSeek, boolean isPaused) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent( this, RemotePlayerService.class );
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
            intent.putExtra("itemId", itemId);
            intent.putExtra("isLocalPlayer", isLocalPlayer);

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

    @JavascriptInterface
    public void chooseDirectory() {

        final Intent chooserIntent = new Intent(this, DirectoryChooserActivity.class);

        final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .newDirectoryName("EmbySync")
                .allowReadOnlyDirectory(false)
                .allowNewDirectoryNameModification(true)
                .build();

        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config);

        // REQUEST_DIRECTORY is a constant integer to identify the request, e.g. 0
        startActivityForResult(chooserIntent, REQUEST_DIRECTORY);
    }

    @JavascriptInterface
    public void playAudioVlc(String path, String itemJson, String mediaSourceJson, String posterUrl) {

        Intent intent = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            intent = new Intent( this, MediaService.class );
        }
        else {
            intent = new Intent( this, KitKatMediaService.class );
        }

        intent.setAction( Constants.ACTION_PLAY );
        intent.putExtra("path", path);
        intent.putExtra("item", itemJson);
        intent.putExtra("mediaSource", mediaSourceJson);
        intent.putExtra("posterUrl", posterUrl);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        startService( intent );
    }

    @JavascriptInterface
    public void playVideoVlc(String path,
                             long startPositionMs,
                             String itemName,
                             String itemJson,
                             String mediaSourceJson,
                             String playbackStartInfoJson,
                             String serverId,
                             String serverUrl,
                             String appName,
                             String appVersion,
                             String deviceId,
                             String deviceName,
                             String userId,
                             String accessToken,
                             String deviceProfileJson,
                             String videoQualityOptionsJson) {

        getLogger().Debug("Video path: %s", path);
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.setAction(VideoPlayerActivity.PLAY_FROM_VIDEOGRID);
        intent.putExtra(VideoPlayerActivity.PLAY_EXTRA_ITEM_LOCATION, path);
        intent.putExtra(VideoPlayerActivity.PLAY_EXTRA_ITEM_TITLE, itemName);
        //intent.putExtra(VideoPlayerActivity.PLAY_EXTRA_OPENED_POSITION, 0);
        //intent.putExtra("item", itemJson);
        intent.putExtra("mediaSourceJson", mediaSourceJson);
        intent.putExtra("playbackStartInfoJson", playbackStartInfoJson);
        intent.putExtra("serverId", serverId);
        intent.putExtra("serverUrl", serverUrl);
        intent.putExtra("appName", appName);
        intent.putExtra("appVersion", appVersion);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("deviceName", deviceName);
        intent.putExtra("userId", userId);
        intent.putExtra("accessToken", accessToken);
        intent.putExtra("deviceProfileJson", deviceProfileJson);
        intent.putExtra("videoQualityOptionsJson", videoQualityOptionsJson);

        if (startPositionMs > 0){
            intent.putExtra("position", startPositionMs);
        }

        startActivityForResult(intent, VIDEO_PLAYBACK);
    }

    @JavascriptInterface
    public void destroyVlc() {

        Intent intent = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            intent = new Intent( this, MediaService.class );
        }
        else {
            intent = new Intent( this, KitKatMediaService.class );
        }

        intent.setAction( Constants.ACTION_STOP );
        startService( intent );
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @JavascriptInterface
    public void sendVlcCommand(String name, String arg1) {

        getLogger().Debug("Vlc received command: %s", name);

        Intent intent = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            intent = new Intent( this, MediaService.class );
        }
        else {
            intent = new Intent( this, KitKatMediaService.class );
        }

        try {
            if (name.equalsIgnoreCase("pause")){

                intent.setAction( Constants.ACTION_PAUSE );
                startService( intent );
            }
            else if (name.equalsIgnoreCase("unpause")){
                intent.setAction( Constants.ACTION_UNPAUSE );
                startService( intent );
            }
            else if (name.equalsIgnoreCase("stop")){

                intent.setAction( Constants.ACTION_STOP );
                boolean stopService = StringHelper.EqualsIgnoreCase(arg1, "true");
                intent.putExtra("stopService", stopService);
                startService( intent );
            }
            else if (name.equalsIgnoreCase("setvolume")){

                // incoming value is 0-100

                float val = Float.parseFloat(arg1);
                val = Math.min(val, 100);
                val = Math.max(val, 0);

                //mLibVLC.setVolume(Math.round(val));
            }
            else if (name.equalsIgnoreCase("setposition")){

                // incoming value is ms

                intent.setAction( Constants.ACTION_SEEK );
                getLogger().Debug("Sending seek command to Vlc Service. Position: %s", arg1);
                try {
                    float newPosition = Float.parseFloat(arg1);
                    long roundedPosition = Math.round(newPosition);

                    intent.putExtra("position", roundedPosition);
                    startService( intent );
                }
                catch (NumberFormatException ex){
                    getLogger().ErrorException("Error parsing seek value", ex);
                }
            }
        }
        catch (Exception ex){
            getLogger().ErrorException("Error sending command %s to Vlc", ex, name);
        }
    }

    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equalsIgnoreCase(Constants.ACTION_SHOW_PLAYER)) {
//                showAudioPlayer();
            }
        }
    };

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            RespondToWebView("LibraryMenu.onHardwareMenuButtonClick();");
            return true;
        } else {
            return super.onKeyUp(keyCode, event);
        }
    }

    @JavascriptInterface
    public String getSyncStatus() {
        return MediaSyncAdapter.isSyncActive() ? "Active" : MediaSyncAdapter.isSyncPending() ? "Pending" : "Idle";
    }

    @JavascriptInterface
    public void startSync() {
        new OnDemandSync(getApplicationContext()).Run();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            unregisterReceiver(messageReceiver);
        } catch (IllegalArgumentException e) {}
    }
}
