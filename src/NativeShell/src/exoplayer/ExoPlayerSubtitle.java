package org.jellyfin.mobile.exoplayer;

public class ExoPlayerSubtitle {
    private String title;
    private String language;
    private int index;
    private Integer playerIndex = null;

    public ExoPlayerSubtitle(String title, String language, int index) {
        this.title = title;
        this.language = language;
        this.index = index;
    }

    public String getTitle() {
        return title;
    }

    public String getLanguage() {
        return language;
    }

    public int getIndex() {
        return index;
    }

    public Integer getPlayerIndex() {
        return playerIndex;
    }

    public void setPlayerIndex(Integer playerIndex) {
        this.playerIndex = playerIndex;
    }
}
