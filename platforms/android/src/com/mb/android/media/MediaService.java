package com.mb.android.media;

import java.util.Objects;
import java.util.Random;
import java.util.Stack;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.LibVlcUtil;
import org.videolan.libvlc.Media;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mb.android.MainActivity;
import com.mb.android.api.ApiClientBridge;
import com.mb.android.webviews.IWebView;

import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.logging.ConsoleLogger;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;

import android.media.session.MediaSession;

public class MediaService extends Service implements IMediaService {

    private static final String TAG = "VLC/AudioService";

    public static final String SLEEP_INTENT = "com.mb.android.sleep";
    public static final String INCOMING_CALL_INTENT = "com.mb.android.media.incomingcall";
    public static final String CALL_ENDED_INTENT = "com.mb.android.media.callended";

    private LibVLC mLibVLC;
    private OnAudioFocusChangeListener audioFocusListener;
    private boolean mDetectHeadset = true;
    private PowerManager.WakeLock mWakeLock;
    private ILogger logger;
    private MediaSession mSession;
    private IJsonSerializer jsonSerializer = new GsonJsonSerializer();

    private static boolean mWasPlayingAudio = false;

    // Index management
    /**
     * Stack of previously played indexes, used in shuffle mode
     */
    private Stack<Integer> mPrevious;

    // Playback management
    private boolean mShuffling = false;
    private Random mRandom = null; // Used in shuffling process

    private MediaNotificationManager mMediaNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        logger = new ConsoleLogger();

        // Get libVLC instance
        try {
            mLibVLC = getLibVlcInstance();

            mVlcEventHandler = new AudioServiceEventHandler(logger, mLibVLC, this);

            EventHandler.getInstance().addHandler(mVlcEventHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }

        mPrevious = new Stack<Integer>();

        // Make sure the audio player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        IntentFilter filter = new IntentFilter();
        filter.setPriority(Integer.MAX_VALUE);
        filter.addAction(Constants.ACTION_PLAYPAUSE);
        filter.addAction(Constants.ACTION_PLAY);
        filter.addAction(Constants.ACTION_PAUSE);
        filter.addAction(Constants.ACTION_UNPAUSE);
        filter.addAction(Constants.ACTION_STOP);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        filter.addAction(SLEEP_INTENT);
        filter.addAction(INCOMING_CALL_INTENT);
        filter.addAction(CALL_ENDED_INTENT);
        registerReceiver(serviceReceiver, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Start a new MediaSession
            mSession = new MediaSession(this, "MediaService");
            mSession.setCallback(new MediaSessionCallback());
            mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        }

        mMediaNotificationManager = new MediaNotificationManager(this, this, GetHttpClient());
    }

    private VolleyHttpClient GetHttpClient() {

        VolleyHttpClient httpClient = null;
        if (ApiClientBridge.Current != null) {
            httpClient = ApiClientBridge.Current.httpClient;
        }

        if (httpClient == null) {
            httpClient = new VolleyHttpClient(new ConsoleLogger(), getApplicationContext());
        }

        return httpClient;
    }

    private LibVLC getLibVlcInstance() {

        if (mLibVLC == null) {
            mLibVLC = new LibVLC();
            try {
                mLibVLC.init(this);
            } catch(LibVlcException e) {
                e.printStackTrace();
                return null;
            }
        }

        return mLibVLC;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
            return START_STICKY;

        handleIntent(this, intent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        if (mWakeLock.isHeld())
            mWakeLock.release();
        unregisterReceiver(serviceReceiver);
        EventHandler.getInstance().removeHandler(mVlcEventHandler);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mSession != null){
                mSession.release();
            }
        }

        if (mLibVLC != null){
            mLibVLC.destroy();
            mLibVLC = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private void changeAudioFocus(boolean gain) {
        if (!LibVlcUtil.isFroyoOrLater()) // NOP if not supported
            return;

        if (audioFocusListener == null) {
            audioFocusListener = new OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    LibVLC libVLC = getLibVlcInstance();
                    switch (focusChange)
                    {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            if (libVLC.isPlaying())
                                libVLC.pause();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            /*
                             * Lower the volume to 36% to "duck" when an alert or something
                             * needs to be played.
                             */
                            libVLC.setVolume(36);
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                        case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                        case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                            libVLC.setVolume(100);
                            break;
                    }
                }
            };
        }

        AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
        if(gain)
            am.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        else
            am.abandonAudioFocus(audioFocusListener);

    }

    @Override
    public Parcelable getSessionToken() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mSession.getSessionToken();
        }

        return null;
    }

    @Override
    public Class<?> getServiceClass() {
        return MediaService.class;
    }

    private void handlePauseCommand() {
        if (mLibVLC.isPlaying() && hasCurrentMedia())
            pause();
    }

    private void handleUnpauseCommand() {
        if (!mLibVLC.isPlaying() && hasCurrentMedia())
            play();
    }

    private void handleStopCommand() {
        stop();
    }

    private void handleNextTrackCommand() {
        MainActivity.RespondToWebView("MediaController.nextTrack();");
    }

    private void handlePreviousTrackCommand() {
        MainActivity.RespondToWebView("MediaController.previousTrack();");
    }

    private void handleSeekCommand(long positionMs) {

    }

    private void handleIntent(Context context, Intent intent) {

        String action = intent.getAction();
        int state = intent.getIntExtra("state", 0);
        if( mLibVLC == null ) {
            Log.w(TAG, "Intent received, but VLC is not loaded, skipping.");
            return;
        }

            /*
             * Incoming Call : Pause if VLC is playing audio or video.
             */
        if (action.equalsIgnoreCase(INCOMING_CALL_INTENT)) {
            mWasPlayingAudio = mLibVLC.isPlaying() && mLibVLC.getVideoTracksCount() < 1;
            if (mLibVLC.isPlaying())
                pause();
        }

            /*
             * Call ended : Play only if VLC was playing audio.
             */
        if (action.equalsIgnoreCase(CALL_ENDED_INTENT)
                && mWasPlayingAudio) {
            play();
        }

        // skip all headsets events if there is a call
        TelephonyManager telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telManager != null && telManager.getCallState() != TelephonyManager.CALL_STATE_IDLE)
            return;

            /*
             * Remote / headset control events
             */
        if (action.equalsIgnoreCase(Constants.ACTION_PLAYPAUSE)) {
            if (mLibVLC.isPlaying() && hasCurrentMedia())
                handlePauseCommand();
            else if (!mLibVLC.isPlaying() && hasCurrentMedia())
                handleUnpauseCommand();
        } else if (action.equalsIgnoreCase(Constants.ACTION_UNPAUSE)) {
            handleUnpauseCommand();
        } else if (action.equalsIgnoreCase(Constants.ACTION_PAUSE)) {
            handlePauseCommand();
        } else if (action.equalsIgnoreCase(Constants.ACTION_STOP)) {
            handleStopCommand();

        } else if (action.equalsIgnoreCase(Constants.ACTION_PLAY)) {
            play(context, intent);
        }
        else if (action.equalsIgnoreCase(Constants.ACTION_PREVIOUS)) {
            handlePreviousTrackCommand();
        } else if (action.equalsIgnoreCase(Constants.ACTION_NEXT)) {
            handleNextTrackCommand();
        }

            /*
             * headset plug events
             */
        if (mDetectHeadset) {
            if (action.equalsIgnoreCase(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                Log.i(TAG, "Headset Removed.");
                if (mLibVLC.isPlaying() && hasCurrentMedia())
                    pause();
            }
            else if (action.equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG) && state != 0) {
                Log.i(TAG, "Headset Inserted.");
                if (!mLibVLC.isPlaying() && hasCurrentMedia())
                    play();
            }
        }

            /*
             * Sleep
             */
        if (action.equalsIgnoreCase(SLEEP_INTENT)) {
            stop();
        }
    }

    private void play(Context context, Intent intent) {

        String path = intent.getStringExtra("path");
        String itemJson = intent.getStringExtra("item");
        String mediaSourceJson = intent.getStringExtra("mediaSource");
        final String posterUrl = intent.getStringExtra("posterUrl");

        final BaseItemDto item = jsonSerializer.DeserializeFromString(itemJson, BaseItemDto.class);

        Media media = new Media(mLibVLC, path);
        mLibVLC.playMRL(media.getMrl());
        currentMrl = media.getMrl();

        if (posterUrl == null || posterUrl.length() == 0) {
            setMediaSessionInfo(item, posterUrl, null);
            return;
        }

        GetHttpClient().getBitmap(posterUrl, new Response<Bitmap>() {

            @Override
            public void onResponse(Bitmap bitmap) {
                setMediaSessionInfo(item, posterUrl, bitmap);
            }

            @Override
            public void onError(Exception ex) {

                setMediaSessionInfo(item, posterUrl, null);
            }
        });
    }

    private void setMediaSessionInfo(BaseItemDto item, String posterUrl, Bitmap largeIcon) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            mSession.setActive(true);

            String album = item.getAlbum() == null ? "" : item.getAlbum();
            String artist = item.getArtistItems().size() > 0 ? item.getArtistItems().get(0).getName() : "";
            String title = item.getName() == null ? "" : item.getName();
            String itemId = item.getId();

            MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                    .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, itemId);

            if (posterUrl != null && posterUrl.length() > 0) {
                metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, posterUrl);
            }

            if (largeIcon != null) {
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, largeIcon);
            }

            mSession.setMetadata(metadataBuilder.build());
        }
    }

    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleIntent(context, intent);
        }
    };

    /**
     * Handle libvlc asynchronous events
     */
    private  Handler mVlcEventHandler;

    private static class AudioServiceEventHandler extends VlcEventHandler {

        private MediaService audioService;

        public AudioServiceEventHandler(ILogger logger, LibVLC mLibVLC, MediaService audioService) {
            super(logger, mLibVLC);
            this.audioService = audioService;
        }

        @Override
        public void handleMessage(Message msg) {

            super.handleMessage(msg);

            MediaService service = audioService;
            if(service == null) return;

            int event = msg.getData().getInt("event");

            switch (event) {
                case EventHandler.MediaParsedChanged:
                    Log.i(TAG, "MediaParsedChanged");
                    break;
                case EventHandler.MediaPlayerPlaying:
                    Log.i(TAG, "MediaPlayerPlaying");
                    service.executeUpdate();
                    service.executeUpdateProgress();

                    service.changeAudioFocus(true);
                    service.showNotification();
                    if (!service.mWakeLock.isHeld())
                        service.mWakeLock.acquire();
                    break;
                case EventHandler.MediaPlayerPaused:
                    Log.i(TAG, "MediaPlayerPaused");
                    service.executeUpdate();
                    service.executeUpdateProgress();
                    service.showNotification();
                    if (service.mWakeLock.isHeld())
                        service.mWakeLock.release();
                    break;
                case EventHandler.MediaPlayerStopped:
                    Log.i(TAG, "MediaPlayerStopped");
                    service.currentMrl = null;
                    service.executeUpdate();
                    service.executeUpdateProgress();
                    if (service.mWakeLock.isHeld())
                        service.mWakeLock.release();
                    break;
                case EventHandler.MediaPlayerEndReached:
                    Log.i(TAG, "MediaPlayerEndReached");
                    service.currentMrl = null;
                    service.executeUpdate();
                    service.executeUpdateProgress();

                    if (service.mWakeLock.isHeld())
                        service.mWakeLock.release();
                    break;
                case EventHandler.MediaPlayerVout:
                    if(msg.getData().getInt("data") > 0) {
                        service.handleVout();
                    }
                    break;
                case EventHandler.MediaPlayerPositionChanged:
                    float pos = msg.getData().getFloat("data");
                    break;
                case EventHandler.MediaPlayerEncounteredError:
                    /*service.showToast(service.getString(
                            R.string.invalid_location,
                            service.mLibVLC.getMediaList().getMRL(
                                    service.mCurrentIndex)), Toast.LENGTH_SHORT);*/
                    service.currentMrl = null;
                    service.executeUpdate();
                    service.executeUpdateProgress();
                    if (service.mWakeLock.isHeld())
                        service.mWakeLock.release();
                    break;
                case EventHandler.MediaPlayerTimeChanged:
                    // avoid useless error logs
                    break;
                default:
                    Log.e(TAG, String.format("Event not handled (0x%x)", msg.getData().getInt("event")));
                    break;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                PlaybackState.Builder stateBuilder = new PlaybackState.Builder();
                stateBuilder.setActiveQueueItemId(MediaSession.QueueItem.UNKNOWN_ID);
                long actions = PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_STOP | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SEEK_TO | PlaybackState.ACTION_SET_RATING;
                stateBuilder.setActions(actions);

                int playerState = service.mLibVLC.getPlayerState();

                // Expected states by web plugins are: IDLE/CLOSE=0, OPENING=1, BUFFERING=2, PLAYING=3, PAUSED=4, STOPPING=5, ENDED=6, ERROR=7
                boolean isPaused = event == EventHandler.MediaPlayerStopped ?
                        false :
                        event == EventHandler.MediaPlayerPaused || playerState == 4;

                long length = service.mLibVLC.getLength();
                long time = service.mLibVLC.getTime();

                if (isPaused) {
                    stateBuilder.setState(PlaybackState.STATE_PAUSED, time, 1.0f);
                } else {
                    stateBuilder.setState(PlaybackState.STATE_PLAYING, time, 1.0f);
                }

                service.mSession.setPlaybackState(stateBuilder.build());
            }
        }
    };

    private void handleVout() {

        if (!hasCurrentMedia())
            return;
        Log.i(TAG, "Obtained video track");

        //mEventHandler.removeHandler(mVlcEventHandler);
        // Preserve playback when switching to video
        hideNotification(false);

        // Switch to the video player & don't lose the currently playing stream
        //VideoPlayerActivity.start(VLCApplication.getAppContext(), MRL, title, index, true);
    }

    private void executeUpdate() {

    }

    private void executeUpdateProgress() {

    }

    private void showNotification() {

        mMediaNotificationManager.startNotification();
    }

    private void hideNotification() {
        hideNotification(true);
    }

    /**
     * Hides the VLC notification and stops the service.
     *
     * @param stopPlayback True to also stop playback at the same time. Set to false to preserve playback (e.g. for vout events)
     */
    private void hideNotification(boolean stopPlayback) {

        mMediaNotificationManager.stopNotification();
        if(stopPlayback)
            stopSelf();
    }

    private String currentMrl;
    /**
     * Alias for mCurrentIndex >= 0
     *
     * @return True if a media is currently loaded, false otherwise
     */
    private boolean hasCurrentMedia() {

        // Expected states by web plugins are: IDLE/CLOSE=0, OPENING=1, BUFFERING=2, PLAYING=3, PAUSED=4, STOPPING=5, ENDED=6, ERROR=7

        return currentMrl != null && currentMrl.length() > 0;
    }

    private void pause() {
        mLibVLC.pause();
    }

    private void play() {
        if(hasCurrentMedia()) {
            mLibVLC.play();
            showNotification();
        }
    }

    private void stop() {
        mLibVLC.stop();

        mPrevious.clear();
        hideNotification();
        executeUpdate();
        executeUpdateProgress();
        changeAudioFocus(false);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private final class MediaSessionCallback extends MediaSession.Callback {
        @Override
        public void onPlay() {
            handleUnpauseCommand();
        }

        @Override
        public void onSeekTo(long position) {
            handleSeekCommand(position);
        }

        @Override
        public void onPause() {
            handlePauseCommand();
        }

        @Override
        public void onStop() {
            handleStopCommand();
        }

        @Override
        public void onSkipToNext() {
            handleNextTrackCommand();
        }

        @Override
        public void onSkipToPrevious() {
            handlePreviousTrackCommand();
        }
    }
}