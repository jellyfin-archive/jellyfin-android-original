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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.view.KeyEvent;
import android.webkit.WebView;

import com.mb.android.api.ApiClientBridge;
import com.mb.android.iap.IapManager;
import com.mb.android.io.NativeFileSystem;
import com.mb.android.logging.AppLogger;
import com.mb.android.media.MediaService;
import com.mb.android.media.VideoPlayerActivity;
import com.mb.android.media.legacy.KitKatMediaService;
import com.mb.android.media.RemotePlayerService;

import org.apache.cordova.BuildConfig;
import org.apache.cordova.CordovaActivity;
import com.mb.android.preferences.PreferencesProvider;
import com.mb.android.webviews.IWebView;
import com.mb.android.webviews.MySystemWebView;
import com.mb.android.webviews.NativeWebView;
import com.nononsenseapps.filepicker.FilePickerActivity;

import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.engine.SystemWebViewEngine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.mediabrowser.Constants;
import mediabrowser.apiinteraction.android.sync.MediaSyncAdapter;
import mediabrowser.apiinteraction.android.sync.OnDemandSync;
import mediabrowser.apiinteraction.discovery.ServerLocator;
import mediabrowser.model.apiclient.ServerDiscoveryInfo;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.logging.ILogger;

public class MainActivity extends CordovaActivity {
    private final int LAUNCH_REQUEST = 990;
    private final int REQUEST_DIRECTORY = 998;
    private final int REQUEST_DIRECTORY_SAF = 996;
    public static final int VIDEO_PLAYBACK = 997;
    public static final int SHARE_RESULT = 980;
    private static IWebView webView;
    private IapManager iapManager;

    private ILogger getLogger() {
        return AppLogger.getLogger(this);
    }

    public static MainActivity Current;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Current = this;
        loadUrl(launchUrl);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_SHOW_PLAYER);
        registerReceiver(messageReceiver, filter);

        addJavascriptInterfaces();
    }

    @Override
    protected CordovaWebViewEngine makeWebViewEngine() {
        Context context = getApplicationContext();
        CordovaWebViewEngine engine;

        final ILogger logger = getLogger();

        engine =  new SystemWebViewEngine(new MySystemWebView(this, logger, this), preferences);
        WebView webkitView = (WebView)engine.getView();
        webkitView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView = new NativeWebView(webkitView);
        iapManager = new IapManager(context, webView, logger);

        return engine;
    }

    public void addJavascriptInterfaces(){
        Context context = getApplicationContext();
        final ILogger logger = getLogger();

        webView.addJavascriptInterface(iapManager, "NativeIapManager");
        webView.addJavascriptInterface(ApiClientBridge.Current, "ApiClientBridge");
        webView.addJavascriptInterface(new NativeFileSystem(logger, context), "NativeFileSystem");
        webView.addJavascriptInterface(this, "MainActivity");
        webView.addJavascriptInterface(this, "AndroidDirectoryChooser");
        webView.addJavascriptInterface(this, "AndroidVlcPlayer");
        webView.addJavascriptInterface(this, "AndroidSync");

        PreferencesProvider preferencesProvider = new PreferencesProvider(context, logger);

        webView.addJavascriptInterface(preferencesProvider, "AndroidSharedPreferences");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_DIRECTORY_SAF && resultCode == Activity.RESULT_OK) {
            Uri uri = intent.getData();
            final int takeFlags = intent.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // Check for the freshest data.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
            }
            RespondToWebViewWithSelectedPath(uri);
        } else if (requestCode == REQUEST_DIRECTORY && resultCode == RESULT_OK) {
            if (intent.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                ClipData clip = intent.getClipData();
                if (clip != null) {
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        Uri uri = clip.getItemAt(i).getUri();
                        RespondToWebViewWithSelectedPath(uri);
                    }
                }
            } else {
                Uri uri = intent.getData();
                // Do something with the URI
                if (uri != null){
                    RespondToWebViewWithSelectedPath(uri);
                }
            }
        } else if (requestCode == VIDEO_PLAYBACK) {
            boolean completed = resultCode == RESULT_OK;
            boolean error = resultCode == RESULT_OK ? false : (intent == null ? true : intent.getBooleanExtra("error", false));
            long positionMs = intent == null || completed ? 0 : intent.getLongExtra("position", 0);
            String currentSrc = intent == null ? "" : intent.getStringExtra(VideoPlayerActivity.PLAY_EXTRA_ITEM_LOCATION);
            // add this once we find the right method to call
            //RespondToWebView(String.format("VideoRenderer.Current.onActivityClosed(%s, %s, %s, '%s');", !completed, error, positionMs, currentSrc));
        }
    }

    private void RespondToWebViewWithSelectedPath(Uri uri) {
        String path = uri.toString();
        String srch = "file://";
        if (StringHelper.IndexOfIgnoreCase(path, srch) == 0) {
                path = path.substring(srch.length());
        }
        RespondToWebView(String.format("window.NativeDirectoryChooser.onChosen('%s');", path));
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
            getLogger().Info("updateMediaSession isPaused: %s", isPaused);

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

    private final int ExternalStoragePermissionRequestCode = 3;
    private final int AuthorizeStoragePermissionRequestCode = 4;
    private final int DownloadFileRequestCode = 5;
    private String downloadFilePath;
    private String downloadFileUrl;

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case ExternalStoragePermissionRequestCode: {
                // if request is cancelled the result arrays are empty
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    chooseDirectory();
                }
                return;
            }
            case DownloadFileRequestCode: {
                // if request is cancelled the result arrays are empty
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    downloadFile(downloadFileUrl, downloadFilePath);
                }
            }
        }
    }

    @android.webkit.JavascriptInterface
    public boolean authorizeStorage(){
        getLogger().Info("begin authorizeStorage");
        return authorizeStorage(AuthorizeStoragePermissionRequestCode);
    }

    private boolean authorizeStorage(final int requestCode) {
        final Activity activity = this;
        getLogger().Info("authorizeStorage with requestCode %s", requestCode);

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            getLogger().Info("Permission for WRITE_EXTERNAL_STORAGE is not granted");
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                getLogger().Info("calling ActivityCompat.requestPermissions for WRITE_EXTERNAL_STORAGE");
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);

            } else {

                // No explanation needed, we can request the permission.

                getLogger().Info("calling ActivityCompat.requestPermissions for WRITE_EXTERNAL_STORAGE");
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
            getLogger().Info("authorizeStorage returning false");
            return false;
        }

        getLogger().Info("Permission for WRITE_EXTERNAL_STORAGE is granted");
        getLogger().Info("authorizeStorage returning true");
        return true;
    }

    @android.webkit.JavascriptInterface
    public void chooseDirectory() {
        getLogger().Info("begin chooseDirectory");
        if (!authorizeStorage(ExternalStoragePermissionRequestCode)){
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_DIRECTORY_SAF);
        } else {
            getLogger().Info("creating intent for FilePickerActivity");
            Intent intent = new Intent(this, FilePickerActivity.class);
            // This works if you defined the intent filter
            // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

            // Set these depending on your use case. These are the defaults.
            intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            intent.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
            intent.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);

            // Configure initial directory by specifying a String.
            // You could specify a String like "/storage/emulated/0/", but that can
            // dangerous. Always use Android's API calls to get paths to the SD-card or
            // internal memory.
            intent.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

            getLogger().Info("startActivityForResult for FilePickerActivity");
            startActivityForResult(intent, REQUEST_DIRECTORY);
        }
    }

    @android.webkit.JavascriptInterface
    public void playAudioVlc(String path, String itemJson, String mediaSourceJson, String posterUrl) {
        Intent intent = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent = new Intent(this, MediaService.class);
        } else {
            intent = new Intent(this, KitKatMediaService.class);
        }

        intent.setAction(Constants.ACTION_PLAY);
        intent.putExtra("path", path);
        intent.putExtra("item", itemJson);
        intent.putExtra("mediaSource", mediaSourceJson);
        intent.putExtra("posterUrl", posterUrl);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        startService(intent);
    }

    @android.webkit.JavascriptInterface
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
    public void destroyVlc() {
        Intent intent = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent = new Intent(this, MediaService.class);
        } else {
            intent = new Intent(this, KitKatMediaService.class);
        }

        intent.setAction(Constants.ACTION_STOP);
        startService(intent);
    }

    @android.webkit.JavascriptInterface
    public void sendVlcCommand(String name, String arg1) {
        getLogger().Debug("Vlc received command: %s", name);
        Intent intent = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent = new Intent( this, MediaService.class );
        } else {
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
            else if (name.equalsIgnoreCase("playpause")){
                intent.setAction( Constants.ACTION_PLAYPAUSE );
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
                //showAudioPlayer();
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
    public String getSyncStatus() {
        return MediaSyncAdapter.isSyncActive() ? "Active" : MediaSyncAdapter.isSyncPending() ? "Pending" : "Idle";
    }

    @android.webkit.JavascriptInterface
    public void startSync() {
        new OnDemandSync(getApplicationContext()).Run();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(messageReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @android.webkit.JavascriptInterface
    public void downloadFile(String url, String path) {
        downloadFileUrl = url;
        downloadFilePath = path;
        if (!authorizeStorage(DownloadFileRequestCode)) {
            return;
        }

        getLogger().Info("Downloading file %s", url);
        String filename = "download";
        if (path != null && path.length() > 0){
            filename = new File(path).getName();

            // This doesn't appear to handle windows paths
            int index = filename.lastIndexOf('\\');
            if (index != -1) {
                filename = filename.substring(index + 1);
            }
        }

        DownloadManager.Request r = new DownloadManager.Request(android.net.Uri.parse(url));

        // This put the download in the same Download dir the browser uses
        r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

        // When downloading music and videos they will be listed in the player
        // (Seems to be available since Honeycomb only)
        r.allowScanningByMediaScanner();

        // Notify user when download is completed
        // (Seems to be available since Honeycomb only)
        r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        // start download
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        dm.enqueue(r);
    }

    public void handleSslError(SslError error, final Response<Boolean> response){
        final Context context = this;
        SslCertificate cert = error.getCertificate();

        String issuedTo = cert.getIssuedTo().getDName();
        String issuedBy = cert.getIssuedBy().getDName();
        String issuedOn = cert.getValidNotBeforeDate().toString();

        final String srch = error.getUrl()+ "--" + issuedTo + "--" + issuedBy + "--" + issuedOn;
        final String results = getSharedPreferences(this).getString("acurls1", "");

        if (StringHelper.IndexOfIgnoreCase(results, srch) != -1){
            response.onResponse(true);
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String message = getResources().getString(R.string.notification_error_ssl_cert_invalid)
                .replace("{0}", issuedTo.replace("localhost", "Emby Server"))
                .replace("{1}", issuedBy.replace("localhost", "Emby Server"))
                .replace("{2}", issuedOn);

        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                SharedPreferences settings = getSharedPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("acurls1", results + "|" + srch);
                // Commit the edits!
                boolean saved = editor.commit();

                response.onResponse(true);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                response.onResponse(false);
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @android.webkit.JavascriptInterface
    public boolean launchIntent(String uri, String dataType){
        Bundle extras = extras = new Bundle();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (dataType != null) {
            intent.setDataAndType(Uri.parse(uri), dataType);
        } else {
            intent.setData(Uri.parse(uri));
        }

        try {
            intent.putExtras(extras);
            startActivityForResult(intent, LAUNCH_REQUEST);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    @android.webkit.JavascriptInterface
    public void findServers(final int timeoutMs){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                new ServerLocator(getLogger(), new GsonJsonSerializer()).FindServers(timeoutMs, new Response<ArrayList<ServerDiscoveryInfo>>(){
                    @Override
                    public void onResponse(ArrayList<ServerDiscoveryInfo> servers){

                        String json = new GsonJsonSerializer().SerializeToString(servers);
                        RespondToWebView("window.ServerDiscoveryCallback("+json+");");
                    }

                    @Override
                    public void onError(Exception ex){
                        onResponse(new ArrayList<ServerDiscoveryInfo>());
                    }
                });
            }
        });
        thread.start();
    }

    @android.webkit.JavascriptInterface
    public void share(final String msg, final String subject, final String imageUrl, final String url) {
        String message = msg;
        boolean hasMultipleAttachments = false;
        final Intent sendIntent = new Intent(hasMultipleAttachments ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND);
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        sendIntent.setType("text/plain");
        try {
            if (imageUrl != null && imageUrl.length() > 0) {
                final String dir = getDownloadDir();
                if (dir != null) {
                    ArrayList<Uri> fileUris = new ArrayList<Uri>();
                    Uri fileUri = null;
                    fileUri = getFileUriAndSetType(sendIntent, dir, imageUrl, subject, 0);
                    if (fileUri != null) {
                        fileUris.add(fileUri);
                    }
                    if (!fileUris.isEmpty()) {
                        if (hasMultipleAttachments) {
                            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUris);
                        } else {
                            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        }
                    }
                } else {
                    sendIntent.setType("text/plain");
                }
            } else {
                sendIntent.setType("text/plain");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (notEmpty(subject)) {
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        }

        // add the URL to the message, as there seems to be no separate field
        if (notEmpty(url)) {
            if (notEmpty(message)) {
                message += " " + url;
            } else {
                message = url;
            }
        }
        if (notEmpty(message)) {
            sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);
            // sometimes required when the user picks share via sms
            if (Build.VERSION.SDK_INT < 21) { // LOLLIPOP
                sendIntent.putExtra("sms_body", message);
            }
        }

        // this was added to start the intent in a new window as suggested in #300 to prevent crashes upon return
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(Intent.createChooser(sendIntent, "Share"), SHARE_RESULT);
    }

    private PowerManager.WakeLock mWakeLock;

    @android.webkit.JavascriptInterface
    public boolean isWakeLockHeld(){
        if (mWakeLock == null){
            return false;
        }

        return mWakeLock.isHeld();
    }

    @android.webkit.JavascriptInterface
    public void acquireWakeLock(){
        if (mWakeLock == null){
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        mWakeLock.acquire();
    }

    @android.webkit.JavascriptInterface
    public void releaseWakeLock(){
        if (mWakeLock != null){
            mWakeLock.release();
        }
    }

    private WifiManager.WifiLock mNetworkLock;

    @android.webkit.JavascriptInterface
    public boolean isNetworkLockHeld(){
        if (mNetworkLock == null){
            return false;
        }
        return mNetworkLock.isHeld();
    }

    @android.webkit.JavascriptInterface
    public void acquireNetworkLock(){
        if (mNetworkLock == null){
            mNetworkLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)) .createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);
        }
        mNetworkLock.acquire();
    }

    @android.webkit.JavascriptInterface
    public void releaseNetworkLock(){
        if (mNetworkLock != null){
            mNetworkLock.release();
        }
    }

    private String getDownloadDir() throws IOException {
        // better check, otherwise it may crash the app
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            // we need to use external storage since we need to share to another app
            final String dir = getApplicationContext().getExternalFilesDir(null) + "/socialsharing-downloads";
            createOrCleanDir(dir);
            return dir;
        } else {
            return null;
        }
    }

    private void createOrCleanDir(final String downloadDir) throws IOException {
        final File dir = new File(downloadDir);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("CREATE_DIRS_FAILED");
            }
        } else {
            cleanupOldFiles(dir);
        }
    }

    /**
     * As file.deleteOnExit does not work on Android, we need to delete files manually.
     * Deleting them in onActivityResult is not a good idea, because for example a base64 encoded file
     * will not be available for upload to Facebook (it's deleted before it's uploaded).
     * So the best approach is deleting old files when saving (sharing) a new one.
     */
    private void cleanupOldFiles(File dir) {
        for (File f : dir.listFiles()) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }

    private Uri getFileUriAndSetType(Intent sendIntent, String dir, String image, String subject, int nthFile) throws IOException {
        // we're assuming an image, but this can be any filetype you like
        String localImage = image;
        if (image.endsWith("mp4") || image.endsWith("mov") || image.endsWith("3gp")){
            sendIntent.setType("video/*");
        } else {
            sendIntent.setType("image/*");
        }

        if (image.startsWith("http") || image.startsWith("www/")) {
            String filename = getFileName(image);
            localImage = "file://" + dir + "/" + filename;
            if (image.startsWith("http")) {
                // filename optimisation taken from https://github.com/EddyVerbruggen/SocialSharing-PhoneGap-Plugin/pull/56
                URLConnection connection = new URL(image).openConnection();
                String disposition = connection.getHeaderField("Content-Disposition");
                if (disposition != null) {
                    final Pattern dispositionPattern = Pattern.compile("filename=([^;]+)");
                    Matcher matcher = dispositionPattern.matcher(disposition);
                    if (matcher.find()) {
                        filename = matcher.group(1).replaceAll("[^a-zA-Z0-9._-]", "");
                        if (filename.length() == 0) {
                            // in this case we can't determine a filetype so some targets (gmail) may not render it correctly
                            filename = "file";
                        }
                        localImage = "file://" + dir + "/" + filename;
                    }
                }
                saveFile(getBytes(connection.getInputStream()), dir, filename);
            } else {
                saveFile(getBytes(getApplicationContext().getAssets().open(image)), dir, filename);
            }
        } else if (image.startsWith("data:")) {
            // safeguard for https://code.google.com/p/android/issues/detail?id=7901#c43
            if (!image.contains(";base64,")) {
                sendIntent.setType("text/plain");
                return null;
            }
            // image looks like this: data:image/png;base64,R0lGODlhDAA...
            final String encodedImg = image.substring(image.indexOf(";base64,") + 8);
            // correct the intent type if anything else was passed, like a pdf: data:application/pdf;base64,..
            if (!image.contains("data:image/")) {
                sendIntent.setType(image.substring(image.indexOf("data:") + 5, image.indexOf(";base64")));
            }
            // the filename needs a valid extension, so it renders correctly in target apps
            final String imgExtension = image.substring(image.indexOf("/") + 1, image.indexOf(";base64"));
            String fileName;
            // if a subject was passed, use it as the filename
            // filenames must be unique when passing in multiple files [#158]
            if (notEmpty(subject)) {
                fileName = sanitizeFilename(subject) + (nthFile == 0 ? "" : "_" + nthFile) + "." + imgExtension;
            } else {
                fileName = "file" + (nthFile == 0 ? "" : "_" + nthFile) + "." + imgExtension;
            }
            saveFile(Base64.decode(encodedImg, Base64.DEFAULT), dir, fileName);
            localImage = "file://" + dir + "/" + fileName;
        } else if (image.startsWith("df:")) {
            // safeguard for https://code.google.com/p/android/issues/detail?id=7901#c43
            if (!image.contains(";base64,")) {
                sendIntent.setType("text/plain");
                return null;
            }
            // format looks like this :  df:filename.txt;data:image/png;base64,R0lGODlhDAA...
            final String fileName = image.substring(image.indexOf("df:") + 3, image.indexOf(";data:"));
            final String fileType = image.substring(image.indexOf(";data:") + 6, image.indexOf(";base64,"));
            final String encodedImg = image.substring(image.indexOf(";base64,") + 8);
            sendIntent.setType(fileType);
            saveFile(Base64.decode(encodedImg, Base64.DEFAULT), dir, sanitizeFilename(fileName));
            localImage = "file://" + dir + "/" + fileName;
        } else if (!image.startsWith("file://")) {
            throw new IllegalArgumentException("URL_NOT_SUPPORTED");
        } else {
            //get file MIME type
            String type = getMIMEType(image);
            //set intent data and Type
            sendIntent.setType(type);
        }
        return Uri.parse(localImage);
    }

    private static String getFileName(String url) {
        if (url.endsWith("/")) {
            url = url.substring(0, url.length()-1);
        }
        final String pattern = ".*/([^?#]+)?";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(url);
        if (m.find()) {
            return m.group(1);
        } else {
            return "file";
        }
    }

    public static String sanitizeFilename(String name) {
        return name.replaceAll("[:\\\\/*?|<> ]", "_");
    }

    private static boolean notEmpty(String what) {
        return what != null &&
                !"".equals(what) &&
                !"null".equalsIgnoreCase(what);
    }

    private String getMIMEType(String fileName) {
        String type = "*/*";
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex == -1) {
            return type;
        }
        final String end = fileName.substring(dotIndex+1, fileName.length()).toLowerCase();
        String fromMap = MIME_Map.get(end);
        return fromMap == null ? type : fromMap;
    }

    private static final Map<String, String> MIME_Map = new HashMap<String, String>();
    static {
        MIME_Map.put("3gp",   "video/3gpp");
        MIME_Map.put("apk",   "application/vnd.android.package-archive");
        MIME_Map.put("asf",   "video/x-ms-asf");
        MIME_Map.put("avi",   "video/x-msvideo");
        MIME_Map.put("bin",   "application/octet-stream");
        MIME_Map.put("bmp",   "image/bmp");
        MIME_Map.put("c",     "text/plain");
        MIME_Map.put("class", "application/octet-stream");
        MIME_Map.put("conf",  "text/plain");
        MIME_Map.put("cpp",   "text/plain");
        MIME_Map.put("doc",   "application/msword");
        MIME_Map.put("docx",  "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_Map.put("xls",   "application/vnd.ms-excel");
        MIME_Map.put("xlsx",  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MIME_Map.put("exe",   "application/octet-stream");
        MIME_Map.put("gif",   "image/gif");
        MIME_Map.put("gtar",  "application/x-gtar");
        MIME_Map.put("gz",    "application/x-gzip");
        MIME_Map.put("h",     "text/plain");
        MIME_Map.put("htm",   "text/html");
        MIME_Map.put("html",  "text/html");
        MIME_Map.put("jar",   "application/java-archive");
        MIME_Map.put("java",  "text/plain");
        MIME_Map.put("jpeg",  "image/jpeg");
        MIME_Map.put("jpg",   "image/*");
        MIME_Map.put("js",    "application/x-javascript");
        MIME_Map.put("log",   "text/plain");
        MIME_Map.put("m3u",   "audio/x-mpegurl");
        MIME_Map.put("m4a",   "audio/mp4a-latm");
        MIME_Map.put("m4b",   "audio/mp4a-latm");
        MIME_Map.put("m4p",   "audio/mp4a-latm");
        MIME_Map.put("m4u",   "video/vnd.mpegurl");
        MIME_Map.put("m4v",   "video/x-m4v");
        MIME_Map.put("mov",   "video/quicktime");
        MIME_Map.put("mp2",   "audio/x-mpeg");
        MIME_Map.put("mp3",   "audio/x-mpeg");
        MIME_Map.put("mp4",   "video/mp4");
        MIME_Map.put("mpc",   "application/vnd.mpohun.certificate");
        MIME_Map.put("mpe",   "video/mpeg");
        MIME_Map.put("mpeg",  "video/mpeg");
        MIME_Map.put("mpg",   "video/mpeg");
        MIME_Map.put("mpg4",  "video/mp4");
        MIME_Map.put("mpga",  "audio/mpeg");
        MIME_Map.put("msg",   "application/vnd.ms-outlook");
        MIME_Map.put("ogg",   "audio/ogg");
        MIME_Map.put("pdf",   "application/pdf");
        MIME_Map.put("png",   "image/png");
        MIME_Map.put("pps",   "application/vnd.ms-powerpoint");
        MIME_Map.put("ppt",   "application/vnd.ms-powerpoint");
        MIME_Map.put("pptx",  "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        MIME_Map.put("prop",  "text/plain");
        MIME_Map.put("rc",    "text/plain");
        MIME_Map.put("rmvb",  "audio/x-pn-realaudio");
        MIME_Map.put("rtf",   "application/rtf");
        MIME_Map.put("sh",    "text/plain");
        MIME_Map.put("tar",   "application/x-tar");
        MIME_Map.put("tgz",   "application/x-compressed");
        MIME_Map.put("txt",   "text/plain");
        MIME_Map.put("wav",   "audio/x-wav");
        MIME_Map.put("wma",   "audio/x-ms-wma");
        MIME_Map.put("wmv",   "audio/x-ms-wmv");
        MIME_Map.put("wps",   "application/vnd.ms-works");
        MIME_Map.put("xml",   "text/plain");
        MIME_Map.put("z",     "application/x-compress");
        MIME_Map.put("zip",   "application/x-zip-compressed");
        MIME_Map.put("",       "*/*");
    }

    private byte[] getBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    private void saveFile(byte[] bytes, String dirName, String fileName) throws IOException {
        final File dir = new File(dirName);
        final FileOutputStream fos = new FileOutputStream(new File(dir, fileName));
        fos.write(bytes);
        fos.flush();
        fos.close();
    }
}
