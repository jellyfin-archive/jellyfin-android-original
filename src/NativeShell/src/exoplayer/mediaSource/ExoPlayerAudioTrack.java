package org.jellyfin.mobile.exoplayer.mediaSource;

import org.json.JSONException;
import org.json.JSONObject;

public class ExoPlayerAudioTrack extends ExoPlayerBaseTrack {
    public ExoPlayerAudioTrack(JSONObject track) throws JSONException {
        super(track);
    }
}
