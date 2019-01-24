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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.view.WindowManager;
import android.webkit.WebView;

import com.mb.android.api.ApiClientBridge;
import com.mb.android.logging.AppLogger;
import com.mb.android.media.RemotePlayerService;

import org.apache.cordova.BuildConfig;
import org.apache.cordova.CordovaActivity;
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
import mediabrowser.apiinteraction.discovery.ServerLocator;
import mediabrowser.apiinteraction.http.IAsyncHttpClient;
import mediabrowser.model.apiclient.ServerDiscoveryInfo;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
import static android.view.View.SYSTEM_UI_FLAG_VISIBLE;

public class MainActivity extends CordovaActivity {
    private final int LAUNCH_REQUEST = 990;
    private final int REQUEST_DIRECTORY = 998;
    private final int REQUEST_DIRECTORY_SAF = 996;
    public static final int SHARE_RESULT = 980;
    private static IWebView webView;
    private IAsyncHttpClient httpClient;
    private IJsonSerializer jsonSerializer;

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

        jsonSerializer = new GsonJsonSerializer();

        ApiClientBridge apiClientBridge = new ApiClientBridge(context, logger, webView, jsonSerializer);
        httpClient = apiClientBridge.httpClient;

        return engine;
    }

    public void addJavascriptInterfaces(){
        webView.addJavascriptInterface(ApiClientBridge.Current, "ApiClientBridge");
        webView.addJavascriptInterface(this, "MainActivity");
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
    public boolean enableVlcPlayer() {
        return false;
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
                .replace("{0}", issuedTo.replace("localhost", "Jellyfin Server"))
                .replace("{1}", issuedBy.replace("localhost", "Jellyfin Server"))
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
}
