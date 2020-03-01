package org.jellyfin.mobile.exoplayer.mediaSource;

import org.json.JSONException;
import org.json.JSONObject;

public class ExoPlayerVideoTrack extends ExoPlayerBaseTrack {
    public ExoPlayerVideoTrack(JSONObject track) throws JSONException {
        super(track);
    }
}
