package org.jellyfin.cordova;

import android.os.Build;
import android.provider.Settings;
import android.content.Intent;
import android.support.v7.appcompat.BuildConfig;
import android.view.WindowManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.jellyfin.mobile.R;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mb.android.media.RemotePlayerService;

import mediabrowser.apiinteraction.android.mediabrowser.Constants;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
import static android.view.View.SYSTEM_UI_FLAG_VISIBLE;

public class NativeShell extends CordovaPlugin {
    public CallbackContext callbackContext;
    public JSONArray args;

    public static CordovaWebView cordovaWebView;

    @Override
    public void pluginInitialize() {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cordova.getActivity().getWindow().setStatusBarColor(cordova.getActivity().getResources().getColor(R.color.gray));
                    cordova.getActivity().getWindow().setNavigationBarColor(cordova.getActivity().getResources().getColor(R.color.gray));
                }
            }
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
        this.args = args;

        cordovaWebView = webView;
        switch (action) {
            case "getDeviceInformation":    
                return getDeviceInformation();
            case "enableFullscreen":
                return enableFullscreen();
            case "disableFullscreen":
                return disableFullscreen();
            case "updateMediaSession":
                return updateMediaSession();
            case "hideMediaSession":
                return hideMediaSession();
            default:
                callbackContext.error("unrecognized: " + action);
                return false;
        }
    }

    public boolean getDeviceInformation() {
        // replace this later with a randomly generated persistent string stored in local settings
        String deviceId =  Settings.Secure.getString(cordova.getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceName = Build.MODEL;
        String appName = "Jellyfin Mobile";
        String appVersion = Integer.toString(BuildConfig.VERSION_CODE);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("deviceId", deviceId);
            jsonObject.put("deviceName", deviceName);
            jsonObject.put("appName", appName);
            jsonObject.put("appVersion", appVersion);
        } catch(Exception e) {
            callbackContext.error("error: " + e.getMessage());
            return false;
        }

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
        callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    public boolean enableFullscreen() {
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

    public boolean disableFullscreen() {
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

    public boolean updateMediaSession() {
        String action;
        String itemId;
        String title;
        String artist;
        String album;
        String imageUrl;

        int duration;
        int position;

        boolean canSeek;
        boolean isPaused;
        boolean isLocalPlayer;
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
            callbackContext.error("error: " + e.getMessage());
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

    public boolean hideMediaSession() {
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
