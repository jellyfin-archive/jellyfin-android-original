package org.jellyfin.mobile;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.jellyfin.mobile.exoplayer.ExoPlayer;
import org.json.JSONArray;
import org.json.JSONObject;

import static android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS;
import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
import static android.view.View.SYSTEM_UI_FLAG_VISIBLE;

public class NativeShell extends CordovaPlugin {
    public static CordovaWebView cordovaWebView;
    public static PowerManager.WakeLock wakeLock;

    private CallbackContext callbackContext;
    private JSONArray args;
    private ExoPlayer exoPlayer = new ExoPlayer();

    @Override
    public void pluginInitialize() {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // theme the system bars so that all models use the same color
                // several manufacturers style the navigation bar differently
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cordova.getActivity().getWindow().setStatusBarColor(cordova.getActivity().getResources().getColor(android.R.color.black));
                    cordova.getActivity().getWindow().setNavigationBarColor(cordova.getActivity().getResources().getColor(android.R.color.black));
                }

                // ask for battery optimizations on new phones
                // we can only send users to the settings page and not request it directly
                // google play prohibits asking directly except under a select few conditions
                PowerManager powerManager = (PowerManager) cordova.getActivity().getSystemService(Context.POWER_SERVICE);
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JellyfinWakeLock");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!AppPreferences.get(cordova.getActivity()).getIgnoreBatteryOptimizations()
                            && !powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(cordova.getActivity());
                        builder.setTitle(cordova.getContext().getString(R.string.battery_optimizations_title));
                        builder.setMessage(cordova.getContext().getString(R.string.battery_optimizations_message));
                        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AppPreferences.get(cordova.getActivity()).setIgnoreBatteryOptimizations(true);
                            }
                        });

                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Intent intent = new Intent(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                                    cordova.getActivity().startActivity(intent);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        builder.show();
                    }
                }

                // request storage permission at runtime
                if (ContextCompat.checkSelfPermission(cordova.getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(cordova.getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                }

                // add intent filter to watch for headphone state
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_HEADSET_PLUG);

                // bluetooth related filters - needs BLUETOOTH permission
                filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
                filter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
                cordova.getActivity().registerReceiver(RemotePlayerService.receiver, filter);
            }
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {
        cordovaWebView = webView;

        if (action.startsWith("exoplayer")) {
            action = action.replace("exoplayer.", "");
            return exoPlayer.handleRequest(action, args, callbackContext, cordova.getActivity(), webView);
        }

        this.callbackContext = callbackContext;
        this.args = args;

        switch (action) {
            case "getDeviceInformation":
                return getDeviceInformation();
            case "enableFullscreen":
                return enableFullscreen();
            case "disableFullscreen":
                return disableFullscreen();
            case "openIntent":
                return openIntent();
            case "updateMediaSession":
                return updateMediaSession();
            case "hideMediaSession":
                return hideMediaSession();
            case "downloadFile":
                return downloadFile();
            default:
                callbackContext.error("error: unrecognized action: " + action);
                return false;
        }
    }

    private boolean getDeviceInformation() {
        // replace this later with a randomly generated persistent string stored in local settings
        String deviceId = Settings.Secure.getString(cordova.getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceName = Build.MODEL;
        String appName = "Jellyfin Android";
        String appVersion = Integer.toString(BuildConfig.VERSION_CODE);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("deviceId", deviceId);
            jsonObject.put("deviceName", deviceName);
            jsonObject.put("appName", appName);
            jsonObject.put("appVersion", appVersion);
        } catch (Exception e) {
            callbackContext.error("error: getDeviceInformation: " + e.getMessage());
            return false;
        }

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
        callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    private boolean enableFullscreen() {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int visibility = SYSTEM_UI_FLAG_FULLSCREEN
                        | SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                cordova.getActivity().getWindow().getDecorView().setSystemUiVisibility(visibility);
                cordova.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                cordova.getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            }
        });

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    private boolean disableFullscreen() {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cordova.getActivity().getWindow().getDecorView().setSystemUiVisibility(SYSTEM_UI_FLAG_VISIBLE);
                cordova.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                cordova.getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        });

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    public boolean openIntent() {
        String uri;
        try {
            uri = args.getString(0);
        } catch (Exception e) {
            callbackContext.error("error: openIntent: " + e.getMessage());
            return false;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        try {
            cordova.getActivity().startActivity(intent);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        } catch (Exception e) {
            callbackContext.error("error: openIntent: " + e.getMessage());
            return false;
        }
    }

    private boolean updateMediaSession() {
        String action, itemId, title, artist, album, imageUrl;
        int duration, position;
        boolean canSeek, isPaused, isLocalPlayer;

        try {
            JSONObject options = args.getJSONObject(0);
            action = options.getString("action");
            itemId = options.getString("itemId");
            title = options.getString("title");
            artist = options.getString("artist");
            album = options.getString("album");
            imageUrl = options.getString("imageUrl");

            duration = options.getInt("duration");
            position = options.getInt("position");

            canSeek = options.getBoolean("canSeek");
            isPaused = options.getBoolean("isPaused");
            isLocalPlayer = options.getBoolean("isLocalPlayer");
        } catch (Exception e) {
            callbackContext.error("error: updateMediaSession: " + e.getMessage());
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(cordova.getActivity(), RemotePlayerService.class);
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
            cordova.getActivity().startService(intent);
        }

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    private boolean hideMediaSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(cordova.getActivity(), RemotePlayerService.class);
            intent.setAction(Constants.ACTION_REPORT);
            intent.putExtra("playerAction", "playbackstop");
            cordova.getActivity().startService(intent);
        }

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    private boolean downloadFile() {
        String title;
        String filename;
        String url;

        try {
            JSONObject options = args.getJSONObject(0);
            title = options.getString("title");
            filename = options.getString("filename");
            url = options.getString("url");
        } catch (Exception e) {
            callbackContext.error("error: download: " + e.getMessage());
            return false;
        }

        Context context = cordova.getContext();
        Uri uri = Uri.parse(url);

        DownloadManager.Request request = new DownloadManager.Request(uri)
            .setTitle(title)
            .setDescription(cordova.getContext().getString(R.string.downloading))
            .setDestinationInExternalPublicDir("Jellyfin", filename)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        if (AppPreferences.get(cordova.getActivity()).getDownloadMethodDialogShown()) {
            startDownload(request);
        } else {
            cordova.getActivity().runOnUiThread(() -> {
                new AlertDialog.Builder(context)
                        .setTitle(cordova.getContext().getString(R.string.network_title))
                        .setMessage(cordova.getContext().getString(R.string.network_message))
                        .setNegativeButton(cordova.getContext().getString(R.string.wifi_only), (dialog, which) -> {
                            AppPreferences.get(cordova.getActivity()).setDownloadMethod(0);
                            startDownload(request);
                        })
                        .setPositiveButton(cordova.getContext().getString(R.string.mobile_data), ((dialog, which) -> {
                            AppPreferences.get(cordova.getActivity()).setDownloadMethod(1);
                            startDownload(request);
                        }))
                        .setPositiveButton(cordova.getContext().getString(R.string.mobile_data_and_roaming), ((dialog, which) -> {
                            AppPreferences.get(cordova.getActivity()).setDownloadMethod(2);
                            startDownload(request);
                        }))
                        .show();
            });
        }

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    private void startDownload(DownloadManager.Request request) {
        int method = AppPreferences.get(cordova.getActivity()).getDownloadMethod();
        switch (method) {
            case 0:
                request.setAllowedOverMetered(false).setAllowedOverRoaming(false);
                break;
            case 1:
                request.setAllowedOverMetered(true).setAllowedOverRoaming(false);
                break;
            case 2:
                request.setAllowedOverMetered(true).setAllowedOverRoaming(true);
                break;
        }

        Context context = cordova.getContext();
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
    }
}
