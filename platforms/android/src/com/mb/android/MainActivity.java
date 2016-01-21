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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
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
import com.mb.android.webviews.MySystemWebView;
import com.mb.android.webviews.MyXWalkWebViewEngine;
import com.mb.android.webviews.NativeWebView;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.engine.SystemWebViewEngine;
import org.crosswalk.engine.XWalkCordovaView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.mediabrowser.Constants;
import mediabrowser.apiinteraction.android.sync.MediaSyncAdapter;
import mediabrowser.apiinteraction.android.sync.OnDemandSync;
import mediabrowser.apiinteraction.http.HttpRequest;
import mediabrowser.apiinteraction.http.IAsyncHttpClient;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.registration.AppstoreRegRequest;
import mediabrowser.model.serialization.IJsonSerializer;
import tv.emby.iap.InAppProduct;
import tv.emby.iap.PurchaseActivity;

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
    private InAppProduct currentProduct;

    public static String AppPackageName = "com.mb.android";

    private ILogger getLogger(){
        return AppLogger.getLogger(this);
    }

    private int chromeVersion = 45;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        /*new Runnable() {
            @Override
            public void run() {
                readLogcatInBackground();
            }
        }.run();*/

        /*try {
            // This is throwing an exception we can't catch and is crashing the app
             URL.setURLStreamHandlerFactory(new OkUrlFactory(okHttpClient));
        }
        catch (Exception ex){
            // Occasionally seeing factory already set error
        }*/

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);

        /* Prepare the progressBar */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_SHOW_PLAYER);
        registerReceiver(messageReceiver, filter);

        if (enableSystemWebView()){

            /*try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    // This is causing a crash on some devices
                    WebView.setWebContentsDebuggingEnabled(true);
                }
            }
            catch (Exception ex) {
                // This is causing a crash on some devices
                getLogger().ErrorException("Error enabling webview debugging", ex);
            }*/
            addJavascriptInterfaces();
        }
    }

    private boolean enableSystemWebView(){

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){

            getLogger().Info("Enabling Crosswalk due to older android version");
            return false;
        }

        // Use system version if equal or higher.
        int systemWebViewVersion = GetSystemWebViewChromiumVersion();
        if (systemWebViewVersion < chromeVersion) {
            getLogger().Info("Enabling Crosswalk due to older chromium version");
            return false;
        }

        chromeVersion = systemWebViewVersion;

        return true;
    }

    private int GetSystemWebViewChromiumVersion() {

        try {

            getLogger().Info("Searching for com.google.android.webview");

            PackageManager pm = getPackageManager();

            PackageInfo pi = pm.getPackageInfo("com.google.android.webview", 0);

            getLogger().Info("com.google.android.webview version name: " + pi.versionName);
            getLogger().Info("com.google.android.webview version code: " + pi.versionCode);

            String parseString = pi.versionName.split(Pattern.quote("."))[0];

            getLogger().Info("Parsing %s to determine chromium version", parseString);

            int version = Integer.parseInt(parseString);

            getLogger().Info("Chromium version: " + version);

            return version;

        } catch (PackageManager.NameNotFoundException e) {
            getLogger().ErrorException("Android System WebView is not found", e);
            return 0;
        }catch (Exception e) {
            getLogger().ErrorException("Android System WebView is not found", e);
            return 0;
        }
    }


    @Override
    protected CordovaWebViewEngine makeWebViewEngine() {

        Context context = getApplicationContext();
        CordovaWebViewEngine engine;

        if (enableSystemWebView()){

            engine =  new SystemWebViewEngine(new MySystemWebView(this, getLogger()), preferences);
            WebView webkitView = (WebView)engine.getView();
            webView = new NativeWebView(webkitView);

        } else {
            engine =  new MyXWalkWebViewEngine(this, preferences, this);
            XWalkCordovaView xView = (XWalkCordovaView)engine.getView();
            webView = new CrosswalkWebView(xView);
        }

        final ILogger logger = getLogger();
        jsonSerializer = new GsonJsonSerializer();

        iapManager = new IapManager(context, webView, logger);
        ApiClientBridge apiClientBridge = new ApiClientBridge(context, logger, webView, jsonSerializer);
        httpClient = apiClientBridge.httpClient;

        return engine;
    }

    public void addJavascriptInterfaces(){

        Context context = getApplicationContext();
        final ILogger logger = getLogger();

        webView.addJavascriptInterface(iapManager, "NativeIapManager");
        webView.addJavascriptInterface(ApiClientBridge.Current, "ApiClientBridge");
        webView.addJavascriptInterface(new NativeFileSystem(logger), "NativeFileSystem");
        webView.addJavascriptInterface(this, "MainActivity");
        webView.addJavascriptInterface(this, "AndroidDirectoryChooser");
        webView.addJavascriptInterface(this, "AndroidVlcPlayer");
        webView.addJavascriptInterface(this, "AndroidSync");
        webView.addJavascriptInterface(new LoggingBridge(getLogger()), "LoggingBridge");

        PreferencesProvider preferencesProvider = new PreferencesProvider(context, logger);

        webView.addJavascriptInterface(preferencesProvider, "AndroidSharedPreferences");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == PURCHASE_REQUEST) {
            if (resultCode == RESULT_OK) {

                if (currentProduct.getEmbyFeatureCode() != null) {

                    AppstoreRegRequest request = new AppstoreRegRequest();
                    request.setStore(intent.getStringExtra("store"));
                    request.setApplication(AppPackageName);
                    request.setProduct(currentProduct.getSku());
                    request.setFeature(currentProduct.getEmbyFeatureCode());
                    request.setType(currentProduct.getProductType().toString());
                    if (intent.getStringExtra("storeId") != null) request.setStoreId(intent.getStringExtra("storeId"));
                    request.setStoreToken(intent.getStringExtra("storeToken"));
                    request.setEmail(purchaseEmail);
                    request.setAmt(currentProduct.getPrice());

                    RespondToWebView(String.format("window.IapManager.onPurchaseComplete("+jsonSerializer.SerializeToString(request)+");"));
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

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public int getChromeVersion() {
        return chromeVersion;
    }

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void purchasePremiereMonthly(final String email) {
        if (iapManager.isStoreAvailable()) {
            beginPurchase(iapManager.getPremiereMonthly(), email);
        } else{
            getLogger().Error("Cannot proceed with purchasePremiereMonthly because store is not available");
        }
    }

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void purchasePremiereWeekly(final String email) {
        if (iapManager.isStoreAvailable()) {
            beginPurchase(iapManager.getPremiereWeekly(), email);
        } else{
            getLogger().Error("Cannot proceed with purchasePremiereWeekly because store is not available");
        }
    }

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void purchaseUnlock() {
        if (iapManager.isStoreAvailable()) {
            beginPurchase(iapManager.getUnlockProduct(), null);
        } else{
            getLogger().Error("Cannot proceed with purchaseUnlock because store is not available");
        }
    }

    public void beginPurchase(final InAppProduct product, final String purchaseEmail) {

        this.purchaseEmail = purchaseEmail;

            if (product.requiresEmail() && (purchaseEmail == null || purchaseEmail.length() == 0)) {
                //Todo Obtain the email address for purchase - then re-call this method
                getLogger().Error("Aborting beginPurchase because purchaseEmail is required.");
                return;
            }

            if (product.getEmbyFeatureCode() != null) {

                //Test connectivity to our back-end before purchase because we need this to complete it
                getLogger().Debug("Testing back-end connectivity.");

                HttpRequest request = new HttpRequest();
                request.setUrl(embyAdminUrl+"appstore/check");
                httpClient.Send(request, new Response<String>() {
                    @Override
                    public void onResponse(String response) {

                        getLogger().Debug("Back-end connectivity test succeeded");

                        //ok, continue with purchase
                        purchaseInternal(product);
                    }

                    @Override
                    public void onError(Exception exception) {

                        getLogger().Error("Back-end connectivity test failed.");
                        //Unable to connect - display appropriate message
                    }
                });
            } else {
                //Just initiate the purchase
                purchaseInternal(product);
            }
    }

    private void purchaseInternal(InAppProduct product) {

        try {

            getLogger().Debug("purchaseInternal sku: %s", product.getSku());

            currentProduct = product;
            Intent purchaseIntent = new Intent(this, PurchaseActivity.class);
            purchaseIntent.putExtra("googleKey", IapManager.GOOGLE_KEY);
            purchaseIntent.putExtra("sku", product.getSku());
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

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void hideMediaSession() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent( this, RemotePlayerService.class );
            intent.setAction( Constants.ACTION_REPORT );

            intent.putExtra("playerAction", "playbackstop");

            startService( intent );
        }
    }

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
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

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
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

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
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

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
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
                             String videoQualityOptionsJson,
                             long timeLimitMs) {

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

        if (timeLimitMs > 0){
            intent.putExtra("timeLimitMs", timeLimitMs);
        }

        startActivityForResult(intent, VIDEO_PLAYBACK);
    }

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
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

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
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

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public String getSyncStatus() {
        return MediaSyncAdapter.isSyncActive() ? "Active" : MediaSyncAdapter.isSyncPending() ? "Pending" : "Idle";
    }

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
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

    static final int BUFFER_SIZE = 2 * 4096;

    volatile boolean logcatReaderRunning = true;

    protected void readLogcatInBackground() {

        logcatReaderRunning = true;
        Process process = null;

        try {
            process = Runtime.getRuntime().exec("logcat");
        } catch (IOException e) {
            logcatReaderRunning = false;
            return;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()), BUFFER_SIZE);
        } catch (IllegalArgumentException e) {
            logcatReaderRunning = false;
        }
        try {
            while (logcatReaderRunning) {

                getLogger().Debug(reader.readLine());
            }
        } catch (IOException e) {
            logcatReaderRunning = false;
        }
    }

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public String getLegacyDeviceId() {

        Context context = getApplicationContext();
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        String uuid;
        String androidID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceID = tm.getDeviceId();
        String simID = tm.getSimSerialNumber();

        if ("9774d56d682e549c".equals(androidID) || androidID == null) {
            androidID = "";
        }

        if (deviceID == null) {
            deviceID = "";
        }

        if (simID == null) {
            simID = "";
        }

        uuid = androidID + deviceID + simID;
        uuid = String.format("%32s", uuid).replace(' ', '0');
        uuid = uuid.substring(0,32);
        uuid = uuid.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");

        return uuid;
    }

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public boolean supportsPlayStore(){

        // This determines how Chromecast will be supported
        // If play store services are available, we use the Google Cast SDK, which is the preferred method
        // If not, we use the LG Connect SDK
        return BuildConfig.FLAVOR.toLowerCase().indexOf("google") != -1;
    }

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public String getAndroidDeviceId() {

        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void sendEmail(String to, String subject, String body) {

        Intent draft     = getDraftWithProperties(to, subject, body);
        String header    = "Open with";

        final Intent chooser = Intent.createChooser(draft, header);

        startActivityForResult(chooser, 0);
    }

    public Intent getDraftWithProperties (String to, String subject, String body) {

        Intent mail = new Intent(Intent.ACTION_SEND_MULTIPLE);

        setSubject(subject, mail);
        setBody(body, false, mail);
        setRecipients(to, mail);

        mail.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return mail;
    }

    /**
     * Setter for the subject.
     *
     * @param subject
     * The subject of the email.
     * @param draft
     * The intent to send.
     */
    private void setSubject (String subject, Intent draft) {
        draft.putExtra(Intent.EXTRA_SUBJECT, subject);
    }

    /**
     * Setter for the body.
     *
     * @param body
     * The body of the email.
     * @param isHTML
     * Indicates the encoding (HTML or plain text).
     * @param draft
     * The intent to send.
     */
    private void setBody (String body, Boolean isHTML, Intent draft) {

        if (isHTML) {
            /*draft.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(body));
            draft.setType("text/html");

            if (Build.VERSION.SDK_INT > 15) {
                draft.putExtra(Intent.EXTRA_HTML_TEXT, body);
            }*/
        } else {
            draft.putExtra(Intent.EXTRA_TEXT, body);
            draft.setType("text/plain");
        }
    }

    private void setRecipients (String to, Intent draft) {

        String[] receivers = new String[1];

        receivers[0] = to;

        draft.putExtra(Intent.EXTRA_EMAIL, receivers);
    }
}
