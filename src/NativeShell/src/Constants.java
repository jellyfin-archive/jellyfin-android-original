package org.jellyfin.mobile;

public class Constants {
    public static final String ACTION_PLAYPAUSE = "action_playpause";
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_UNPAUSE = "action_unpause";
    public static final String ACTION_REWIND = "action_rewind";
    public static final String ACTION_FAST_FORWARD = "action_fast_foward";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";
    public static final String ACTION_REPORT = "action_report";
    public static final String ACTION_SEEK = "action_seek";
    public static final String ACTION_SHOW_PLAYER = "ACTION_SHOW_PLAYER";
    public static final int TICKS_PER_MILLISECOND = 10000;

    /**
     * exoplayer events
     */
    public static final String EVENT_VOLUME_CHANGE = "VolumeChange";
    public static final String EVENT_PLAY = "Play";
    public static final String EVENT_PLAYING = "Playing";
    public static final String EVENT_PAUSE = "Pause";
    public static final String EVENT_ENDED = "Ended";
    public static final String EVENT_TIME_UPDATE = "TimeUpdate";
}
