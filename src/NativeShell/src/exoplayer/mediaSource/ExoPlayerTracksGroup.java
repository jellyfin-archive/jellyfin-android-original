package org.jellyfin.mobile.exoplayer.mediaSource;

import java.util.Map;

public class ExoPlayerTracksGroup<T extends ExoPlayerBaseTrack> {
    private Map<Integer, T> tracks;
    private Integer selectedTrack;

    public ExoPlayerTracksGroup(Integer selectedTrack, Map<Integer, T> tracks) {
        this.selectedTrack = selectedTrack;
        this.tracks = tracks;
    }

    public Map<Integer, T> getTracks() {
        return tracks;
    }

    public Integer getSelectedTrack() {
        return selectedTrack;
    }

    public void setSelectedTrack(Integer index) {
        this.selectedTrack = index;
    }
}