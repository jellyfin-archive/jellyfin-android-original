package com.mb.android.media;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.PlaybackState;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;

import com.mb.android.media.provider.MusicProvider;
import com.mb.android.media.utils.MediaIDHelper;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.Media;

import java.io.IOException;
import java.util.Date;

import mediabrowser.model.logging.ILogger;

import static android.media.MediaPlayer.OnPreparedListener;
import static android.media.session.MediaSession.QueueItem;

/**
 * A class that implements local media playback using {@link android.media.MediaPlayer}
 */
@TargetApi(14)
public class Playback implements AudioManager.OnAudioFocusChangeListener {

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
    private Callback mCallback;
    private MusicProvider mMusicProvider;
    private volatile boolean mAudioNoisyReceiverRegistered;
    private volatile long mCurrentPosition;
    private volatile String mCurrentMediaId;

    private PowerManager.WakeLock mWakeLock;

    // Type of audio focus we have:
    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private AudioManager mAudioManager;
    private LibVLC mLibVLC;

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

    public Playback(MediaService service, MusicProvider musicProvider, ILogger logger) {
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

    public void start() {
    }

    public void stop(boolean notifyListeners) {
        mState = PlaybackState.STATE_STOPPED;
        if (notifyListeners && mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
        mCurrentPosition = getCurrentStreamPosition();
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

    public boolean isConnected() {
        return true;
    }

    public boolean isPlaying() {
        return mPlayOnFocusGain || (mLibVLC != null && mLibVLC.isPlaying());
    }

    public long getCurrentStreamPosition() {
        return mLibVLC != null ?
                mLibVLC.getTime() : mCurrentPosition;
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

        if (mState == PlaybackState.STATE_PAUSED && !mediaHasChanged && getLibVlcInstance() != null) {
            configMediaPlayerState();
        } else {
            mState = PlaybackState.STATE_STOPPED;
            relaxResources(false); // release everything except MediaPlayer

            try {
                createMediaPlayerIfNeeded();

                mState = PlaybackState.STATE_BUFFERING;

                Media media = new Media(getLibVlcInstance(), source);

                getLibVlcInstance().playMRL(media.getMrl());

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
                    mCallback.onError(ex.getMessage());
                }
            }
        }
    }

    @TargetApi(21)
    public void play(QueueItem item) {
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        String mediaId = item.getDescription().getMediaId();
        boolean mediaHasChanged = !TextUtils.equals(mediaId, mCurrentMediaId);
        if (mediaHasChanged) {
            mCurrentPosition = 0;
            mCurrentMediaId = mediaId;
        }

        if (mState == PlaybackState.STATE_PAUSED && !mediaHasChanged && getLibVlcInstance() != null) {
            configMediaPlayerState();
        } else {
            mState = PlaybackState.STATE_STOPPED;
            relaxResources(false); // release everything except MediaPlayer
            MediaMetadata track = mMusicProvider.getMusic(
                    MediaIDHelper.extractMusicIDFromMediaID(item.getDescription().getMediaId()));

            String source = track.getString(MusicProvider.CUSTOM_METADATA_TRACK_SOURCE);

            try {
                createMediaPlayerIfNeeded();

                mState = PlaybackState.STATE_BUFFERING;

                logger.Debug("Vlc playing source %s");

                Media media = new Media(getLibVlcInstance(), source);

                getLibVlcInstance().playMRL(media.getMrl());

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
                    mCallback.onError(ex.getMessage());
                }
            }
        }
    }

    public void unpause() {

       if (mState == PlaybackState.STATE_PAUSED) {
            // Pause media player and cancel the 'foreground service' state.
            if (mLibVLC != null) {
                mLibVLC.play();
                mCurrentPosition = mLibVLC.getTime();
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
            if (mLibVLC != null && mLibVLC.isPlaying()) {
                mLibVLC.pause();
                mCurrentPosition = mLibVLC.getTime();
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

    public void seekTo(int position) {
        logger.Debug("seekTo called with ", position);

        if (mLibVLC == null) {
            // If we do not have a current media player, simply update the current position
            mCurrentPosition = position;
        } else {
            if (mLibVLC.isPlaying()) {
                mState = PlaybackState.STATE_BUFFERING;
            }
            mLibVLC.setTime(position);
            if (mCallback != null) {
                mCallback.onPlaybackStatusChanged(mState);
            }
        }
    }

    public void setCallback(Callback callback) {
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
                mLibVLC.setVolume(VOLUME_DUCK); // we'll be relatively quiet
            } else {
                if (mLibVLC != null) {
                    mLibVLC.setVolume(VOLUME_NORMAL); // we can be loud again
                } // else do something for remote client.
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (mLibVLC != null && !mLibVLC.isPlaying()) {
                    logger.Debug("configMediaPlayerState startMediaPlayer. seeking to ",
                            mCurrentPosition);
                    if (mCurrentPosition == mLibVLC.getTime()) {
                        mLibVLC.play();
                        mState = PlaybackState.STATE_PLAYING;
                    } else {
                        mLibVLC.setTime(mCurrentPosition);
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

        logger.Debug("createMediaPlayerIfNeeded. needed? ", (mLibVLC == null));
        if (mLibVLC == null) {
            mLibVLC = getLibVlcInstance();
        }
    }

    private LibVLC getLibVlcInstance() {

        if (mLibVLC == null) {
            mLibVLC = new LibVLC();
            try {
                mLibVLC.init(mService);
                mVlcEventHandler = new VlcServiceEventHandler(logger, mLibVLC, this);
                EventHandler.getInstance().addHandler(mVlcEventHandler);
            } catch(LibVlcException e) {
                e.printStackTrace();
                return null;
            }
        }

        return mLibVLC;
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
            if (mLibVLC != null){
                EventHandler.getInstance().removeHandler(mVlcEventHandler);
                mLibVLC.destroy();
                mLibVLC = null;
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

    /**
     * Handle libvlc asynchronous events
     */
    private Handler mVlcEventHandler;

    private static class VlcServiceEventHandler extends VlcEventHandler {

        private long lastReportTime;
        private Playback playback;

        public VlcServiceEventHandler(ILogger logger, LibVLC mLibVLC, Playback playback) {
            super(logger, mLibVLC);
            this.playback = playback;
        }

        @Override
        public void handleMessage(Message msg) {

            super.handleMessage(msg);

            int event = msg.getData().getInt("event");

            switch (event) {
                case EventHandler.MediaParsedChanged:
                    break;
                case EventHandler.MediaPlayerPlaying:

                    playback.mCallback.onPlaybackStatusChanged(PlaybackState.STATE_PLAYING);
                    break;
                case EventHandler.MediaPlayerPaused:
                    playback.mCallback.onPlaybackStatusChanged(PlaybackState.STATE_PAUSED);
                    break;
                case EventHandler.MediaPlayerStopped:
                    playback.mCallback.onPlaybackStatusChanged(PlaybackState.STATE_STOPPED);
                    break;
                case EventHandler.MediaPlayerEndReached:
                    if (playback.mCallback != null) {
                        playback.mCallback.onCompletion();
                    }
                    break;
                case EventHandler.MediaPlayerVout:
                    if(msg.getData().getInt("data") > 0) {
                        //service.handleVout();
                    }
                    break;
                case EventHandler.MediaPlayerPositionChanged:
                    break;
                case EventHandler.MediaPlayerEncounteredError:
                    /*service.showToast(service.getString(
                            R.string.invalid_location,
                            service.mLibVLC.getMediaList().getMRL(
                                    service.mCurrentIndex)), Toast.LENGTH_SHORT);*/
                    if (playback.mCallback != null) {
                        playback.mCallback.onError("Vlc reported an error");
                    }
                    break;
                case EventHandler.MediaPlayerTimeChanged:

                    // avoid useless error logs
                    if (event == EventHandler.MediaPlayerTimeChanged) {
                        // Avoid overly aggressive reporting
                        if ((new Date().getTime() - lastReportTime) < 600){
                            return;
                        }
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

    interface Callback {
        /**
         * On current music completed.
         */
        void onCompletion();
        /**
         * on Playback status changed
         * Implementations can use this callback to update
         * playback state on the media sessions.
         */
        void onPlaybackStatusChanged(int state);

        /**
         * @param error to be added to the PlaybackState
         */
        void onError(String error);

    }

}