package org.jellyfin.mobile.exoplayer;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.text.TextUtils;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ExoPlayer {
    private static Activity cordovaActivity;
    private static CordovaWebView cordovaWebView;
    private static ExoPlayerActivity playerActivity = null;
    private static Map<String, ExoPlayerCodec> supportedVideoCodecs = new HashMap<>();
    private static Map<String, ExoPlayerCodec> supportedAudioCodecs = new HashMap<>();

    public boolean handleRequest(String methodName, JSONArray args, CallbackContext callbackContext, Activity activity, CordovaWebView webView) {
        cordovaActivity = activity;
        cordovaWebView = webView;

        try {
            Method method = this.getClass().getMethod(methodName, JSONArray.class, CallbackContext.class, Activity.class);
            return (boolean) method.invoke(this, args, callbackContext, activity);
        } catch (NoSuchMethodException e) {
            callbackContext.error("method doesn't exist: " + methodName);
        } catch (IllegalAccessException e) {
            callbackContext.error("can't access method: " + methodName);
        } catch (InvocationTargetException e) {
            callbackContext.error("failed to call method: " + methodName);
        }

        callbackContext.error("unknown error for method: " + methodName);
        return false;
    }

    public boolean loadPlayer(JSONArray args, CallbackContext callbackContext, Activity activity) {
        Intent playerIntent = new Intent(activity.getApplicationContext(), ExoPlayerActivity.class);

        try {
            playerIntent.putExtra("item", args.getJSONObject(0).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        activity.startActivity(playerIntent);

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        callbackContext.sendPluginResult(pluginResult);

        return true;
    }

    /**
     * call a method from ExoPlayer instance present in window object
     * @param method    method to invoke in the ExoPlayer instance
     * @param arguments optional arguments to call with this method
     */
    public static void callWebMethod(String method, String... arguments) {
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = "'" + arguments[i] + "'";
        }

        cordovaActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cordovaWebView.loadUrl("javascript:window.ExoPlayer." + method + "(" + TextUtils.join(",", arguments) + ")");
            }
        });
    }

    public static void setPlayer(ExoPlayerActivity player) {
        playerActivity = player;
    }

    public static void unsetPlayer() {
        playerActivity = null;
    }

    public boolean getVolume(JSONArray args, CallbackContext callbackContext, Activity activity) {
        if (playerActivity != null) {
            callbackContext.success(playerActivity.getVolume());
            return true;
        }

        callbackContext.error("player activity not initialized");
        return false;
    }

    public boolean setVolume(JSONArray args, CallbackContext callbackContext, Activity activity) {
        if (playerActivity != null) {
            try {
                playerActivity.setVolume(new Float(args.getString(0)));
                callbackContext.success();
                return true;
            } catch (JSONException e) {
                callbackContext.error("wrong parameter for setVolume: " + args.toString());
            }
        }

        callbackContext.error("player activity not initialized");
        return false;
    }

    public boolean destroyPlayer(JSONArray args, CallbackContext callbackContext, Activity activity) {
        if (playerActivity != null) {
            playerActivity.finish();
        }

        callbackContext.success();
        return true;
    }

    public boolean pausePlayer(JSONArray args, CallbackContext callbackContext, Activity activity) {
        if (playerActivity != null) {
            playerActivity.pause();
        }

        callbackContext.success();
        return true;
    }

    public boolean resumePlayer(JSONArray args, CallbackContext callbackContext, Activity activity) {
        if (playerActivity != null) {
            playerActivity.resume();
        }

        callbackContext.success();
        return true;
    }

    public boolean stopPlayer(JSONArray args, CallbackContext callbackContext, Activity activity) {
        if (playerActivity != null) {
            playerActivity.stop();
        }

        callbackContext.success();
        return true;
    }

    public boolean getSupportedFormats(JSONArray args, CallbackContext callbackContext, Activity activity) {
        if (supportedVideoCodecs.size() == 0 && supportedAudioCodecs.size() == 0) {
            MediaCodecList codecs = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
            for (MediaCodecInfo codec : codecs.getCodecInfos()) {
                if (!codec.isEncoder()) {
                    for (String mimeType : codec.getSupportedTypes()) {
                        ExoPlayerCodec codecObj = ExoPlayerFormats.getCodecCapabilities(codec.getCapabilitiesForType(mimeType));
                        if (codecObj != null) {
                            Map<String, ExoPlayerCodec> supportedCodecs = codecObj.isAudio() ? supportedAudioCodecs : supportedVideoCodecs;
                            if (supportedCodecs.containsKey(mimeType)) {
                                ExoPlayerCodec currentCodecObj = supportedCodecs.get(mimeType);
                                currentCodecObj.mergeCodec(codecObj);
                            } else {
                                supportedCodecs.put(mimeType, codecObj);
                            }
                        }
                    }
                }
            }
        }

        try {
            JSONObject response = new JSONObject();
            JSONArray codecs = new JSONArray();
            for (ExoPlayerCodec codec : supportedAudioCodecs.values()) {
                codecs.put(codec.getJSONObject());
            }

            response.put("audioCodecs", codecs);

            codecs = new JSONArray();
            for (ExoPlayerCodec codec : supportedVideoCodecs.values()) {
                codecs.put(codec.getJSONObject());
            }

            response.put("videoCodecs", codecs);
            callbackContext.success(response);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
}
