package org.jellyfin.mobile.exoplayer.mediaSource;

import com.google.android.exoplayer2.C;

import org.jellyfin.mobile.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExoPlayerMediaSource {
    private JSONObject item;
    private String title;
    private String url;
    private Long mediaStartTicks;
    private boolean isTranscoding;
    private Map<Integer, ExoPlayerTracksGroup> tracksGroup = new HashMap();

    public ExoPlayerMediaSource(JSONObject item) throws JSONException {
        this.item = item;
        this.title = item.getString("title");
        this.url = item.getString("url");
        this.mediaStartTicks = item.has("playerStartPositionTicks") ? item.getLong("playerStartPositionTicks") : null;

        if (this.mediaStartTicks > 0) {
            this.mediaStartTicks /= Constants.TICKS_PER_MILLISECOND;
        }

        loadMediaSource(item.getJSONObject("mediaSource"));
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public Long getMediaStartTicks() {
        return mediaStartTicks;
    }

    public boolean isTranscoding() {
        return isTranscoding;
    }

    public Map<Integer, ExoPlayerTracksGroup> getTracksGroup() {
        return tracksGroup;
    }

    private void loadMediaSource(JSONObject mediaSource) throws JSONException {
        JSONArray tracks = mediaSource.getJSONArray("MediaStreams");
        List<JSONObject> subtitleTracks = new ArrayList();
        List<JSONObject> audioTracks = new ArrayList();
        List<JSONObject> videoTracks = new ArrayList();

        for (int index = 0; index < tracks.length(); index++) {
            JSONObject track = tracks.getJSONObject(index);

            switch (track.getString("Type")) {
                case "Subtitle":
                    subtitleTracks.add(track);
                    break;
                case "Audio":
                    audioTracks.add(track);
                    break;
                case "Video":
                    videoTracks.add(track);
                    break;
                default: // other streams ignore
                    break;
            }
        }

        loadSubtitleTracks(mediaSource, subtitleTracks);
        loadAudioTracks(mediaSource, audioTracks);
        loadVideoTracks(videoTracks);

        this.isTranscoding = item.has("TranscodingSubProtocol") && item.getString("TranscodingSubProtocol").equals("hls");
    }

    private void loadVideoTracks(List<JSONObject> tracks) throws JSONException {
        Integer defaultSelection = 1;
        Map<Integer, ExoPlayerVideoTrack> videoTracks = new HashMap();

        for (JSONObject track: tracks) {
            videoTracks.put(track.getInt("Index"), new ExoPlayerVideoTrack(track));
        }

        tracksGroup.put(C.TRACK_TYPE_VIDEO, new ExoPlayerTracksGroup(defaultSelection, videoTracks));
    }

    private void loadAudioTracks(JSONObject mediaSource, List<JSONObject> tracks) throws JSONException {
        Integer defaultSelection = mediaSource.getInt("DefaultAudioStreamIndex");
        Map<Integer, ExoPlayerAudioTrack> audioTracks = new HashMap();

        for (JSONObject track: tracks) {
            audioTracks.put(track.getInt("Index"), new ExoPlayerAudioTrack(track));
        }

        tracksGroup.put(C.TRACK_TYPE_AUDIO, new ExoPlayerTracksGroup(defaultSelection, audioTracks));
    }

    private void loadSubtitleTracks(JSONObject mediaSource, List<JSONObject> tracks) throws JSONException {
        Integer defaultSelection = null;
        Map<Integer, String> textTracksUrl = new HashMap();
        JSONArray textTracks = item.getJSONArray("textTracks");

        for (int index = 0; index < textTracks.length(); index++) {
            JSONObject textTrack = textTracks.getJSONObject(index);
            textTracksUrl.put(textTrack.getInt("index"), textTrack.getString("url"));
        }

        if (mediaSource.has("DefaultSubtitleStreamIndex")) {
            defaultSelection = mediaSource.getInt("DefaultSubtitleStreamIndex");
        }

        Map<Integer, ExoPlayerTextTrack> finalTracks = new HashMap();

        for (JSONObject track: tracks) {
            finalTracks.put(track.getInt("Index"), new ExoPlayerTextTrack(track,textTracksUrl));
        }

        tracksGroup.put(C.TRACK_TYPE_TEXT, new ExoPlayerTracksGroup(defaultSelection, finalTracks));
    }
}
