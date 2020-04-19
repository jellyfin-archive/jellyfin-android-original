package org.jellyfin.mobile.exoplayer.mediaSource;

import org.jellyfin.mobile.exoplayer.ExoPlayerFormats;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Map;

public class ExoPlayerTextTrack extends ExoPlayerBaseTrack {
    private String language;
    private String uri;
    private String format;
    private boolean localDelivery;

    public ExoPlayerTextTrack() {
        super();
    }

    public ExoPlayerTextTrack(JSONObject track, Map<Integer, String> textTracksUrl) throws JSONException {
        super(track);

        if (track.has("Language")) {
            language = track.getString("Language");
        } else {
            language = "und";
        }

        uri = textTracksUrl.containsKey(this.getIndex()) ? textTracksUrl.get(this.getIndex()) : null;
        format = ExoPlayerFormats.getSubtitleFormat(track.getString("Codec"));

        String deliveryMethod = track.getString("DeliveryMethod");

        localDelivery = deliveryMethod.equals("Embed") || deliveryMethod.equals("External");
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

    public boolean canApplyLocalSubtitle() {
        return localDelivery;
    }
}
