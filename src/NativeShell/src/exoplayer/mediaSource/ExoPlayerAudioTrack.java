package org.jellyfin.mobile.exoplayer.mediaSource;

import org.json.JSONException;
import org.json.JSONObject;

public class ExoPlayerAudioTrack extends ExoPlayerBaseTrack {
    private String language;

    public ExoPlayerAudioTrack(JSONObject track) throws JSONException {
        super(track);
        this.language = track.getString("Language");
    }

    public String getLanguage() {
        return this.language;
    }
}
