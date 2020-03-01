package org.jellyfin.mobile.exoplayer.mediaSource;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class ExoPlayerBaseTrack {
    private Integer index;
    private Integer playerIndex = null;
    private String title;

    public ExoPlayerBaseTrack(JSONObject track) throws JSONException {
        this.index = track.getInt("Index");
        this.title = track.getString("DisplayTitle");
    }

    public Integer getPlayerIndex() {
        return playerIndex;
    }

    public void setPlayerIndex(Integer index) {
        this.playerIndex = index;
    }


    public Integer getIndex() {
        return index;
    }

    public String getTitle() {
        return title;
    }
}
