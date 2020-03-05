package org.jellyfin.mobile.exoplayer.mediaSource;

import org.jellyfin.mobile.exoplayer.ExoPlayerFormats;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Map;

public class ExoPlayerTextTrack extends ExoPlayerBaseTrack {
    private String language;
    private String uri;
    private String format;

    public ExoPlayerTextTrack() {
        super();
    }

    public ExoPlayerTextTrack(JSONObject track, Map<Integer, String> textTracksUrl) throws JSONException {
        super(track);
        language = track.getString("Language");
        uri = textTracksUrl.containsKey(this.getIndex()) ? textTracksUrl.get(this.getIndex()) : null;
        format = ExoPlayerFormats.getSubtitleFormat(track.getString("Codec"));
    }

    public String getLanguage() {
        return language;
    }

    public String getUri() {
        return uri;
    }

    public String getFormat() {
        return format;
    }
}
