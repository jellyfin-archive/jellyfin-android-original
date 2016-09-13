package com.mb.android.media;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.mb.android.BuildConfig;
import com.mb.android.MainActivity;
import com.mb.android.R;
import com.mb.android.api.ApiClientBridge;
import com.mb.android.logging.AppLogger;
import com.mb.android.media.legacy.RemoteControlClientReceiver;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaList;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.AndroidUtil;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.apiinteraction.android.AndroidApiClient;
import mediabrowser.apiinteraction.android.AndroidDevice;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.android.mediabrowser.Constants;
import mediabrowser.apiinteraction.device.IDevice;
import mediabrowser.model.dlna.DeviceProfile;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.PlaybackProgressInfo;

public class PlaybackService extends Service implements IVLCVout.Callback {

    private static final String TAG = "VLC/PlaybackService";

    private static final int SHOW_PROGRESS = 0;
    private static final int SHOW_TOAST = 1;
    public static final String ACTION_REMOTE_GENERIC =  Strings.buildPkgString("remote.");
    public static final String ACTION_REMOTE_BACKWARD = ACTION_REMOTE_GENERIC+"Backward";
    public static final String ACTION_REMOTE_PLAY = ACTION_REMOTE_GENERIC+"Play";
    public static final String ACTION_REMOTE_PLAYPAUSE = ACTION_REMOTE_GENERIC+"PlayPause";
    public static final String ACTION_REMOTE_PAUSE = ACTION_REMOTE_GENERIC+"Pause";
    public static final String ACTION_REMOTE_STOP = ACTION_REMOTE_GENERIC+"Stop";
    public static final String ACTION_REMOTE_FORWARD = ACTION_REMOTE_GENERIC+"Forward";
    public static final String ACTION_REMOTE_LAST_PLAYLIST = ACTION_REMOTE_GENERIC+"LastPlaylist";
    public static final String ACTION_REMOTE_LAST_VIDEO_PLAYLIST = ACTION_REMOTE_GENERIC+"LastVideoPlaylist";
    public static final String ACTION_REMOTE_SWITCH_VIDEO = ACTION_REMOTE_GENERIC+"SwitchToVideo";
    public static final String ACTION_REMOTE_RESUME_VIDEO = "org.videolan.vlc.remote.ResumeVideo";
    public static final String ACTION_WIDGET_INIT = "org.videolan.vlc.widget.INIT";

    private ILogger logger;
    private IJsonSerializer jsonSerializer = new GsonJsonSerializer();

    // Indicates whether the service was started.
    private boolean mServiceStarted;

    private long TranscodingOffsetPositionTicks = 0;
    private boolean EnableServerSeek = false;
    private long lastReportedPositionTicks = 0;
    private VideoApiHelper apiHelper;

    public interface Callback {
        void update();
        void updateProgress();
        void onMediaEvent(Media.Event event);
        void onMediaPlayerEvent(MediaPlayer.Event event);
    }

    private class LocalBinder extends Binder {
        PlaybackService getService() {
            return PlaybackService.this;
        }
    }
    public static PlaybackService getService(IBinder iBinder) {
        LocalBinder binder = (LocalBinder) iBinder;
        return binder.getService();
    }

    private SharedPreferences mSettings;
    private final IBinder mBinder = new LocalBinder();
    private MediaWrapperList mMediaList = new MediaWrapperList();
    private MediaPlayer mMediaPlayer;
    private boolean mParsed = false;
    private boolean mSeekable = false;
    private boolean mPausable = false;
    private boolean mIsAudioTrack = false;
    private boolean mHasHdmiAudio = false;
    private boolean mSwitchingToVideo = false;
    private boolean mVideoBackground = false;

    final private ArrayList<Callback> mCallbacks = new ArrayList<Callback>();
    private boolean mDetectHeadset = true;
    private PowerManager.WakeLock mWakeLock;
    private final AtomicBoolean mExpanding = new AtomicBoolean(false);

    private static boolean mWasPlayingAudio = false; // used only if readPhoneState returns true

    // Index management
    /**
     * Stack of previously played indexes, used in shuffle mode
     */
    private Stack<Integer> mPrevious;
    private int mCurrentIndex; // Set to -1 if no media is currently loaded
    private int mPrevIndex; // Set to -1 if no previous media
    private int mNextIndex; // Set to -1 if no next media

    // Playback management

    MediaSessionCompat mMediaSession;
    protected MediaSessionCallback mSessionCallback;
    private static final long PLAYBACK_ACTIONS = PlaybackStateCompat.ACTION_PAUSE
            | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_STOP
            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;

    public static final int TYPE_AUDIO = 0;
    public static final int TYPE_VIDEO = 1;

    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_ONE = 1;
    public static final int REPEAT_ALL = 2;
    private boolean mShuffling = false;
    private int mRepeating = REPEAT_NONE;
    private Random mRandom = null; // Used in shuffling process
    private long mSavedTime = 0l;
    private boolean mHasAudioFocus = false;
    // RemoteControlClient-related
    /**
     * RemoteControlClient is for lock screen playback control.
     */
    private RemoteControlClientReceiver mRemoteControlClientReceiver = null;
    /**
     * Last widget position update timestamp
     */
    private long mWidgetPositionTimestamp = Calendar.getInstance().getTimeInMillis();
    private ComponentName mRemoteControlClientReceiverComponent;
    //private PopupManager mPopupManager;

    private static LibVLC LibVLC(Context context, ILogger logger) {
        return VLCInstance.get(context, logger);
    }

    private MediaPlayer newMediaPlayer() {
        final MediaPlayer mp = new MediaPlayer(LibVLC(getApplicationContext(), logger));
        final String aout = VLCOptions.getAout(mSettings, logger);
        if (mp.setAudioOutput(aout) && aout.equals("android_audiotrack")) {
            mIsAudioTrack = true;
            if (mHasHdmiAudio)
                mp.setAudioOutputDevice("hdmi");
        } else
            mIsAudioTrack = false;
        mp.getVLCVout().addCallback(this);

        return mp;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        logger = AppLogger.getLogger(getApplicationContext());

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mMediaPlayer = newMediaPlayer();
        mMediaPlayer.setEqualizer(VLCOptions.getEqualizer(this));

        if (!VLCInstance.testCompatibleCPU(this)) {
            stopSelf();
            return;
        }

        if (!AndroidDevices.hasTsp() && !AndroidDevices.hasPlayServices(getApplicationContext()))
            AndroidDevices.setRemoteControlReceiverEnabled(getApplicationContext(), true);

        mDetectHeadset = mSettings.getBoolean("enable_headset_detection", true);

        mCurrentIndex = -1;
        mPrevIndex = -1;
        mNextIndex = -1;
        mPrevious = new Stack<Integer>();
        mRemoteControlClientReceiverComponent = new ComponentName(BuildConfig.APPLICATION_ID,
                RemoteControlClientReceiver.class.getName());

        // Make sure the audio player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        IntentFilter filter = new IntentFilter();
        filter.setPriority(Integer.MAX_VALUE);
        filter.addAction(Constants.ACTION_PREVIOUS);
        filter.addAction(Constants.ACTION_PLAYPAUSE);
        filter.addAction(Constants.ACTION_PLAY);
        filter.addAction(Constants.ACTION_PAUSE);
        filter.addAction(Constants.ACTION_STOP);
        filter.addAction(Constants.ACTION_NEXT);
        filter.addAction(ACTION_REMOTE_LAST_PLAYLIST);
        filter.addAction(ACTION_REMOTE_RESUME_VIDEO);
        filter.addAction(ACTION_WIDGET_INIT);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        filter.addAction(Constants.SLEEP_INTENT);

        registerReceiver(mReceiver, filter);
        registerV21();

        boolean stealRemoteControl = mSettings.getBoolean("enable_steal_remote_control", false);

        if (stealRemoteControl) {
            /* Backward compatibility for API 7 */
            filter = new IntentFilter();
            if (stealRemoteControl)
                filter.setPriority(Integer.MAX_VALUE);
            filter.addAction(Intent.ACTION_MEDIA_BUTTON);
            mRemoteControlClientReceiver = new RemoteControlClientReceiver();
            registerReceiver(mRemoteControlClientReceiver, filter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
            return START_STICKY;
        /*if(ACTION_REMOTE_PLAYPAUSE.equals(intent.getAction())){
            if (hasCurrentMedia())
                return START_STICKY;
            else
                loadLastPlaylist(TYPE_AUDIO);
        } else if (ACTION_REMOTE_PLAY.equals(intent.getAction())) {
            if (hasCurrentMedia())
                play();
            else
                loadLastPlaylist(TYPE_AUDIO);
        }*/
        handleIntent(this, intent);
        updateWidget();
        return super.onStartCommand(intent, flags, startId);
    }

    private void handleIntent(Context context, Intent intent) {

        String action = intent.getAction();
        if (action == null) {
            action = "";
        }

        int state = intent.getIntExtra("state", 0);
        if( mMediaPlayer == null ) {
            logger.Warn("Intent received, but VLC is not loaded, skipping.");
            return;
        }

        // skip all headsets events if there is a call
        TelephonyManager telManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (telManager != null && telManager.getCallState() != TelephonyManager.CALL_STATE_IDLE)
            return;

            /*
             * Launch the activity if needed
             */
        if (action.startsWith(ACTION_REMOTE_GENERIC) && !mMediaPlayer.isPlaying() && !hasCurrentMedia()) {
            Intent iVlc = new Intent(context, MainActivity.class);
            iVlc.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(iVlc);
        }

            /*
             * Remote / headset control events
             */
        if (action.equalsIgnoreCase(Constants.ACTION_PLAYPAUSE)) {
            if (mMediaPlayer.isPlaying() && hasCurrentMedia())
                pause();
            else if (!mMediaPlayer.isPlaying() && hasCurrentMedia())
                play();
        } else if (action.equalsIgnoreCase(Constants.ACTION_UNPAUSE)) {
            play();
        } else if (action.equalsIgnoreCase(Constants.ACTION_PLAY)) {
            play(context, intent);
        } else if (action.equalsIgnoreCase(Constants.ACTION_PAUSE)) {
            if (mMediaPlayer.isPlaying() && hasCurrentMedia())
                pause();
        } else if (action.equalsIgnoreCase(Constants.ACTION_PREVIOUS)) {
            previous();
        } else if (action.equalsIgnoreCase(Constants.ACTION_STOP)) {
            stop();
        } else if (action.equalsIgnoreCase(Constants.ACTION_NEXT)) {
            next();
        }else if (action.equalsIgnoreCase(ACTION_REMOTE_LAST_PLAYLIST)) {
            loadLastPlaylist(TYPE_AUDIO);
        } else if (action.equalsIgnoreCase(ACTION_REMOTE_LAST_VIDEO_PLAYLIST)) {
            loadLastPlaylist(TYPE_VIDEO);
        } else if (action.equalsIgnoreCase(ACTION_REMOTE_SWITCH_VIDEO)) {
            removePopup();
            if (hasMedia()) {
                getCurrentMediaWrapper().removeFlags(MediaWrapper.MEDIA_FORCE_AUDIO);
                switchToVideo();
            }
        } else if (action.equalsIgnoreCase(ACTION_WIDGET_INIT)) {
            updateWidget();
        }

            /*
             * headset plug events
             */
        if (mDetectHeadset && !mHasHdmiAudio) {
            if (action.equalsIgnoreCase(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                logger.Info("Headset Removed.");
                if (mMediaPlayer.isPlaying() && hasCurrentMedia())
                    pause();
            }
            else if (action.equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG) && state != 0) {
                logger.Info("Headset Inserted.");
                if (!mMediaPlayer.isPlaying() && hasCurrentMedia())
                    play();
            }
        }

            /*
             * Sleep
             */
        if (action.equalsIgnoreCase(Constants.SLEEP_INTENT)) {
            stop();
        }
    }

    private BaseItemDto currentItem;
    private String posterUrl;

    private void destroyCurrentMediaInfo() {
        posterUrl = null;
        currentItem = null;
    }

    private void play(Context context, Intent intent) {

        if (!mServiceStarted) {
            logger.Info("Starting service");
            // The MusicService needs to keep running even after the calling MediaBrowser
            // is disconnected. Call startService(Intent) and then stopSelf(..) when we no longer
            // need to play media.
            startService(new Intent(getApplicationContext(), PlaybackService.class));
            mServiceStarted = true;
        }

        String path = intent.getStringExtra("path");
        String itemJson = intent.getStringExtra("item");
        String mediaSourceJson = intent.getStringExtra("mediaSourceJson");
        posterUrl = intent.getStringExtra("posterUrl");

        currentItem = jsonSerializer.DeserializeFromString(itemJson, BaseItemDto.class);

        MediaSourceInfo mediaSourceInfo = jsonSerializer.DeserializeFromString(mediaSourceJson, MediaSourceInfo.class);

        String deviceProfileJson = intent.getStringExtra("deviceProfileJson");
        DeviceProfile deviceProfile = jsonSerializer.DeserializeFromString(deviceProfileJson, DeviceProfile.class);

        String apiAppName = intent.getStringExtra("appName");
        String apiAppVersion = intent.getStringExtra("appVersion");
        String apiDeviceId = intent.getStringExtra("deviceId");
        String apiDeviceName = intent.getStringExtra("deviceName");
        String apiUserId = intent.getStringExtra("userId");
        String apiAccessToken = intent.getStringExtra("accessToken");
        String apiServerUrl = intent.getStringExtra("serverUrl");
        String apiServerId = intent.getStringExtra("serverId");
        String playbackStartInfoJson = intent.getStringExtra("playbackStartInfoJson");
        //videoQualityOptionsJson = intent.getStringExtra("videoQualityOptionsJson");

        PlaybackProgressInfo playbackStartInfo = jsonSerializer.DeserializeFromString(playbackStartInfoJson, PlaybackProgressInfo.class);

        IDevice device = new AndroidDevice(getApplicationContext(), apiDeviceId, apiDeviceName);
        ApiClient apiClient = new AndroidApiClient(GetHttpClient(), jsonSerializer, logger, apiServerUrl, apiAppName, device, apiAppVersion, new ApiEventListener());
        apiClient.SetAuthenticationInfo(apiAccessToken, apiUserId);

        apiHelper.setInitialInfo(apiServerId, false, apiClient, deviceProfile, playbackStartInfo, mediaSourceInfo);

        final MediaWrapper mw = new MediaWrapper(Uri.parse(path));

        load(mw);
        //switchToVideo();
    }

    public void setApiHelper(VideoApiHelper helper){
        this.apiHelper = helper;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();

        if (!AndroidDevices.hasTsp() && !AndroidDevices.hasPlayServices(getApplicationContext()))
            AndroidDevices.setRemoteControlReceiverEnabled(getApplicationContext(), false);

        if (mWakeLock.isHeld())
            mWakeLock.release();
        unregisterReceiver(mReceiver);
        if (mReceiverV21 != null)
            unregisterReceiver(mReceiverV21);
        if (mRemoteControlClientReceiver != null) {
            unregisterReceiver(mRemoteControlClientReceiver);
            mRemoteControlClientReceiver = null;
        }
        mMediaPlayer.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!hasCurrentMedia())
            stopSelf();
        return true;
    }

    public IVLCVout getVLCVout()  {
        return mMediaPlayer.getVLCVout();
    }

    private final OnAudioFocusChangeListener mAudioFocusListener = createOnAudioFocusChangeListener();

    private OnAudioFocusChangeListener createOnAudioFocusChangeListener() {
        return new OnAudioFocusChangeListener() {
            private boolean mLossTransient = false;
            private int mLossTransientVolume = -1;
            private boolean wasPlaying = false;

            @Override
            public void onAudioFocusChange(int focusChange) {
                /*
                 * Pause playback during alerts and notifications
                 */
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        logger.Info("AUDIOFOCUS_LOSS");
                        // Pause playback
                        changeAudioFocus(false);
                        pause();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        logger.Info("AUDIOFOCUS_LOSS_TRANSIENT");
                        // Pause playback
                        mLossTransient = true;
                        wasPlaying = isPlaying();
                        if (wasPlaying)
                            pause();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        logger.Info("AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                        // Lower the volume
                        if (mMediaPlayer.isPlaying()) {
                            mLossTransientVolume = mMediaPlayer.getVolume();
                            mMediaPlayer.setVolume(36);
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        logger.Info("AUDIOFOCUS_GAIN: " + mLossTransientVolume + ", " + mLossTransient);
                        // Resume playback
                        if (mLossTransientVolume != -1) {
                            mMediaPlayer.setVolume(mLossTransientVolume);
                            mLossTransientVolume = -1;
                        } else if (mLossTransient) {
                            if (wasPlaying)
                                mMediaPlayer.play();
                            mLossTransient = false;
                        }
                        break;
                }
            }
        };
    }

    private void changeAudioFocus(boolean acquire) {
        final AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
        if (am == null)
            return;

        if (acquire) {
            if (!mHasAudioFocus) {
                final int result = am.requestAudioFocus(mAudioFocusListener,
                        AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    am.setParameters("bgm_state=true");
                    mHasAudioFocus = true;
                }
            }
        } else {
            if (mHasAudioFocus) {
                am.abandonAudioFocus(mAudioFocusListener);
                am.setParameters("bgm_state=false");
                mHasAudioFocus = false;
            }
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void registerV21() {
        final IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_HDMI_AUDIO_PLUG);
        registerReceiver(mReceiverV21, intentFilter);
    }

    private final BroadcastReceiver mReceiverV21 = AndroidUtil.isLolliPopOrLater() ? new BroadcastReceiver()
    {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action == null)
                return;
            if (action.equalsIgnoreCase(AudioManager.ACTION_HDMI_AUDIO_PLUG)) {
                mHasHdmiAudio = intent.getIntExtra(AudioManager.EXTRA_AUDIO_PLUG_STATE, 0) == 1;
                if (mMediaPlayer != null && mIsAudioTrack)
                    mMediaPlayer.setAudioOutputDevice(mHasHdmiAudio ? "hdmi" : "stereo");
            }
        }
    } : null;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            handleIntent(context, intent);
        }
    };


    @Override
    public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {
        hideNotification(false);
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {
        mSwitchingToVideo = false;
    }

    private final Media.EventListener mMediaListener = new Media.EventListener() {
        @Override
        public void onEvent(Media.Event event) {
            boolean update = true;
            switch (event.type) {
                case Media.Event.MetaChanged:
                    /* Update Meta if file is already parsed */
                    if (mParsed && updateCurrentMeta(event.getMetaId()))
                        executeUpdate();
                    logger.Info("Media.Event.MetaChanged: " + event.getMetaId());
                    break;
                case Media.Event.ParsedChanged:
                    logger.Info("Media.Event.ParsedChanged");
                    updateCurrentMeta(-1);
                    mParsed = true;
                    break;
                default:
                    update = false;

            }
            if (update) {
                for (Callback callback : mCallbacks)
                    callback.onMediaEvent(event);
                if (mParsed && mMediaSession != null)
                    showNotification();
            }
        }
    };

    /**
     * Update current media meta and return true if player needs to be updated
     *
     * @param id of the Meta event received, -1 for none
     * @return true if UI needs to be updated
     */
    private boolean updateCurrentMeta(int id) {
        if (id == Media.Meta.Publisher)
            return false;
        final MediaWrapper mw = getCurrentMedia();
        if (mw != null)
            mw.updateMeta(mMediaPlayer);
        return id != Media.Meta.NowPlaying || getCurrentMedia().getNowPlaying() != null;
    }

    private final MediaPlayer.EventListener mMediaPlayerListener = new MediaPlayer.EventListener() {

        private Timer timer;
        private long lastReportTime;

        //KeyguardManager keyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);

        @Override
        public void onEvent(MediaPlayer.Event event) {
            switch (event.type) {
                case MediaPlayer.Event.Playing:
                    if(mSavedTime != 0l)
                        seek(mSavedTime);
                    mSavedTime = 0l;

                    logger.Info("MediaPlayer.Event.Playing");
                    executeUpdate();
                    publishState(event.type);
                    executeUpdateProgress();

                    /*final MediaWrapper mw = mMediaList.getMedia(mCurrentIndex);
                    if (mw != null) {
                        long length = mMediaPlayer.getLength();
                        MediaDatabase dbManager = MediaDatabase.getInstance();
                        MediaWrapper m = dbManager.getMedia(mw.getUri());
                        *//**
                         * 1) There is a media to update
                         * 2) It has a length of 0
                         * (dynamic track loading - most notably the OGG container)
                         * 3) We were able to get a length even after parsing
                         * (don't want to replace a 0 with a 0)
                         *//*
                        if (m != null && m.getLength() == 0 && length > 0) {
                            dbManager.updateMedia(mw.getUri(),
                                    MediaDatabase.INDEX_MEDIA_LENGTH, length);
                        }
                    }*/

                    changeAudioFocus(true);
                    if (!mWakeLock.isHeld())
                        mWakeLock.acquire();
                    if (/*!keyguardManager.inKeyguardRestrictedInputMode() &&*/ !mVideoBackground && switchToVideo())
                        hideNotification(false);
                    else
                        showNotification();
                    mVideoBackground = false;
                    startTimer();
                    break;
                case MediaPlayer.Event.Paused:
                    logger.Info("MediaPlayer.Event.Paused");
                    executeUpdate();
                    publishState(event.type);
                    executeUpdateProgress();
                    showNotification();
                    if (mWakeLock.isHeld())
                        mWakeLock.release();
                    reportState("paused", false);
                    break;
                case MediaPlayer.Event.Stopped:
                    logger.Info("MediaPlayer.Event.Stopped");
                    stopTimer();
                    executeUpdate();
                    publishState(event.type);
                    executeUpdateProgress();
                    if (mWakeLock.isHeld())
                        mWakeLock.release();
                    changeAudioFocus(false);
                    break;
                case MediaPlayer.Event.EndReached:
                    logger.Info("MediaPlayer.Event.EndReached");
                    stopTimer();
                    executeUpdateProgress();
                    determinePrevAndNextIndices(true);
                    next();
                    if (mWakeLock.isHeld())
                        mWakeLock.release();
                    changeAudioFocus(false);
                    break;
                case MediaPlayer.Event.EncounteredError:
                    stopTimer();
                    showToast(getString(
                            R.string.invalid_location,
                            mMediaList.getMRL(mCurrentIndex)), Toast.LENGTH_SHORT);
                    executeUpdate();
                    executeUpdateProgress();
                    next();
                    if (mWakeLock.isHeld())
                        mWakeLock.release();
                    break;
                case MediaPlayer.Event.TimeChanged:
                    reportState("positionchange", true);
                    break;
                case MediaPlayer.Event.PositionChanged:
                    updateWidgetPosition(event.getPositionChanged());
                    break;
                case MediaPlayer.Event.Vout:
                    break;
                case MediaPlayer.Event.ESAdded:
                    if (event.getEsChangedType() == Media.Track.Type.Video && (mVideoBackground || !switchToVideo())) {
                        /* Update notification content intent: resume video or resume audio activity */
                        updateMetadata();
                    }
                    break;
                case MediaPlayer.Event.ESDeleted:
                    break;
                case MediaPlayer.Event.PausableChanged:
                    mPausable = event.getPausable();
                    break;
                case MediaPlayer.Event.SeekableChanged:
                    mSeekable = event.getSeekable();
                    break;
            }
            for (Callback callback : mCallbacks)
                callback.onMediaPlayerEvent(event);
        }

        private void startTimer(){

            apiHelper.enableProgressReporting = true;

            timer = new Timer(true);

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    reportState("positionchange", true);
                }
            }, 0, 1000);
        }

        private void stopTimer(){

            apiHelper.enableProgressReporting = false;

            if (timer != null){
                timer.cancel();
                timer = null;
            }
        }

        private void reportState(String eventName, boolean checkLastReportTime) {

            if (!apiHelper.enableProgressReporting) {
                return;
            }

            if (checkLastReportTime){

                // avoid useless error logs
                // Avoid overly aggressive reporting
                long currentTime = System.currentTimeMillis();

                if ((currentTime - lastReportTime) < 1500){
                    return;
                }

                lastReportTime = System.currentTimeMillis();
            }

            long timeTicks = 0;

            try {
                timeTicks = getTimeTicks();
            }
            catch (IllegalStateException ex) {
                return;
            }

            int playerState = mMediaPlayer.getPlayerState();

            // Expected states by web plugins are: IDLE/CLOSE=0, OPENING=1, BUFFERING=2, PLAYING=3, PAUSED=4, STOPPING=5, ENDED=6, ERROR=7
            boolean isPaused = eventName.equalsIgnoreCase("playbackstop") ?
                    false :
                    eventName.equalsIgnoreCase("paused") || playerState == 4;

            logger.Debug("Vlc player state: %s", playerState);

            int volume = getVolume();

            lastReportedPositionTicks = timeTicks;

            apiHelper.ReportPlaybackProgress(timeTicks, volume, isPaused);
        }
    };

    private final MediaWrapperList.EventListener mListEventListener = new MediaWrapperList.EventListener() {

        @Override
        public void onItemAdded(int index, String mrl) {
            logger.Info("CustomMediaListItemAdded");
            if(mCurrentIndex >= index && !mExpanding.get())
                mCurrentIndex++;

            determinePrevAndNextIndices();
            executeUpdate();
        }

        @Override
        public void onItemRemoved(int index, String mrl) {
            logger.Info("CustomMediaListItemDeleted");
            if (mCurrentIndex == index && !mExpanding.get()) {
                // The current item has been deleted
                mCurrentIndex--;
                determinePrevAndNextIndices();
                if (mNextIndex != -1)
                    next();
                else if (mCurrentIndex != -1) {
                    playIndex(mCurrentIndex, 0);
                } else
                    stop();
            }

            if(mCurrentIndex > index && !mExpanding.get())
                mCurrentIndex--;
            determinePrevAndNextIndices();
            executeUpdate();
        }

        @Override
        public void onItemMoved(int indexBefore, int indexAfter, String mrl) {
            logger.Info("CustomMediaListItemMoved");
            if (mCurrentIndex == indexBefore) {
                mCurrentIndex = indexAfter;
                if (indexAfter > indexBefore)
                    mCurrentIndex--;
            } else if (indexBefore > mCurrentIndex
                    && indexAfter <= mCurrentIndex)
                mCurrentIndex++;
            else if (indexBefore < mCurrentIndex
                    && indexAfter > mCurrentIndex)
                mCurrentIndex--;

            // If we are in random mode, we completely reset the stored previous track
            // as their indices changed.
            mPrevious.clear();

            determinePrevAndNextIndices();
            executeUpdate();
        }
    };

    public boolean canSwitchToVideo() {
        return hasCurrentMedia() /*&& mMediaPlayer.getVideoTracksCount() > 0*/;
    }

    @MainThread
    public boolean switchToVideo() {
        MediaWrapper media = mMediaList.getMedia(mCurrentIndex);
        if (media == null || media.hasFlag(MediaWrapper.MEDIA_FORCE_AUDIO) || !canSwitchToVideo())
            return false;
        mVideoBackground = false;
        if (isVideoPlaying()) {//Player is already running, just send it an intent
            setVideoTrackEnabled(true);
            /*LocalBroadcastManager.getInstance(this).sendBroadcast(
                    VideoPlayerActivity.getIntent(VideoPlayerActivity.PLAY_FROM_SERVICE,
                            media, false, mCurrentIndex));*/
        } else if (!mSwitchingToVideo) {//Start the video player
            VideoPlayerActivity.startOpened(getApplicationContext(),
                    media.getUri(), mCurrentIndex);
            mSwitchingToVideo = true;
        }
        return true;
    }

    public void setResult(boolean completed, boolean error) {

        long positionMs = completed ? 0 : (lastReportedPositionTicks / 10000);

        positionMs -= (getTranscodingOffsetPositionTicks() / 10000);

        MainActivity.RespondToWebView(String.format("VideoRenderer.Current.onActivityClosed(%s, %s, %s);", !completed, error, positionMs));
    }

    private void executeUpdate() {
        executeUpdate(true);
    }

    private void executeUpdate(Boolean updateWidget) {
        for (Callback callback : mCallbacks) {
            callback.update();
        }
        if (updateWidget)
            updateWidget();
        updateMetadata();
        broadcastMetadata();
    }

    private void executeUpdateProgress() {
        for (Callback callback : mCallbacks) {
            callback.updateProgress();
        }
    }

    /**
     * Return the current media.
     *
     * @return The current media or null if there is not any.
     */
    @Nullable
    private MediaWrapper getCurrentMedia() {
        return mMediaList.getMedia(mCurrentIndex);
    }

    /**
     * Alias for mCurrentIndex >= 0
     *
     * @return True if a media is currently loaded, false otherwise
     */
    private boolean hasCurrentMedia() {
        return isValidIndex(mCurrentIndex);
    }

    private final Handler mHandler = new AudioServiceHandler(this);

    private static class AudioServiceHandler extends WeakHandler<PlaybackService> {

        public AudioServiceHandler(PlaybackService fragment) {
            super(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            PlaybackService service = getOwner();
            if(service == null) return;

            switch (msg.what) {
                case SHOW_PROGRESS:
                    if (service.mCallbacks.size() > 0) {
                        removeMessages(SHOW_PROGRESS);
                        service.executeUpdateProgress();
                        sendEmptyMessageDelayed(SHOW_PROGRESS, 1000);
                    }
                    break;
                case SHOW_TOAST:
                    /*final Bundle bundle = msg.getData();
                    final String text = bundle.getString("text");
                    final int duration = bundle.getInt("duration");
                    Toast.makeText(context, text, duration).show();*/
                    break;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void showNotification() {
        /*if (mMediaPlayer.getVLCVout().areViewsAttached()) {
            hideNotification(false);
            return;
        }
        try {
            boolean coverOnLockscreen = mSettings.getBoolean("lockscreen_cover", true);
            MediaMetadataCompat metaData = mMediaSession.getController().getMetadata();
            String title = metaData.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            String artist = metaData.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST);
            String album = metaData.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
            Bitmap cover = coverOnLockscreen ?
                    metaData.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART) :
                    AudioUtil.getCover(this, getCurrentMedia(), 512);
            if (cover == null)
                cover = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.icon);
            Notification notification;

            //Watch notification dismissed
            PendingIntent piStop = PendingIntent.getBroadcast(this, 0,
                    new Intent(ACTION_REMOTE_STOP), PendingIntent.FLAG_UPDATE_CURRENT);

            // add notification to status bar
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setSmallIcon(R.drawable.ic_stat_vlc)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentTitle(title)
                    .setContentText(artist + " - " + album)
                    .setLargeIcon(cover)
                    .setTicker(title + " - " + artist)
                    .setAutoCancel(!mMediaPlayer.isPlaying())
                    .setOngoing(mMediaPlayer.isPlaying())
                    .setDeleteIntent(piStop);


            PendingIntent pendingIntent;
            if (mVideoBackground || (canSwitchToVideo() && !mMediaList.getMedia(mCurrentIndex).hasFlag(MediaWrapper.MEDIA_FORCE_AUDIO))) {
                *//* Resume VideoPlayerActivity from ACTION_REMOTE_SWITCH_VIDEO intent *//*
                final Intent notificationIntent = new Intent(ACTION_REMOTE_SWITCH_VIDEO);
                pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                *//* Resume AudioPlayerActivity *//*

                final Intent notificationIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                notificationIntent.setAction(AudioPlayerContainerActivity.ACTION_SHOW_PLAYER);
                notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            }

            builder.setContentIntent(pendingIntent);

            PendingIntent piBackward = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_REMOTE_BACKWARD), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent piPlay = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_REMOTE_PLAYPAUSE), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent piForward = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_REMOTE_FORWARD), PendingIntent.FLAG_UPDATE_CURRENT);

            builder.addAction(R.drawable.ic_previous_w, getString(R.string.previous), piBackward);
            if (mMediaPlayer.isPlaying())
                builder.addAction(R.drawable.ic_pause_w, getString(R.string.pause), piPlay);
            else
                builder.addAction(R.drawable.ic_play_w, getString(R.string.play), piPlay);
            builder.addAction(R.drawable.ic_next_w, getString(R.string.next), piForward);

            if (AndroidDevices.showMediaStyle) {
                builder.setStyle(new NotificationCompat.MediaStyle()
                        .setMediaSession(mMediaSession.getSessionToken())
                        .setShowActionsInCompactView(new int[] {0,1,2})
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(piStop)
                );
            }

            notification = builder.build();

            startService(new Intent(this, PlaybackService.class));
            if (!AndroidUtil.isLolliPopOrLater() || mMediaPlayer.isPlaying())
                startForeground(3, notification);
            else {
                stopForeground(false);
                NotificationManagerCompat.from(this).notify(3, notification);
            }
        } catch (IllegalArgumentException e){
            // On somme crappy firmwares, shit can happen
            Log.e(TAG, "Failed to display notification", e);
        }*/
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
        stopForeground(true);
        //NotificationManagerCompat.from(this).cancel(3);
        if(stopPlayback) {
            mServiceStarted = false;
            stopSelf();
        }
    }

    @MainThread
    public void pause() {
        if (mPausable) {
            savePosition();
            mHandler.removeMessages(SHOW_PROGRESS);
            // hideNotification(); <-- see event handler
            mMediaPlayer.pause();
            broadcastMetadata();
        }
    }

    @MainThread
    public void play() {
        if(hasCurrentMedia()) {
            mMediaPlayer.play();
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
            updateMetadata();
            updateWidget();
            broadcastMetadata();
        }
    }

    @MainThread
    public void stopPlayback() {
        if (mMediaSession != null) {
            mMediaSession.setActive(false);
            mMediaSession.release();
            mMediaSession = null;
        }
        removePopup();
        if (mMediaPlayer == null)
            return;
        savePosition();
        final Media media = mMediaPlayer.getMedia();
        if (media != null) {
            media.setEventListener(null);
            mMediaPlayer.setEventListener(null);
            mMediaPlayer.stop();
            mMediaPlayer.setMedia(null);
            media.release();
        }
        mMediaList.removeEventListener(mListEventListener);
        mCurrentIndex = -1;
        mPrevious.clear();
        mHandler.removeMessages(SHOW_PROGRESS);
        hideNotification();
        broadcastMetadata();
        executeUpdate();
        executeUpdateProgress();
        changeAudioFocus(false);
    }

    @MainThread
    public void stop() {
        if (apiHelper != null) {
            apiHelper.enableProgressReporting = false;
        }

        stopPlayback();
        stopSelf();
    }

    private void determinePrevAndNextIndices() {
        determinePrevAndNextIndices(false);
    }

    private void determinePrevAndNextIndices(boolean expand) {
        if (expand) {
            mExpanding.set(true);
            mNextIndex = expand();
            mExpanding.set(false);
        } else {
            mNextIndex = -1;
        }
        mPrevIndex = -1;

        if (mNextIndex == -1) {
            // No subitems; play the next item.
            int size = mMediaList.size();
            mShuffling &= size > 2;

            // Repeating once doesn't change the index
            if (mRepeating == REPEAT_ONE) {
                mPrevIndex = mNextIndex = mCurrentIndex;
            } else {

                if(mShuffling) {
                    if(!mPrevious.isEmpty()){
                        mPrevIndex = mPrevious.peek();
                        while (!isValidIndex(mPrevIndex)) {
                            mPrevious.remove(mPrevious.size() - 1);
                            if (mPrevious.isEmpty()) {
                                mPrevIndex = -1;
                                break;
                            }
                            mPrevIndex = mPrevious.peek();
                        }
                    }
                    // If we've played all songs already in shuffle, then either
                    // reshuffle or stop (depending on RepeatType).
                    if(mPrevious.size() + 1 == size) {
                        if(mRepeating == REPEAT_NONE) {
                            mNextIndex = -1;
                            return;
                        } else {
                            mPrevious.clear();
                            mRandom = new Random(System.currentTimeMillis());
                        }
                    }
                    if(mRandom == null) mRandom = new Random(System.currentTimeMillis());
                    // Find a new index not in mPrevious.
                    do
                    {
                        mNextIndex = mRandom.nextInt(size);
                    }
                    while(mNextIndex == mCurrentIndex || mPrevious.contains(mNextIndex));

                } else {
                    // normal playback
                    if(mCurrentIndex > 0)
                        mPrevIndex = mCurrentIndex - 1;
                    if(mCurrentIndex + 1 < size)
                        mNextIndex = mCurrentIndex + 1;
                    else {
                        if(mRepeating == REPEAT_NONE) {
                            mNextIndex = -1;
                        } else {
                            mNextIndex = 0;
                        }
                    }
                }
            }
        }
    }

    private boolean isValidIndex(int position) {
        return position >= 0 && position < mMediaList.size();
    }

    private void initMediaSession() {
        ComponentName mediaButtonEventReceiver = new ComponentName(this,
                RemoteControlClientReceiver.class);
        mSessionCallback = new MediaSessionCallback();
        mMediaSession = new MediaSessionCompat(this, "VLC", mediaButtonEventReceiver, null);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setCallback(mSessionCallback);
        try {
            mMediaSession.setActive(true);
        } catch (NullPointerException e) {
            // Some versions of KitKat do not support AudioManager.registerMediaButtonIntent
            // with a PendingIntent. They will throw a NullPointerException, in which case
            // they should be able to activate a MediaSessionCompat with only transport
            // controls.
            mMediaSession.setActive(false);
            mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
            mMediaSession.setActive(true);
        }
    }

    private final class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            play();
        }
        @Override
        public void onPause() {
            pause();
        }

        @Override
        public void onStop() {
            stop();
        }

        @Override
        public void onSkipToNext() {
            next();
        }

        @Override
        public void onSkipToPrevious() {
            previous();
        }

        @Override
        public void onSeekTo(long pos) {
            seek(pos);
        }

        @Override
        public void onFastForward() {
            next();
        }

        @Override
        public void onRewind() {
            previous();
        }
    }

    protected void updateMetadata() {
        /*MediaWrapper media = getCurrentMedia();
        if (media == null)
            return;
        if (mMediaSession == null)
            initMediaSession();
        String title = media.getNowPlaying();
        if (title == null)
            title = media.getTitle();
        boolean coverOnLockscreen = mSettings.getBoolean("lockscreen_cover", true);
        MediaMetadataCompat.Builder bob = new MediaMetadataCompat.Builder();
        bob.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, MediaUtils.getMediaGenre(this, media))
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, media.getTrackNumber())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, MediaUtils.getMediaArtist(this, media))
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, MediaUtils.getMediaReferenceArtist(this, media))
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, MediaUtils.getMediaAlbum(this, media))
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, media.getLength());
        if (coverOnLockscreen) {
            Bitmap cover = AudioUtil.getCover(this, media, 512);
            if (cover != null && cover.getConfig() != null) //In case of format not supported
                bob.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cover.copy(cover.getConfig(), false));
        }
        mMediaSession.setMetadata(bob.build());*/
    }

    protected void publishState(int state) {
        if (mMediaSession == null)
            return;
        PlaybackStateCompat.Builder bob = new PlaybackStateCompat.Builder();
        bob.setActions(PLAYBACK_ACTIONS);
        switch (state) {
            case MediaPlayer.Event.Playing:
                bob.setState(PlaybackStateCompat.STATE_PLAYING, -1, 1);
                break;
            case MediaPlayer.Event.Stopped:
                bob.setState(PlaybackStateCompat.STATE_STOPPED, -1, 0);
                break;
            default:
                bob.setState(PlaybackStateCompat.STATE_PAUSED, -1, 0);
        }
        PlaybackStateCompat pbState = bob.build();
        mMediaSession.setPlaybackState(pbState);
        mMediaSession.setActive(state != PlaybackStateCompat.STATE_STOPPED);
    }

    private VolleyHttpClient GetHttpClient() {

        VolleyHttpClient httpClient = null;
        if (ApiClientBridge.Current != null) {
            httpClient = ApiClientBridge.Current.httpClient;
        }

        if (httpClient == null) {
            httpClient = new VolleyHttpClient(logger, getApplicationContext());
        }

        return httpClient;
    }

    private void notifyTrackChanged() {
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
        updateMetadata();
        updateWidget();
        broadcastMetadata();
    }

    private void onMediaChanged() {
        notifyTrackChanged();

        saveCurrentMedia();
        determinePrevAndNextIndices();
    }

    private void onMediaListChanged() {
        saveMediaList();
        determinePrevAndNextIndices();
        executeUpdate();
    }

    @MainThread
    public void next() {
        int size = mMediaList.size();

        mPrevious.push(mCurrentIndex);
        mCurrentIndex = mNextIndex;
        if (size == 0 || mCurrentIndex < 0 || mCurrentIndex >= size) {
            if (mCurrentIndex < 0)
                saveCurrentMedia();
            logger.Info("Warning: invalid next index, aborted !");
            //Close video player if started
            //LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(VideoPlayerActivity.EXIT_PLAYER));
            stop();
            return;
        }
        mVideoBackground = !isVideoPlaying() && canSwitchToVideo();
        playIndex(mCurrentIndex, 0);
        saveCurrentMedia();
    }

    @MainThread
    public void previous() {
        if (hasPrevious() && mCurrentIndex > 0 &&
                (!mMediaPlayer.isSeekable() || mMediaPlayer.getTime() < 2000l)) {
            int size = mMediaList.size();
            mCurrentIndex = mPrevIndex;
            if (mPrevious.size() > 0)
                mPrevious.pop();
            if (size == 0 || mPrevIndex < 0 || mCurrentIndex >= size) {
                logger.Info("Warning: invalid previous index, aborted !");
                stop();
                return;
            }
            playIndex(mCurrentIndex, 0);
            saveCurrentMedia();
        } else
            setPosition(0f);
    }

    @MainThread
    public void shuffle() {
        if (mShuffling)
            mPrevious.clear();
        mShuffling = !mShuffling;
        savePosition();
        determinePrevAndNextIndices();
    }

    @MainThread
    public void setRepeatType(int repeatType) {
        mRepeating = repeatType;
        savePosition();
        determinePrevAndNextIndices();
    }

    private void updateWidget() {
        updateWidgetState();
        updateWidgetCover();
    }

    private void updateWidgetState() {
        /*Intent i = new Intent(VLCAppWidgetProvider.ACTION_WIDGET_UPDATE);

        if (hasCurrentMedia()) {
            final MediaWrapper media = getCurrentMedia();
            i.putExtra("title", media.getTitle());
            i.putExtra("artist", media.isArtistUnknown() && media.getNowPlaying() != null ?
                    media.getNowPlaying()
                    : MediaUtils.getMediaArtist(this, media));
        }
        else {
            i.putExtra("title", getString(R.string.widget_default_text));
            i.putExtra("artist", "");
        }
        i.putExtra("isplaying", mMediaPlayer.isPlaying());

        sendBroadcast(i);*/
    }

    private void updateWidgetCover() {
        /*Intent i = new Intent(VLCAppWidgetProvider.ACTION_WIDGET_UPDATE_COVER);

        Bitmap cover = hasCurrentMedia() ? AudioUtil.getCover(this, getCurrentMedia(), 64) : null;
        i.putExtra("cover", cover);

        sendBroadcast(i);*/
    }

    private void updateWidgetPosition(float pos) {
        /*// no more than one widget update for each 1/50 of the song
        long timestamp = Calendar.getInstance().getTimeInMillis();
        if (!hasCurrentMedia()
                || timestamp - mWidgetPositionTimestamp < getCurrentMedia().getLength() / 50)
            return;

        updateWidgetState();

        mWidgetPositionTimestamp = timestamp;
        Intent i = new Intent(VLCAppWidgetProvider.ACTION_WIDGET_UPDATE_POSITION);
        i.putExtra("position", pos);
        sendBroadcast(i);*/
    }

    private void broadcastMetadata() {
        MediaWrapper media = getCurrentMedia();
        if (media == null || media.getType() != MediaWrapper.TYPE_AUDIO)
            return;

        boolean playing = mMediaPlayer.isPlaying();

        Intent broadcast = new Intent("com.android.music.metachanged");
        broadcast.putExtra("track", media.getTitle());
        broadcast.putExtra("artist", media.getArtist());
        broadcast.putExtra("album", media.getAlbum());
        broadcast.putExtra("duration", media.getLength());
        broadcast.putExtra("playing", playing);
        broadcast.putExtra("package", "org.videolan.vlc");

        sendBroadcast(broadcast);
    }

    public synchronized void loadLastPlaylist(int type) {
        boolean audio = type == TYPE_AUDIO;
        String currentMedia = mSettings.getString(audio ? "current_song" : "current_media", "");
        if (currentMedia.equals(""))
            return;
        String[] locations = mSettings.getString(audio ? "audio_list" : "media_list", "").split(" ");
        if (locations.length == 0)
            return;

        List<String> mediaPathList = new ArrayList<String>(locations.length);
        for (int i = 0 ; i < locations.length ; ++i)
            mediaPathList.add(Uri.decode(locations[i]));

        mShuffling = mSettings.getBoolean(audio ? "audio_shuffling" : "media_shuffling", false);
        mRepeating = mSettings.getInt(audio ? "audio_repeating" : "media_repeating", REPEAT_NONE);
        int position = mSettings.getInt(audio ? "position_in_audio_list" : "position_in_media_list",
                Math.max(0, mediaPathList.indexOf(currentMedia)));
        long time = mSettings.getLong(audio ? "position_in_song" : "position_in_media", -1);
        mSavedTime = time;
        // load playlist
        loadLocations(mediaPathList, position);
        if (time > 0)
            seek(time);
        if (!audio) {
            boolean paused = mSettings.getBoolean(PreferencesActivity.VIDEO_PAUSED, !isPlaying());
            float rate = mSettings.getFloat(PreferencesActivity.VIDEO_SPEED, getRate());
            if (paused)
                pause();
            if (rate != 1.0f)
                setRate(rate, false);
        }
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt(audio ? "position_in_audio_list" : "position_in_media_list", 0);
        editor.putLong(audio ? "position_in_song" : "position_in_media", 0);
        editor.apply();
    }

    private synchronized void saveCurrentMedia() {
        boolean audio = true;
        for (int i = 0; i < mMediaList.size(); i++) {
            if (mMediaList.getMedia(i).getType() == MediaWrapper.TYPE_VIDEO)
                audio = false;
        }
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(audio ? "current_song" : "current_media", mMediaList.getMRL(Math.max(mCurrentIndex, 0)));
        editor.apply();
    }

    private synchronized void saveMediaList() {
        if (getCurrentMedia() == null)
            return;
        StringBuilder locations = new StringBuilder();
        boolean audio = true;
        for (int i = 0; i < mMediaList.size(); i++) {
            if (mMediaList.getMedia(i).getType() == MediaWrapper.TYPE_VIDEO)
                audio = false;
            locations.append(" ").append(Uri.encode(mMediaList.getMRL(i)));
        }
        //We save a concatenated String because putStringSet is APIv11.
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(audio ? "audio_list" : "media_list", locations.toString().trim());
        editor.apply();
    }

    private synchronized void savePosition(){
        if (getCurrentMedia() == null)
            return;
        SharedPreferences.Editor editor = mSettings.edit();
        boolean audio = true;
        for (int i = 0; i < mMediaList.size(); i++) {
            if (mMediaList.getMedia(i).getType() == MediaWrapper.TYPE_VIDEO)
                audio = false;
        }
        editor.putBoolean(audio ? "audio_shuffling" : "media_shuffling", mShuffling);
        editor.putInt(audio ? "audio_repeating" : "media_repeating", mRepeating);
        editor.putInt(audio ? "position_in_audio_list" : "position_in_media_list", mCurrentIndex);
        editor.putLong(audio ? "position_in_song" : "position_in_media", mMediaPlayer.getTime());
        if(!audio) {
            editor.putBoolean(PreferencesActivity.VIDEO_PAUSED, !isPlaying());
            editor.putFloat(PreferencesActivity.VIDEO_SPEED, getRate());
        }
        editor.apply();
    }

    private boolean validateLocation(String location)
    {
        /* Check if the MRL contains a scheme */
        if (!location.matches("\\w+://.+"))
            location = "file://".concat(location);
        if (location.toLowerCase(Locale.ENGLISH).startsWith("file://")) {
            /* Ensure the file exists */
            File f;
            try {
                f = new File(new URI(location));
            } catch (URISyntaxException e) {
                return false;
            } catch (IllegalArgumentException e) {
                return false;
            }
            if (!f.isFile())
                return false;
        }
        return true;
    }

    private void showToast(String text, int duration) {
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("text", text);
        bundle.putInt("duration", duration);
        msg.setData(bundle);
        msg.what = SHOW_TOAST;
        mHandler.sendMessage(msg);
    }

    @MainThread
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @MainThread
    public boolean isSeekable() {
        return mSeekable;
    }

    @MainThread
    public boolean isPausable() {
        return mPausable;
    }

    @MainThread
    public boolean isShuffling() {
        return mShuffling;
    }

    @MainThread
    public boolean canShuffle()  {
        return getMediaListSize() > 2;
    }

    @MainThread
    public int getRepeatType() {
        return mRepeating;
    }

    @MainThread
    public boolean hasMedia()  {
        return hasCurrentMedia();
    }

    @MainThread
    public boolean hasPlaylist()  {
        return getMediaListSize() > 1;
    }

    @MainThread
    public boolean isVideoPlaying() {
        return mMediaPlayer.getVLCVout().areViewsAttached();
    }

    @MainThread
    public String getAlbum() {
        /*if (hasCurrentMedia())
            return MediaUtils.getMediaAlbum(PlaybackService.this, getCurrentMedia());
        else*/
            return null;
    }

    @MainThread
    public String getArtist() {
        /*if (hasCurrentMedia()) {
            final MediaWrapper media = getCurrentMedia();
            return media.getNowPlaying() != null ?
                    media.getTitle()
                    : MediaUtils.getMediaArtist(PlaybackService.this, media);
        } else*/
            return null;
    }

    @MainThread
    public String getArtistPrev() {
        /*if (mPrevIndex != -1)
            return MediaUtils.getMediaArtist(PlaybackService.this, mMediaList.getMedia(mPrevIndex));
        else*/
            return null;
    }

    @MainThread
    public String getArtistNext() {
        /*if (mNextIndex != -1)
            return MediaUtils.getMediaArtist(PlaybackService.this, mMediaList.getMedia(mNextIndex));
        else*/
            return null;
    }

    @MainThread
    public String getTitle() {
        if (hasCurrentMedia())
            return getCurrentMedia().getNowPlaying() != null ? getCurrentMedia().getNowPlaying() : getCurrentMedia().getTitle();
        else
            return null;
    }

    @MainThread
    public String getTitlePrev() {
        if (mPrevIndex != -1)
            return mMediaList.getMedia(mPrevIndex).getTitle();
        else
            return null;
    }

    @MainThread
    public String getTitleNext() {
        if (mNextIndex != -1)
            return mMediaList.getMedia(mNextIndex).getTitle();
        else
            return null;
    }

    @MainThread
    public Bitmap getCover() {
        /*if (hasCurrentMedia()) {
            return AudioUtil.getCover(PlaybackService.this, getCurrentMedia(), 512);
        }*/
        return null;
    }

    @MainThread
    public Bitmap getCoverPrev() {
        /*if (mPrevIndex != -1)
            return AudioUtil.getCover(PlaybackService.this, mMediaList.getMedia(mPrevIndex), 64);
        else*/
            return null;
    }

    @MainThread
    public Bitmap getCoverNext() {
        /*if (mNextIndex != -1)
            return AudioUtil.getCover(PlaybackService.this, mMediaList.getMedia(mNextIndex), 64);
        else*/
            return null;
    }

    @MainThread
    public synchronized void addCallback(Callback cb) {
        if (!mCallbacks.contains(cb)) {
            mCallbacks.add(cb);
            if (hasCurrentMedia())
                mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    }

    @MainThread
    public synchronized void removeCallback(Callback cb) {
        mCallbacks.remove(cb);
    }

    @MainThread
    public long getTime() {
        return getTimeTicks() / 10000;
    }

    @MainThread
    public long getTimeTicks() {

        long mediaPlayerTime = mMediaPlayer.getTime();

        //logger.Info("mediaPlayerTime %s. adding transcoding offset ticks %s", mediaPlayerTime, getTranscodingOffsetPositionTicks());

        return (mediaPlayerTime * 10000) + getTranscodingOffsetPositionTicks();
    }

    @MainThread
    public boolean getEnableServerSeek() {
        return EnableServerSeek;
    }

    @MainThread
    public void setEnableServerSeek(boolean val) {
        EnableServerSeek = val;
    }

    @MainThread
    public long getTranscodingOffsetPositionTicks() {
        return TranscodingOffsetPositionTicks;
    }

    @MainThread
    public void setTranscodingOffsetPositionTicks(long val) {
        TranscodingOffsetPositionTicks = val;
    }

    @MainThread
    public long getLength() {
        long length = mMediaPlayer.getLength();

        if (length == 0 || getEnableServerSeek()) {
            if (apiHelper.getMediaSource().getRunTimeTicks() != null){
                length = (apiHelper.getMediaSource().getRunTimeTicks()/ 10000);
            }
        }

        return length;
    }

    /**
     * Loads a selection of files (a non-user-supplied collection of media)
     * into the primary or "currently playing" playlist.
     *
     * @param mediaPathList A list of locations to load
     * @param position The position to start playing at
     */
    @MainThread
    public void loadLocations(List<String> mediaPathList, int position) {
        /*ArrayList<MediaWrapper> mediaList = new ArrayList<MediaWrapper>();
        MediaDatabase db = MediaDatabase.getInstance();

        for (int i = 0; i < mediaPathList.size(); i++) {
            String location = mediaPathList.get(i);
            MediaWrapper mediaWrapper = db.getMedia(Uri.parse(location));
            if (mediaWrapper == null) {
                if (!validateLocation(location)) {
                    Log.w(TAG, "Invalid location " + location);
                    showToast(getResources().getString(R.string.invalid_location, location), Toast.LENGTH_SHORT);
                    continue;
                }
                Log.v(TAG, "Creating on-the-fly Media object for " + location);
                mediaWrapper = new MediaWrapper(Uri.parse(location));
            }
            mediaList.add(mediaWrapper);
        }
        load(mediaList, position);*/
    }

    @MainThread
    public void loadUri(Uri uri) {
        String path = uri.toString();
        if (TextUtils.equals(uri.getScheme(), "content")) {
            path = "file://"+ FileUtils.getPathFromURI(getApplicationContext(), uri);
        }
        loadLocation(path);
    }

    @MainThread
    public void loadLocation(String mediaPath) {
        loadLocations(Collections.singletonList(mediaPath), 0);
    }

    @MainThread
    public void load(List<MediaWrapper> mediaList, int position) {
        logger.Info("Loading position " + ((Integer) position).toString() + " in " + mediaList.toString());

        if (hasCurrentMedia())
            savePosition();

        mMediaList.removeEventListener(mListEventListener);
        mMediaList.clear();
        MediaWrapperList currentMediaList = mMediaList;

        mPrevious.clear();

        for (int i = 0; i < mediaList.size(); i++) {
            currentMediaList.add(mediaList.get(i));
        }

        if (mMediaList.size() == 0) {
            logger.Info("Warning: empty media list, nothing to play !");
            return;
        }
        if (isValidIndex(position)) {
            mCurrentIndex = position;
        } else {
            logger.Info("Warning: positon " + position + " out of bounds");
            mCurrentIndex = 0;
        }

        // Add handler after loading the list
        mMediaList.addEventListener(mListEventListener);

        playIndex(mCurrentIndex, 0);
        saveMediaList();
        onMediaChanged();
    }

    @MainThread
    public void load(MediaWrapper media) {
        ArrayList<MediaWrapper> arrayList = new ArrayList<MediaWrapper>();
        arrayList.add(media);
        load(arrayList, 0);
    }

    /**
     * Play a media from the media list (playlist)
     *
     * @param index The index of the media
     * @param flags LibVLC.MEDIA_* flags
     */
    public void playIndex(int index, int flags) {
        if (mMediaList.size() == 0) {
            logger.Info("Warning: empty media list, nothing to play !");
            return;
        }
        if (isValidIndex(index)) {
            mCurrentIndex = index;
        } else {
            logger.Info("Warning: index " + index + " out of bounds");
            mCurrentIndex = 0;
        }

        String mrl = mMediaList.getMRL(index);
        if (mrl == null)
            return;
        final MediaWrapper mw = mMediaList.getMedia(index);
        if (mw == null)
            return;

        boolean isVideoPlaying = isVideoPlaying();
        if (!mVideoBackground && mw.getType() == MediaWrapper.TYPE_VIDEO && isVideoPlaying)
            mw.addFlags(MediaWrapper.MEDIA_VIDEO);

        if (mVideoBackground)
            mw.addFlags(MediaWrapper.MEDIA_FORCE_AUDIO);

        /* Pausable and seekable are true by default */
        mParsed = false;
        mSwitchingToVideo = false;
        mPausable = mSeekable = true;
        final Media media = new Media(VLCInstance.get(getApplicationContext(), logger), mw.getUri());
        VLCOptions.setMediaOptions(media, this, flags | mw.getFlags());

        /*if (mw.getSlaves() != null) {
            for (Media.Slave slave : mw.getSlaves())
                media.addSlave(slave);
            VLCApplication.runBackground(new Runnable() {
                @Override
                public void run() {
                    MediaDatabase.getInstance().saveSlaves(mw);
                }
            });
        }
        VLCApplication.runBackground(new Runnable() {
            @Override
            public void run() {
                final ArrayList<Media.Slave> list = MediaDatabase.getInstance().getSlaves(mw.getLocation());
                for (Media.Slave slave : list)
                    mMediaPlayer.addSlave(slave.type, Uri.parse(slave.uri), false);
            }
        });*/

        media.setEventListener(mMediaListener);
        mMediaPlayer.setMedia(media);
        media.release();

        if (mw .getType() != MediaWrapper.TYPE_VIDEO || mw.hasFlag(MediaWrapper.MEDIA_FORCE_AUDIO)
                || isVideoPlaying) {
            mMediaPlayer.setEqualizer(VLCOptions.getEqualizer(this));
            mMediaPlayer.setVideoTitleDisplay(MediaPlayer.Position.Disable, 0);
            changeAudioFocus(true);
            mMediaPlayer.setEventListener(mMediaPlayerListener);
            /*if (!isVideoPlaying && mMediaPlayer.getRate() == 1.0F && mSettings.getBoolean(PreferencesActivity.KEY_AUDIO_PLAYBACK_SPEED_PERSIST, true))
                setRate(mSettings.getFloat(PreferencesActivity.KEY_AUDIO_PLAYBACK_RATE, 1.0F), true);*/
            mMediaPlayer.play();

            determinePrevAndNextIndices();
            /*if (mSettings.getBoolean(PreferencesFragment.PLAYBACK_HISTORY, true))
                VLCApplication.runBackground(new Runnable() {
                    @Override
                    public void run() {
                        MediaDatabase.getInstance().addHistoryItem(mw);
                    }
                });*/
        } else {//Start VideoPlayer for first video, it will trigger playIndex when ready.
            VideoPlayerActivity.startOpened(getApplicationContext(),
                    getCurrentMediaWrapper().getUri(), mCurrentIndex);
        }
    }

    /**
     * Use this function to play a media inside whatever MediaList LibVLC is following.
     *
     * Unlike load(), it does not import anything into the primary list.
     */
    @MainThread
    public void playIndex(int index) {
        playIndex(index, 0);
    }

    public void restartIndex(int index, int flags) {

        final Media media = mMediaPlayer.getMedia();
        if (media != null) {
            media.setEventListener(null);
            media.release();
        }

        playIndex(index, flags);
        onMediaChanged();
    }

    @MainThread
    public MediaWrapper setMedia(Uri uri, int index) {

        final Media media = new Media(VLCInstance.get(getApplicationContext(), logger), uri);
        MediaWrapper mediaWrapper = new MediaWrapper(media);
        mediaWrapper.removeFlags(MediaWrapper.MEDIA_FORCE_AUDIO);
        mediaWrapper.addFlags(MediaWrapper.MEDIA_VIDEO);
        mMediaList.set(index,mediaWrapper);
        return mediaWrapper;
    }

    @MainThread
    public MediaPlayer getMediaPlayer() {
        return  mMediaPlayer;
    }

    /**
     * Use this function to show an URI in the audio interface WITHOUT
     * interrupting the stream.
     *
     * Mainly used by VideoPlayerActivity in response to loss of video track.
     */
    @MainThread
    public void showWithoutParse(int index) {
        setVideoTrackEnabled(false);
        MediaWrapper media = mMediaList.getMedia(index);

        if(media == null || !mMediaPlayer.isPlaying())
            return;
        // Show an URI without interrupting/losing the current stream
        logger.Info("Showing index " + index + " with playing URI " + media.getUri());
        mCurrentIndex = index;

        notifyTrackChanged();
        showNotification();
    }

    @MainThread
    public void switchToPopup(int index) {
        showWithoutParse(index);
        showPopup();
    }

    @MainThread
    public void removePopup() {
        /*if (mPopupManager != null) {
            mPopupManager.removePopup();
        }
        mPopupManager = null;*/
    }

    @MainThread
    public boolean isPlayingPopup() {
        return false;
        //return mPopupManager != null;
    }

    @MainThread
    public void showPopup() {
        /*if (mPopupManager == null)
            mPopupManager = new PopupManager(this);
        mPopupManager.showPopup();*/
    }

    public void setVideoTrackEnabled(boolean enabled) {
        if (!hasMedia() || !isPlaying())
            return;
        if (enabled)
            getCurrentMedia().addFlags(MediaWrapper.MEDIA_VIDEO);
        else
            getCurrentMedia().removeFlags(MediaWrapper.MEDIA_VIDEO);
        mMediaPlayer.setVideoTrackEnabled(enabled);
    }

    /**
     * Append to the current existing playlist
     */
    @MainThread
    public void append(List<MediaWrapper> mediaList) {
        if (!hasCurrentMedia())
        {
            load(mediaList, 0);
            return;
        }

        for (int i = 0; i < mediaList.size(); i++) {
            MediaWrapper mediaWrapper = mediaList.get(i);
            mMediaList.add(mediaWrapper);
        }
        onMediaListChanged();
    }

    @MainThread
    public void append(MediaWrapper media) {
        ArrayList<MediaWrapper> arrayList = new ArrayList<MediaWrapper>();
        arrayList.add(media);
        append(arrayList);
    }

    /**
     * Move an item inside the playlist.
     */
    @MainThread
    public void moveItem(int positionStart, int positionEnd) {
        mMediaList.move(positionStart, positionEnd);
        PlaybackService.this.saveMediaList();
    }

    @MainThread
    public void insertItem(int position, MediaWrapper mw) {
        mMediaList.insert(position, mw);
        saveMediaList();
        determinePrevAndNextIndices();
    }


    @MainThread
    public void remove(int position) {
        mMediaList.remove(position);
        saveMediaList();
        determinePrevAndNextIndices();
    }

    @MainThread
    public void removeLocation(String location) {
        mMediaList.remove(location);
        saveMediaList();
        determinePrevAndNextIndices();
    }

    public int getMediaListSize() {
        return mMediaList.size();
    }

    @MainThread
    public List<MediaWrapper> getMedias() {
        final ArrayList<MediaWrapper> ml = new ArrayList<MediaWrapper>();
        for (int i = 0; i < mMediaList.size(); i++) {
            ml.add(mMediaList.getMedia(i));
        }
        return ml;
    }

    @MainThread
    public List<String> getMediaLocations() {
        ArrayList<String> medias = new ArrayList<String>();
        for (int i = 0; i < mMediaList.size(); i++) {
            medias.add(mMediaList.getMRL(i));
        }
        return medias;
    }

    @MainThread
    public String getCurrentMediaLocation() {
        return mMediaList.getMRL(mCurrentIndex);
    }

    @MainThread
    public int getCurrentMediaPosition() {
        return mCurrentIndex;
    }

    @MainThread
    public MediaWrapper getCurrentMediaWrapper() {
        return PlaybackService.this.getCurrentMedia();
    }

    @MainThread
    public void setTime(long time) {
        if (mSeekable)
            mMediaPlayer.setTime(time);
    }

    @MainThread
    public boolean hasNext() {
        return mNextIndex != -1;
    }

    @MainThread
    public boolean hasPrevious() {
        return mPrevIndex != -1;
    }

    @MainThread
    public void detectHeadset(boolean enable)  {
        mDetectHeadset = enable;
    }

    @MainThread
    public float getRate()  {
        return mMediaPlayer.getRate();
    }

    @MainThread
    public void setRate(float rate, boolean save) {
        mMediaPlayer.setRate(rate);
        /*if (save && mSettings.getBoolean(PreferencesActivity.KEY_AUDIO_PLAYBACK_SPEED_PERSIST, true))
            mSettings.edit().putFloat(PreferencesActivity.KEY_AUDIO_PLAYBACK_RATE, rate).apply();*/
    }

    @MainThread
    public void navigate(int where) {
        mMediaPlayer.navigate(where);
    }

    @MainThread
    public MediaPlayer.Chapter[] getChapters(int title) {
        return mMediaPlayer.getChapters(title);
    }

    @MainThread
    public MediaPlayer.Title[] getTitles() {
        return mMediaPlayer.getTitles();
    }

    @MainThread
    public int getChapterIdx() {
        return mMediaPlayer.getChapter();
    }

    @MainThread
    public void setChapterIdx(int chapter) {
        mMediaPlayer.setChapter(chapter);
    }

    @MainThread
    public int getTitleIdx() {
        return mMediaPlayer.getTitle();
    }

    @MainThread
    public void setTitleIdx(int title) {
        mMediaPlayer.setTitle(title);
    }

    @MainThread
    public int getVolume() {
        return mMediaPlayer.getVolume();
    }

    @MainThread
    public int setVolume(int volume) {
        return mMediaPlayer.setVolume(volume);
    }

    @MainThread
    public void seek(long position) {
        seek(position, getLength());
    }

    @MainThread
    public void seek(long position, double length) {
        if (length > 0.0D)
            setPosition((float) (position/length));
        else
            setTime(position);
    }

    @MainThread
    public void saveTimeToSeek(long time) {
        mSavedTime = time;
    }

    @MainThread
    public void setPosition(float pos) {
        if (mSeekable)
            mMediaPlayer.setPosition(pos);
    }

    @MainThread
    public int getAudioTracksCount() {
        return mMediaPlayer.getAudioTracksCount();
    }

    @MainThread
    public MediaPlayer.TrackDescription[] getAudioTracks() {
        return mMediaPlayer.getAudioTracks();
    }

    @MainThread
    public int getAudioTrack() {
        return mMediaPlayer.getAudioTrack();
    }

    @MainThread
    public boolean setAudioTrack(int index) {
        return mMediaPlayer.setAudioTrack(index);
    }

    @MainThread
    public int getVideoTracksCount() {
        return mMediaPlayer.getVideoTracksCount();
    }

    @MainThread
    public boolean addSubtitleTrack(String path, boolean select) {
        return mMediaPlayer.addSlave(Media.Slave.Type.Subtitle, path, select);
    }

    @MainThread
    public boolean addSubtitleTrack(Uri uri,boolean select) {
        return mMediaPlayer.addSlave(Media.Slave.Type.Subtitle, uri, select);
    }

    @MainThread
    public boolean addSubtitleTrack(String path) {
        return addSubtitleTrack(path, false);
    }

    @MainThread
    public boolean addSubtitleTrack(Uri uri) {
        return addSubtitleTrack(uri, false);
    }

    @MainThread
    public MediaPlayer.TrackDescription[] getSpuTracks() {
        return mMediaPlayer.getSpuTracks();
    }

    @MainThread
    public int getSpuTrack() {
        return mMediaPlayer.getSpuTrack();
    }

    @MainThread
    public boolean setSpuTrack(int index) {
        return mMediaPlayer.setSpuTrack(index);
    }

    @MainThread
    public int getSpuTracksCount() {
        return mMediaPlayer.getSpuTracksCount();
    }

    @MainThread
    public boolean setAudioDelay(long delay) {
        return mMediaPlayer.setAudioDelay(delay);
    }

    @MainThread
    public long getAudioDelay() {
        return mMediaPlayer.getAudioDelay();
    }

    @MainThread
    public boolean setSpuDelay(long delay) {
        return mMediaPlayer.setSpuDelay(delay);
    }

    @MainThread
    public long getSpuDelay() {
        return mMediaPlayer.getSpuDelay();
    }

    @MainThread
    public void setEqualizer(MediaPlayer.Equalizer equalizer) {
        mMediaPlayer.setEqualizer(equalizer);
    }

    /**
     * Expand the current media.
     * @return the index of the media was expanded, and -1 if no media was expanded
     */
    @MainThread
    public int expand() {
        final Media media = mMediaPlayer.getMedia();
        if (media == null)
            return -1;
        final MediaList ml = media.subItems();
        media.release();
        int ret;

        if (ml.getCount() > 0) {
            mMediaList.remove(mCurrentIndex);
            for (int i = ml.getCount() - 1; i >= 0; --i) {
                final Media child = ml.getMediaAt(i);
                child.parse();
                mMediaList.insert(mCurrentIndex, new MediaWrapper(child));
                child.release();
            }
            ret = 0;
        } else {
            ret = -1;
        }
        ml.release();
        return ret;
    }

    public void restartMediaPlayer() {
        stop();
        mMediaPlayer.release();
        mMediaPlayer = newMediaPlayer();
        /* TODO RESUME */
    }

    public static class Client {
        public static final String TAG = "PlaybackService.Client";

        @MainThread
        public interface Callback {
            void onConnected(PlaybackService service);
            void onDisconnected();
        }

        private boolean mBound = false;
        private final Callback mCallback;
        private final Context mContext;

        private final ServiceConnection mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder iBinder) {
                if (!mBound)
                    return;

                final PlaybackService service = PlaybackService.getService(iBinder);
                if (service != null)
                    mCallback.onConnected(service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mBound = false;
                mCallback.onDisconnected();
            }
        };

        private static Intent getServiceIntent(Context context) {
            return new Intent(context, PlaybackService.class);
        }

        private static void startService(Context context) {
            context.startService(getServiceIntent(context));
        }

        private static void stopService(Context context) {
            context.stopService(getServiceIntent(context));
        }

        public Client(Context context, Callback callback) {
            if (context == null || callback == null)
                throw new IllegalArgumentException("Context and callback can't be null");
            mContext = context;
            mCallback = callback;
        }

        @MainThread
        public void connect() {
            if (mBound)
                throw new IllegalStateException("already connected");
            startService(mContext);
            mBound = mContext.bindService(getServiceIntent(mContext), mServiceConnection, BIND_AUTO_CREATE);
        }

        @MainThread
        public void disconnect() {
            if (mBound) {
                mBound = false;
                mContext.unbindService(mServiceConnection);
            }
        }

        public static void restartService(Context context) {
            stopService(context);
            startService(context);
        }
    }
}