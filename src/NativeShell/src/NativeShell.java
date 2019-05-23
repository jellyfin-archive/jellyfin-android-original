package org.jellyfin.mobile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.content.Intent;
import android.view.WindowManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.engine.SystemWebView;
import org.apache.cordova.engine.SystemWebViewEngine;
import org.jellyfin.mobile.exoPlayer.ExoPlayer;
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
                // use custom WebView client to handle invalid SSL certificates
                if (webView instanceof SystemWebView) {
                    SystemWebView systemWebView = (SystemWebView) webView;
                    SystemWebViewEngine engine = new SystemWebViewEngine(new SystemWebView(cordova.getActivity()), preferences);
                    systemWebView.setWebViewClient(new CustomWebViewClient(cordova.getActivity(), engine));
                }
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
                        // TODO translate these strings
                        builder.setTitle("Disable Battery Optimizations");
                        builder.setMessage("Please disable battery optimizations for media playback while the screen is off.");
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
            }
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {
        cordovaWebView = webView;

        if (action.startsWith("exoPlayer")) {
            action = action.replace("exoPlayer.", "");
            return exoPlayer.handleRequest(action, args, callbackContext, cordova.getActivity());
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
            default:
                callbackContext.error("error: unrecognized action: " + action);
                return false;
        }
    }

    private boolean getDeviceInformation() {
        // replace this later with a randomly generated persistent string stored in local settings
        String deviceId = Settings.Secure.getString(cordova.getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceName = Build.MODEL;
        String appName = "Jellyfin Mobile";
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
}
