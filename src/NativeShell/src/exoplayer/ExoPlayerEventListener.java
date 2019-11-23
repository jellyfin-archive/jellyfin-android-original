package org.jellyfin.mobile.exoplayer;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioListener;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import org.jellyfin.mobile.Constants;

public class ExoPlayerEventListener implements Player.EventListener, AudioListener {

    private ExoPlayerActivity instance;

    public ExoPlayerEventListener(ExoPlayerActivity instance) {
        this.instance = instance;
    }

    public void onVolumeChanged(float volume) {
        instance.notifyEvent(Constants.EVENT_VOLUME_CHANGE, Float.toString(volume));
    }

    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        String event = null;

        switch (playbackState) {
            case Player.STATE_BUFFERING:
                if (playWhenReady) {
                    event = Constants.EVENT_PLAY;
                } else {
                    event = Constants.EVENT_PAUSE;
                }
                break;
            case Player.STATE_READY:
                if (playWhenReady) {
                    event = Constants.EVENT_PLAYING;
                } else {
                    event = Constants.EVENT_PAUSE;
                }
                break;
            case Player.STATE_IDLE:
                event = Constants.EVENT_PAUSE;
                break;
            case Player.STATE_ENDED:
                event = Constants.EVENT_ENDED;
            default:
                break;
        }

        if (event != null) {
            instance.notifyEvent(event);
        }
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        instance.processGroupTracks(trackSelections);
    }
}
