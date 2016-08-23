package com.mb.android.media;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.IOException;
import java.util.Date;

import mediabrowser.apiinteraction.android.mediabrowser.Constants;
import mediabrowser.apiinteraction.android.mediabrowser.IPlayback;
import mediabrowser.apiinteraction.android.mediabrowser.IPlaybackCallback;
import mediabrowser.apiinteraction.android.mediabrowser.MediaProvider;
import mediabrowser.model.logging.ILogger;

import static android.media.MediaPlayer.OnPreparedListener;
import static android.media.session.MediaSession.QueueItem;

/**
 * A class that implements local media playback using {@link android.media.MediaPlayer}
 */
@TargetApi(14)
public class Playback implements IPlayback, AudioManager.OnAudioFocusChangeListener {

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final int VOLUME_DUCK = 20;
    // The volume we set the media player when we have audio focus.
    public static final int VOLUME_NORMAL = 100;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED  = 2;

    private final MediaService mService;
    private final WifiManager.WifiLock mWifiLock;
    private int mState;
    private boolean mPlayOnFocusGain;
    private IPlaybackCallback mCallback;
    private MediaProvider mMusicProvider;
    private volatile boolean mAudioNoisyReceiverRegistered;
    private volatile long mCurrentPosition;
    private volatile String mCurrentMediaId;

    private PowerManager.WakeLock mWakeLock;

    // Type of audio focus we have:
    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private AudioManager mAudioManager;
    private org.videolan.libvlc.MediaPlayer mMediaPlayer;
    private boolean mIsAudioTrack = false;
    private boolean mHasHdmiAudio = false;

    private IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private ILogger logger;

    private BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                logger.Debug("Headphones disconnected.");
                if (isPlaying()) {
                    Intent i = new Intent(context, MediaService.class);
                    i.setAction(Constants.ACTION_PAUSE);
                    mService.startService(i);
                }
            }
            else if (action.equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG) && intent.getIntExtra("state", 0) != 0) {
                logger.Info("Headset Inserted.");
                if (getState() == PlaybackState.STATE_PAUSED) {
                    Intent i = new Intent(context, MediaService.class);
                    i.setAction(Constants.ACTION_UNPAUSE);
                    mService.startService(i);
                }
            }
        }
    };

    public Playback(MediaService service, MediaProvider musicProvider, ILogger logger) {
        this.mService = service;
        this.mMusicProvider = musicProvider;
        this.logger = logger;
        this.mAudioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        this.mWifiLock = ((WifiManager) service.getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "sample_lock");

        // Make sure the audio player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        PowerManager pm = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Playback");
    }

    private org.videolan.libvlc.MediaPlayer newMediaPlayer(Context context) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        final org.videolan.libvlc.MediaPlayer mp = new org.videolan.libvlc.MediaPlayer(LibVLC(context, logger));
        final String aout = VLCOptions.getAout(pref);
        if (mp.setAudioOutput(aout) && aout.equals("android_audiotrack")) {
            mIsAudioTrack = true;
            if (mHasHdmiAudio)
                mp.setAudioOutputDevice("hdmi");
        } else
            mIsAudioTrack = false;
        return mp;
    }

    private static LibVLC LibVLC(Context context, ILogger logger) {
        return VLCInstance.get(context, logger);
    }

    public void start() {
    }

    public void stop(boolean notifyListeners) {
        mState = PlaybackState.STATE_STOPPED;
        if (notifyListeners && mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
        mCurrentPosition = getCurrentPositionMs();
        // Give up Audio focus
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        // Relax all resources
        relaxResources(true);
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    public void setState(int state) {
        this.mState = state;
    }

    public int getState() {
        return mState;
    }

    public boolean isPlaying() {
        return mPlayOnFocusGain || (mMediaPlayer != null && mMediaPlayer.isPlaying());
    }

    @Override
    public long getCurrentPositionMs() {
        return mMediaPlayer != null ?
                mMediaPlayer.getTime() : mCurrentPosition;
    }

    public void play(String source) {
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        String mediaId = source;
        boolean mediaHasChanged = !TextUtils.equals(mediaId, mCurrentMediaId);
        if (mediaHasChanged) {
            mCurrentPosition = 0;
            mCurrentMediaId = mediaId;
        }

        if (mState == PlaybackState.STATE_PAUSED && !mediaHasChanged && mMediaPlayer != null) {
            configMediaPlayerState();
        } else {
            mState = PlaybackState.STATE_STOPPED;
            relaxResources(false); // release everything except MediaPlayer

            try {
                createMediaPlayerIfNeeded();

                mState = PlaybackState.STATE_BUFFERING;

                final MediaWrapper mw = new MediaWrapper(Uri.parse(source));
                final Media media = new Media(VLCInstance.get(mService.getApplicationContext(), logger), mw.getUri());
                VLCOptions.setMediaOptions(media, mService.getApplicationContext(), 0 | mw.getFlags());
                //media.setEventListener(mMediaListener);
                mMediaPlayer.setMedia(media);
                media.release();
                mMediaPlayer.setEqualizer(VLCOptions.getEqualizer(mService.getApplicationContext()));
                mMediaPlayer.setVideoTitleDisplay(MediaPlayer.Position.Disable, 0);
                mMediaPlayer.setEventListener(new VlcServiceEventHandler(logger, mMediaPlayer, this));
                mMediaPlayer.play();

                // If we are streaming from the internet, we want to hold a
                // Wifi lock, which prevents the Wifi radio from going to
                // sleep while the song is playing.
                mWifiLock.acquire();
                mWakeLock.acquire();

                if (mCallback != null) {
                    mCallback.onPlaybackStatusChanged(mState);
                }

                configMediaPlayerState();

            } catch (Exception ex) {
                logger.ErrorException("Exception playing media", ex);
                if (mCallback != null) {
                    mCallback.onPlaybackError(ex.getMessage());
                }
            }
        }
    }

    @TargetApi(21)
    public void play(QueueItem item) {
        MediaMetadata track = mMusicProvider.getMusic(item.getDescription().getMediaId());

        String source = track.getString(MediaProvider.CUSTOM_METADATA_TRACK_SOURCE);
        play(source);
    }

    public void unPause() {

       if (mState == PlaybackState.STATE_PAUSED) {
            // Pause media player and cancel the 'foreground service' state.
            if (mMediaPlayer != null) {
                mMediaPlayer.play();
                mCurrentPosition = mMediaPlayer.getTime();
            }

            // while paused, retain the MediaPlayer but give up audio focus
            //relaxResources(false);
            tryToGetAudioFocus();
        }
        mState = PlaybackState.STATE_PLAYING;
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
        registerAudioNoisyReceiver();
    }

    public void pause() {
        if (mState == PlaybackState.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentPosition = mMediaPlayer.getTime();
            }

            // while paused, retain the MediaPlayer but give up audio focus
            //relaxResources(false);
            giveUpAudioFocus();
        }
        mState = PlaybackState.STATE_PAUSED;
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
        unregisterAudioNoisyReceiver();
    }

    public void seek(long positionMs) {
        logger.Debug("seekTo called with ", positionMs);

        if (mMediaPlayer == null) {
            // If we do not have a current media player, simply update the current position
            mCurrentPosition = positionMs;
        } else {
            if (mMediaPlayer.isPlaying()) {
                mState = PlaybackState.STATE_BUFFERING;
            }
            mMediaPlayer.setTime(positionMs);
            if (mCallback != null) {
                mCallback.onPlaybackStatusChanged(mState);
            }
        }
    }

    public void setCallback(IPlaybackCallback callback) {
        this.mCallback = callback;
    }

    /**
     * Try to get the system audio focus.
     */
    private void tryToGetAudioFocus() {
        logger.Debug("tryToGetAudioFocus");
        if (mAudioFocus != AUDIO_FOCUSED) {
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_FOCUSED;
            }
        }
    }

    /**
     * Give up the audio focus.
     */
    private void giveUpAudioFocus() {
        logger.Debug("giveUpAudioFocus");
        if (mAudioFocus == AUDIO_FOCUSED) {
            if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
            }
        }
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and
     * starts/restarts it. This method starts/restarts the MediaPlayer
     * respecting the current audio focus state. So if we have focus, it will
     * play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is
     * allowed by the current focus settings. This method assumes mPlayer !=
     * null, so if you are calling it, you have to do so from a context where
     * you are sure this is the case.
     */
    private void configMediaPlayerState() {
        logger.Debug("configMediaPlayerState. mAudioFocus=", mAudioFocus);
        if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (mState == PlaybackState.STATE_PLAYING) {
                pause();
            }
        } else {  // we have audio focus:
            if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                mMediaPlayer.setVolume(VOLUME_DUCK); // we'll be relatively quiet
            } else {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setVolume(VOLUME_NORMAL); // we can be loud again
                } // else do something for remote client.
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
                    logger.Debug("configMediaPlayerState startMediaPlayer. seeking to ",
                            mCurrentPosition);
                    if (mCurrentPosition == mMediaPlayer.getTime()) {
                        mMediaPlayer.play();
                        mState = PlaybackState.STATE_PLAYING;
                    } else {
                        mMediaPlayer.setTime(mCurrentPosition);
                        mState = PlaybackState.STATE_BUFFERING;
                    }
                }
                mPlayOnFocusGain = false;
            }
        }
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    /**
     * Called by AudioManager on audio focus changes.
     * Implementation of {@link android.media.AudioManager.OnAudioFocusChangeListener}
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        logger.Debug("onAudioFocusChange. focusChange=", focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            mAudioFocus = AUDIO_FOCUSED;

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            mAudioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

            // If we are playing, we need to reset media player by calling configMediaPlayerState
            // with mAudioFocus properly set.
            if (mState == PlaybackState.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        } else {
            logger.Error("onAudioFocusChange: Ignoring unsupported focusChange: %s", focusChange);
        }
        configMediaPlayerState();
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private void createMediaPlayerIfNeeded() {

        logger.Debug("createMediaPlayerIfNeeded. needed? ", (mMediaPlayer == null));
        if (mMediaPlayer == null) {
            mMediaPlayer = newMediaPlayer(mService.getApplicationContext());
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     *            be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        logger.Debug("relaxResources. releaseMediaPlayer=", releaseMediaPlayer);

        mService.stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer) {
            if (mMediaPlayer != null){
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }

        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mService.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mService.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }

    private static class VlcServiceEventHandler extends VlcEventHandler {

        private long lastReportTime;
        private Playback playback;

        public VlcServiceEventHandler(ILogger logger, MediaPlayer mLibVLC, Playback playback) {
            super(logger, mLibVLC);
            this.playback = playback;
        }

        @Override
        public void onEvent(org.videolan.libvlc.MediaPlayer.Event event) {

            super.onEvent(event);

            switch (event.type) {
                case MediaPlayer.Event.Playing:

                    playback.mCallback.onPlaybackStatusChanged(PlaybackState.STATE_PLAYING);
                    break;
                case MediaPlayer.Event.Paused:
                    playback.mCallback.onPlaybackStatusChanged(PlaybackState.STATE_PAUSED);
                    break;
                case MediaPlayer.Event.Stopped:
                    playback.mCallback.onPlaybackStatusChanged(PlaybackState.STATE_STOPPED);
                    break;
                case MediaPlayer.Event.EndReached:
                    if (playback.mCallback != null) {
                        playback.mCallback.onPlaybackCompletion();
                    }
                    break;
                case MediaPlayer.Event.Vout:
                    break;
                case MediaPlayer.Event.PositionChanged:
                    break;
                case MediaPlayer.Event.EncounteredError:
                    /*service.showToast(service.getString(
                            R.string.invalid_location,
                            service.mLibVLC.getMediaList().getMRL(
                                    service.mCurrentIndex)), Toast.LENGTH_SHORT);*/
                    if (playback.mCallback != null) {
                        playback.mCallback.onPlaybackError("Vlc reported an error");
                    }
                    break;
                case MediaPlayer.Event.TimeChanged:

                    // Avoid overly aggressive reporting
                    if ((new Date().getTime() - lastReportTime) < 5000){
                        return;
                    }

                    lastReportTime = new Date().getTime();
                    playback.mCurrentPosition = mLibVLC.getTime();
                    playback.mCallback.onPlaybackStatusChanged(PlaybackState.STATE_PLAYING);
                    break;
                default:
                    break;
            }
        }
    };
}