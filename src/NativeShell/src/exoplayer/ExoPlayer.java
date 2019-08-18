package org.jellyfin.mobile.exoplayer;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ExoPlayer {
    private static Activity cordovaActivity;
    private static CordovaWebView cordovaWebView;

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

    public boolean checkTracksSupport(JSONArray args, CallbackContext callbackContext, Activity activity) {
        MediaCodecRenderer videoRenderer = new MediaCodecVideoRenderer(activity, MediaCodecSelector.DEFAULT);
        MediaCodecRenderer audioRenderer = new MediaCodecAudioRenderer(activity, MediaCodecSelector.DEFAULT);

        try {
            JSONArray videoTracks = args.getJSONArray(0);
            JSONArray audioTracks = args.getJSONArray(1);
            JSONArray subtitleTracks = args.getJSONArray(2);

            for (int i = 0; i < videoTracks.length(); i++) {
                JSONObject track = videoTracks.getJSONObject(i);

                String application = MimeTypes.APPLICATION_MP4;
                String codecs = MimeTypes.getVideoMediaMimeType(track.getString("codec"));
                int bitrate = track.getInt("bitrate");
                int width = track.getInt("width");
                int height = track.getInt("height");
                float framerate = (float) track.getInt("framerate");

                Format format = Format.createVideoContainerFormat(null, null, null, application, codecs, bitrate, width, height, framerate, null, C.SELECTION_FLAG_AUTOSELECT, C.ROLE_FLAG_MAIN);
                try {
                    int result = videoRenderer.supportsFormat(format) & RendererCapabilities.FORMAT_SUPPORT_MASK;
                    track.put("supported", result == RendererCapabilities.FORMAT_HANDLED || result == RendererCapabilities.FORMAT_EXCEEDS_CAPABILITIES);
                } catch (ExoPlaybackException e) {
                    track.put("supported", false);
                }

                videoTracks.put(i, track);
            }

            for (int i = 0; i < audioTracks.length(); i++) {
                JSONObject track = audioTracks.getJSONObject(i);

                String application = MimeTypes.APPLICATION_MP4;
                String codecs = MimeTypes.getAudioMediaMimeType(track.getString("codec"));
                int bitrate = track.getInt("bitrate");
                int channels = track.getInt("channels");
                int sampleRate = track.getInt("sampleRate");

                Format format = Format.createAudioContainerFormat(null, null, null, null, codecs, bitrate, channels, sampleRate, null, C.SELECTION_FLAG_AUTOSELECT, C.ROLE_FLAG_MAIN, null);
                try {
                    int result = audioRenderer.supportsFormat(format) & RendererCapabilities.FORMAT_SUPPORT_MASK;
                    track.put("supported", result == RendererCapabilities.FORMAT_HANDLED || result == RendererCapabilities.FORMAT_EXCEEDS_CAPABILITIES);
                } catch (ExoPlaybackException e) {
                    track.put("supported", false);
                }

                audioTracks.put(i, track);
            }

            for (int i = 0; i < subtitleTracks.length(); i++) {
                JSONObject track = subtitleTracks.getJSONObject(i);
                track.put("supported", true);
                subtitleTracks.put(i, track);
            }

            JSONArray tracks = new JSONArray();
            tracks.put(videoTracks);
            tracks.put(audioTracks);
            tracks.put(subtitleTracks);

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, tracks);
            callbackContext.sendPluginResult(pluginResult);
        } catch (JSONException e) {
            callbackContext.error("Wrong set of arguments for method isFormatSupported");
            return false;
        }

        return true;
    }
}
