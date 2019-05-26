package org.jellyfin.mobile.exoPlayer;

import android.app.Activity;
import android.content.Intent;

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
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ExoPlayer {

    public boolean handleRequest(String methodName, JSONArray args, CallbackContext callbackContext, Activity activity) {
        try {
            Method method = this.getClass().getMethod(methodName, JSONArray.class, CallbackContext.class, Activity.class);

            try {
                return (boolean) method.invoke(this, args, callbackContext, activity);
            } catch (IllegalAccessException e) {
                callbackContext.error("can't access method: " + methodName);
            } catch (InvocationTargetException e) {
                callbackContext.error("failed to call method: " + methodName);
            }
        } catch (NoSuchMethodException e) {
            callbackContext.error("method doesn't exist: " + methodName);
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
                int bitRate = track.getInt("bitRate");
                int width = track.getInt("width");
                int height = track.getInt("height");
                float frameRate = (float) track.getInt("frameRate");

                Format format = Format.createVideoContainerFormat(null, null, null, application, codecs, bitRate, width, height, frameRate, null, C.SELECTION_FLAG_AUTOSELECT, C.ROLE_FLAG_MAIN);

                track.put("supported", false);

                try {
                    int result = videoRenderer.supportsFormat(format) & RendererCapabilities.FORMAT_SUPPORT_MASK;
                    track.put("supported", result == RendererCapabilities.FORMAT_HANDLED || result == RendererCapabilities.FORMAT_EXCEEDS_CAPABILITIES);
                } catch (ExoPlaybackException e) {}

                videoTracks.put(i, track);
            }

            for (int i = 0; i < audioTracks.length(); i++) {
                JSONObject track = audioTracks.getJSONObject(i);

                String application = MimeTypes.APPLICATION_MP4;
                String codecs = MimeTypes.getAudioMediaMimeType(track.getString("codec"));
                int bitRate = track.getInt("bitRate");
                int channels = track.getInt("channels");
                int sampleRate = track.getInt("sampleRate");



                Format format = Format.createAudioContainerFormat(null, null, null, null, codecs, bitRate, channels, sampleRate, null, C.SELECTION_FLAG_AUTOSELECT, C.ROLE_FLAG_MAIN, null);

                track.put("supported", false);

                try {
                    int result = audioRenderer.supportsFormat(format) & RendererCapabilities.FORMAT_SUPPORT_MASK;
                    track.put("supported", result == RendererCapabilities.FORMAT_HANDLED || result == RendererCapabilities.FORMAT_EXCEEDS_CAPABILITIES);
                } catch (ExoPlaybackException e) {}

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
