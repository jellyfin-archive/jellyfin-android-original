package org.jellyfin.mobile.exoplayer.mediaSource;

import org.json.JSONException;
import org.json.JSONObject;

public class ExoPlayerAudioTrack extends ExoPlayerBaseTrack {
    private String language;
    private boolean supportsDirectPlay;

    public ExoPlayerAudioTrack(JSONObject track) throws JSONException {
        super(track);
        this.language = track.has("Language") ? track.getString("Language") : null;
        this.supportsDirectPlay = track.getBoolean("supportsDirectPlay");
    }

    public String getLanguage() {
        return this.language;
    }

    public boolean supportsDirectPlay() {
        return this.supportsDirectPlay;
    }
}
