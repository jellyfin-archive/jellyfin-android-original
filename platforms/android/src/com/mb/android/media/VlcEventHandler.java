package com.mb.android.media;

import com.mb.android.MainActivity;
import org.videolan.libvlc.MediaPlayer;

import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 6/10/2015.
 */
public class VlcEventHandler implements MediaPlayer.EventListener {

    private ILogger logger;
    protected MediaPlayer mLibVLC;

    private long lastReportTime;

    public VlcEventHandler(ILogger logger, MediaPlayer mLibVLC) {
        this.logger = logger;
        this.mLibVLC = mLibVLC;
    }

    @Override
    public void onEvent(MediaPlayer.Event event) {

        switch (event.type) {
            case MediaPlayer.Event.EncounteredError:
                logger.Debug("MediaPlayerEncounteredError");
                reportState("playbackstop");
                break;
            case MediaPlayer.Event.EndReached:
                logger.Debug("MediaPlayerEndReached");
                reportState("playbackstop");
                break;
            case MediaPlayer.Event.ESAdded:
                logger.Debug("MediaPlayerESAdded");
                break;
            case MediaPlayer.Event.ESDeleted:
                logger.Debug("MediaPlayerESDeleted");
                break;
            case MediaPlayer.Event.Paused:
                logger.Debug("MediaPlayerPaused");
                reportState("paused");
                break;
            case MediaPlayer.Event.Playing:
                logger.Debug("MediaPlayerPlaying");
                reportState("playing");
                break;
            case MediaPlayer.Event.PositionChanged:
                break;
            case MediaPlayer.Event.Stopped:
                logger.Debug("MediaPlayerStopped");
                reportState("playbackstop");
                break;
            case MediaPlayer.Event.TimeChanged:

                // Avoid overly aggressive reporting
                if ((System.currentTimeMillis() - lastReportTime) < 800){
                    return;
                }

                lastReportTime = System.currentTimeMillis();
                reportState("positionchange");
                break;
            case MediaPlayer.Event.Vout:
                logger.Debug("MediaPlayerVout");
                break;
            default:
                break;
        }
    }

    private void reportState(String eventName) {

        int playerState = mLibVLC.getPlayerState();

        // Expected states by web plugins are: IDLE/CLOSE=0, OPENING=1, BUFFERING=2, PLAYING=3, PAUSED=4, STOPPING=5, ENDED=6, ERROR=7
        boolean isPaused = eventName.equalsIgnoreCase("playbackstop") ?
                false :
                eventName.equalsIgnoreCase("paused") || playerState == 4;

        //logger.Debug("Vlc player state: %s", playerState);

        long length = mLibVLC.getLength() / 1000;

        long time = mLibVLC.getTime();

        int volume = mLibVLC.getVolume();

        String js = String.format("window.VlcAudioPlayer.report('%s', %s, %s, %s, %s)",
                eventName,
                String.valueOf(length).toLowerCase(),
                String.valueOf(time).toLowerCase(),
                String.valueOf(isPaused).toLowerCase(),
                String.valueOf(volume).toLowerCase());

        RespondToWebView(js);
    }

    private void RespondToWebView(final String js) {

        //logger.Info("Sending url to webView: %s", js);
        MainActivity.RespondToWebView(js);
    }
}
