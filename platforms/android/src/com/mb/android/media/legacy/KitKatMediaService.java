package com.mb.android.media.legacy;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
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
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.mb.android.BuildConfig;
import com.mb.android.MainActivity;
import com.mb.android.R;
import com.mb.android.api.ApiClientBridge;
import com.mb.android.logging.AppLogger;
import com.mb.android.media.MediaWrapper;
import com.mb.android.media.MediaWrapperList;
import com.mb.android.media.PlaybackService;
import com.mb.android.media.VLCInstance;
import com.mb.android.media.VLCOptions;
import com.mb.android.media.VlcEventHandler;
import com.mb.android.media.WeakHandler;

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
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.android.mediabrowser.Constants;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;

public class KitKatMediaService extends Service implements IVLCVout.Callback {

    private static final String TAG = "VLC/PlaybackService";

    public static final String START_FROM_NOTIFICATION = "from_notification";

    private static final int SHOW_PROGRESS = 0;
    private static final int SHOW_TOAST = 1;

    private ILogger logger;
    private IJsonSerializer jsonSerializer = new GsonJsonSerializer();

    // Indicates whether the service was started.
    private boolean mServiceStarted;

    public interface Callback {
        void update();
        void updateProgress();
        void onMediaEvent(Media.Event event);
        void onMediaPlayerEvent(MediaPlayer.Event event);
    }

    private class LocalBinder extends Binder {
        KitKatMediaService getService() {
            return KitKatMediaService.this;
        }
    }
    public static KitKatMediaService getService(IBinder iBinder) {
        LocalBinder binder = (LocalBinder) iBinder;
        return binder.getService();
    }

    private final IBinder mBinder = new LocalBinder();
    private MediaWrapperList mMediaList = new MediaWrapperList();
    private MediaPlayer mMediaPlayer;
    private boolean mIsAudioTrack = false;
    private boolean mHasHdmiAudio = false;

    final private ArrayList<Callback> mCallbacks = new ArrayList<Callback>();
    private boolean mDetectHeadset = true;
    private boolean mPebbleEnabled;
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
    private boolean mShuffling = false;
    private RepeatType mRepeating = RepeatType.None;
    private Random mRandom = null; // Used in shuffling process

    private boolean mHasAudioFocus = false;
    // RemoteControlClient-related
    /**
     * RemoteControlClient is for lock screen playback control.
     */
    private RemoteControlClient mRemoteControlClient = null;
    private RemoteControlClientReceiver mRemoteControlClientReceiver = null;
    /**
     * Last widget position update timestamp
     */
    private long mWidgetPositionTimestamp = Calendar.getInstance().getTimeInMillis();
    private ComponentName mRemoteControlClientReceiverComponent;

    private PlayerListener mMediaPlayerListener;

    private static LibVLC LibVLC(Context context, ILogger logger) {
        return VLCInstance.get(context, logger);
    }

    private MediaPlayer newMediaPlayer() {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        final MediaPlayer mp = new MediaPlayer(LibVLC(getApplicationContext(), logger));
        final String aout = VLCOptions.getAout(pref, logger);
        if (mp.setAudioOutput(aout) && aout.equals("android_audiotrack")) {
            mIsAudioTrack = true;
            if (mHasHdmiAudio)
                mp.setAudioOutputDevice("hdmi");
        } else
            mIsAudioTrack = false;
        return mp;
    }

    public static enum RepeatType {
        None,
        Once,
        All
    }

    private static boolean isFroyoOrLater(){
        return true;
    }

    private static boolean readPhoneState() {
        return !isFroyoOrLater();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        logger = AppLogger.getLogger(getApplicationContext());

        mMediaPlayer = newMediaPlayer();
        mMediaPlayerListener = new PlayerListener(logger, mMediaPlayer);
        mMediaPlayer.getVLCVout().addCallback(this);

        if (!VLCInstance.testCompatibleCPU(this)) {
            stopSelf();
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mDetectHeadset = prefs.getBoolean("enable_headset_detection", true);

        mCurrentIndex = -1;
        mPrevIndex = -1;
        mNextIndex = -1;
        mPrevious = new Stack<Integer>();
        mRemoteControlClientReceiverComponent = new ComponentName(BuildConfig.APPLICATION_ID, RemoteControlClientReceiver.class.getName());

        // Make sure the audio player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        IntentFilter filter = new IntentFilter();
        filter.setPriority(Integer.MAX_VALUE);
        filter.addAction(Constants.ACTION_PLAYPAUSE);
        filter.addAction(Constants.ACTION_NEXT);
        filter.addAction(Constants.ACTION_PREVIOUS);
        filter.addAction(Constants.ACTION_PLAY);
        filter.addAction(Constants.ACTION_PAUSE);
        filter.addAction(Constants.ACTION_UNPAUSE);
        filter.addAction(Constants.ACTION_STOP);
        filter.addAction(Constants.ACTION_SEEK);
        filter.addAction(Constants.SLEEP_INTENT);

        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        if (readPhoneState()) {
            filter.addAction(Constants.INCOMING_CALL_INTENT);
            filter.addAction(Constants.CALL_ENDED_INTENT);
        }

        registerReceiver(mReceiver, filter);
        registerV21();

        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean stealRemoteControl = pref.getBoolean("enable_steal_remote_control", false);

        if (!isFroyoOrLater() || stealRemoteControl) {
            /* Backward compatibility for API 7 */
            filter = new IntentFilter();
            if (stealRemoteControl)
                filter.setPriority(Integer.MAX_VALUE);
            filter.addAction(Intent.ACTION_MEDIA_BUTTON);
            mRemoteControlClientReceiver = new RemoteControlClientReceiver();
            registerReceiver(mRemoteControlClientReceiver, filter);
        }
        try {
            getPackageManager().getPackageInfo("com.getpebble.android", PackageManager.GET_ACTIVITIES);
            mPebbleEnabled = true;
        } catch (PackageManager.NameNotFoundException e) {
            mPebbleEnabled = false;
        }
    }

    /**
     * A function to control the Remote Control Client. It is needed for
     * compatibility with devices below Ice Cream Sandwich (4.0).
     *
     * @param state Playback state
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setRemoteControlClientPlaybackState(int state) {
        if (!AndroidUtil.isICSOrLater() || mRemoteControlClient == null)
            return;

        switch (state) {
            case MediaPlayer.Event.Playing:
                mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
                break;
            case MediaPlayer.Event.Paused:
                mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
                break;
            case MediaPlayer.Event.Stopped:
                mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
                break;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
            return START_STICKY;
        handleIntent(this, intent);

        return super.onStartCommand(intent, flags, startId);
    }

    private void handleIntent(Context context, Intent intent) {

        String action = intent.getAction();

        if (action == null) {
            action = "";
        }
        int state = intent.getIntExtra("state", 0);
        if( mMediaPlayer == null ) {
            Log.w(TAG, "Intent received, but VLC is not loaded, skipping.");
            return;
        }

        if (readPhoneState()) {
                /*
                 * Incoming Call : Pause if VLC is playing audio or video.
                 */
            if (action.equalsIgnoreCase(Constants.INCOMING_CALL_INTENT)) {
                mWasPlayingAudio = mMediaPlayer.isPlaying() && hasCurrentMedia();
                if (mWasPlayingAudio)
                    pause();
            }

                /*
                 * Call ended : Play only if VLC was playing audio.
                 */
            if (action.equalsIgnoreCase(Constants.CALL_ENDED_INTENT)
                    && mWasPlayingAudio) {
                play();
            }
        }

        // skip all headsets events if there is a call
        TelephonyManager telManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (telManager != null && telManager.getCallState() != TelephonyManager.CALL_STATE_IDLE)
            return;

            /*
             * Launch the activity if needed
             */
        if (action.startsWith(PlaybackService.ACTION_REMOTE_GENERIC) && !mMediaPlayer.isPlaying() && !hasCurrentMedia()) {
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
        }

            /*
             * headset plug events
             */
        if (mDetectHeadset && !mHasHdmiAudio) {
            if (action.equalsIgnoreCase(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                Log.i(TAG, "Headset Removed.");
                if (mMediaPlayer.isPlaying() && hasCurrentMedia())
                    pause();
            }
            else if (action.equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG) && state != 0) {
                Log.i(TAG, "Headset Inserted.");
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
            startService(new Intent(getApplicationContext(), KitKatMediaService.class));
            mServiceStarted = true;
        }

        String path = intent.getStringExtra("path");
        String itemJson = intent.getStringExtra("item");
        String mediaSourceJson = intent.getStringExtra("mediaSource");
        posterUrl = intent.getStringExtra("posterUrl");

        currentItem = jsonSerializer.DeserializeFromString(itemJson, BaseItemDto.class);

        final MediaWrapper mw = new MediaWrapper(Uri.parse(path));
        load(mw);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
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

    public IVLCVout getVLCVout()  {
        return mMediaPlayer.getVLCVout();
    }

    private final OnAudioFocusChangeListener mAudioFocusListener = isFroyoOrLater() ?
            createOnAudioFocusChangeListener() : null;

    @TargetApi(Build.VERSION_CODES.FROYO)
    private OnAudioFocusChangeListener createOnAudioFocusChangeListener() {
        return new OnAudioFocusChangeListener() {
            private boolean mLossTransient = false;
            private boolean mLossTransientCanDuck = false;

            @Override
            public void onAudioFocusChange(int focusChange) {
                /*
                 * Pause playback during alerts and notifications
                 */
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        Log.i(TAG, "AUDIOFOCUS_LOSS");
                        // Stop playback
                        changeAudioFocus(false);
                        stop();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
                        // Pause playback
                        if (mMediaPlayer.isPlaying()) {
                            mLossTransient = true;
                            mMediaPlayer.pause();
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                        // Lower the volume
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.setVolume(36);
                            mLossTransientCanDuck = true;
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        Log.i(TAG, "AUDIOFOCUS_GAIN: " + mLossTransientCanDuck + ", " + mLossTransient);
                        // Resume playback
                        if (mLossTransientCanDuck) {
                            mMediaPlayer.setVolume(100);
                            mLossTransientCanDuck = false;
                        }
                        if (mLossTransient) {
                            mMediaPlayer.play();
                            mLossTransient = false;
                        }
                        break;
                }
            }
        };
    }

    /**
     * Set up the remote control and tell the system we want to be the default receiver for the MEDIA buttons
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void changeRemoteControlClient(AudioManager am, boolean acquire) {
        if (acquire) {
            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            mediaButtonIntent.setComponent(mRemoteControlClientReceiverComponent);
            PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);

            // create and register the remote control client
            mRemoteControlClient = new RemoteControlClient(mediaPendingIntent);
            am.registerRemoteControlClient(mRemoteControlClient);

            mRemoteControlClient.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                            RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                            RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                            RemoteControlClient.FLAG_KEY_MEDIA_STOP);
        } else {
            am.unregisterRemoteControlClient(mRemoteControlClient);
            mRemoteControlClient = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private void changeAudioFocusFroyoOrLater(boolean acquire) {
        final AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
        if (am == null)
            return;

        if (acquire) {
            if (!mHasAudioFocus) {
                final int result = am.requestAudioFocus(mAudioFocusListener,
                        AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    am.setParameters("bgm_state=true");
                    am.registerMediaButtonEventReceiver(mRemoteControlClientReceiverComponent);
                    if (AndroidUtil.isICSOrLater())
                        changeRemoteControlClient(am, acquire);
                    mHasAudioFocus = true;
                }
            }
        } else {
            if (mHasAudioFocus) {
                final int result = am.abandonAudioFocus(mAudioFocusListener);
                am.setParameters("bgm_state=false");
                am.unregisterMediaButtonEventReceiver(mRemoteControlClientReceiverComponent);
                if (AndroidUtil.isICSOrLater())
                    changeRemoteControlClient(am, acquire);
                mHasAudioFocus = false;
            }
        }
    }

    private void changeAudioFocus(boolean acquire) {
        if (isFroyoOrLater())
            changeAudioFocusFroyoOrLater(acquire);
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
        handleVout();
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {
        handleVout();
    }

    private final Media.EventListener mMediaListener = new Media.EventListener() {
        @Override
        public void onEvent(Media.Event event) {
            switch (event.type) {
                case Media.Event.ParsedChanged:
                    Log.i(TAG, "Media.Event.ParsedChanged");
                    final MediaWrapper mw = getCurrentMedia();
                    if (mw != null)
                        mw.updateMeta(mMediaPlayer);
                    executeUpdate();
                    showNotification();
                    updateRemoteControlClientMetadata();
                    break;
                case Media.Event.MetaChanged:
                    break;
            }
            for (Callback callback : mCallbacks)
                callback.onMediaEvent(event);
        }
    };

    private class PlayerListener extends VlcEventHandler {

        public PlayerListener(ILogger logger, MediaPlayer mLibVLC) {
            super(logger, mLibVLC);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {

            super.onEvent(event);

            switch (event.type) {
                case MediaPlayer.Event.Playing:
                    Log.i(TAG, "MediaPlayer.Event.Playing");
                    executeUpdate();
                    executeUpdateProgress();

                    changeAudioFocus(true);
                    setRemoteControlClientPlaybackState(event.type);
                    showNotification();
                    if (!mWakeLock.isHeld())
                        mWakeLock.acquire();
                    break;
                case MediaPlayer.Event.Paused:
                    Log.i(TAG, "MediaPlayer.Event.Paused");
                    executeUpdate();
                    executeUpdateProgress();
                    showNotification();
                    setRemoteControlClientPlaybackState(event.type);
                    if (mWakeLock.isHeld())
                        mWakeLock.release();
                    break;
                case MediaPlayer.Event.Stopped:
                    Log.i(TAG, "MediaPlayer.Event.Stopped");
                    executeUpdate();
                    executeUpdateProgress();
                    setRemoteControlClientPlaybackState(event.type);
                    if (mWakeLock.isHeld())
                        mWakeLock.release();
                    changeAudioFocus(false);
                    break;
                case MediaPlayer.Event.EndReached:
                    Log.i(TAG, "MediaPlayerEndReached");
                    executeUpdate();
                    executeUpdateProgress();
                    determinePrevAndNextIndices(true);
                    next();
                    if (mWakeLock.isHeld())
                        mWakeLock.release();
                    changeAudioFocus(false);
                    break;
                case MediaPlayer.Event.EncounteredError:
                    showToast(getString(R.string.invalid_location, mMediaList.getMRL(mCurrentIndex)), Toast.LENGTH_SHORT);
                    executeUpdate();
                    executeUpdateProgress();
                    next();
                    if (mWakeLock.isHeld())
                        mWakeLock.release();
                    break;
                case MediaPlayer.Event.TimeChanged:
                    break;
                case MediaPlayer.Event.PositionChanged:
                    break;
                case MediaPlayer.Event.Vout:
                    break;
                case MediaPlayer.Event.ESAdded:
                    if (event.getEsChangedType() == Media.Track.Type.Video) {
                        if (!handleVout()) {
                            /* Update notification content intent: resume video or resume audio activity */
                            showNotification();
                        }
                    }
                    break;
                case MediaPlayer.Event.ESDeleted:
                    break;
            }
            for (Callback callback : mCallbacks)
                callback.onMediaPlayerEvent(event);
        }
    };

    private final MediaWrapperList.EventListener mListEventListener = new MediaWrapperList.EventListener() {

        @Override
        public void onItemAdded(int index, String mrl) {
            Log.i(TAG, "CustomMediaListItemAdded");
            if(mCurrentIndex >= index && !mExpanding.get())
                mCurrentIndex++;

            determinePrevAndNextIndices();
            executeUpdate();
        }

        @Override
        public void onItemRemoved(int index, String mrl) {
            Log.i(TAG, "CustomMediaListItemDeleted");
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
            Log.i(TAG, "CustomMediaListItemMoved");
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
        return hasCurrentMedia() && mMediaPlayer.getVideoTracksCount() > 0;
    }

    private boolean handleVout() {
        if (!canSwitchToVideo() || !mMediaPlayer.isPlaying())
            return false;
        if (mMediaPlayer.getVLCVout().areViewsAttached()) {
            hideNotification(false);
            return true;
        } else
            return false;
    }

    @MainThread
    public boolean switchToVideo() {
        if (!canSwitchToVideo())
            return false;
        if (!mMediaPlayer.getVLCVout().areViewsAttached()) {
            //VideoPlayerActivity.startOpened(getApplicationContext(), mCurrentIndex);
        }
        return true;
    }

    private void executeUpdate() {
        executeUpdate(true);
    }

    private void executeUpdate(Boolean updateWidget) {
        for (Callback callback : mCallbacks) {
            callback.update();
        }
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
        return mCurrentIndex >= 0 && mCurrentIndex < mMediaList.size();
    }

    private final Handler mHandler = new AudioServiceHandler(this);

    private static class AudioServiceHandler extends WeakHandler<KitKatMediaService> {
        public AudioServiceHandler(KitKatMediaService fragment) {
            super(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            KitKatMediaService service = getOwner();
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
                    final Bundle bundle = msg.getData();
                    final String text = bundle.getString("text");
                    final int duration = bundle.getInt("duration");
                    //Toast.makeText(service.getApplicationContext(), text, duration).show();
                    break;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void showNotification() {

        if (posterUrl == null || posterUrl.length() == 0) {
            showNotification(null);
            return;
        }

        GetHttpClient().getBitmap(posterUrl, new Response<Bitmap>(){

            @Override
            public void onResponse(Bitmap bitmap) {
                showNotification(bitmap);
            }

            @Override
            public void onError(Exception ex) {

                showNotification(null);
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void showNotification(Bitmap cover) {
        if (mMediaPlayer.getVLCVout().areViewsAttached())
            return;
        try {

            BaseItemDto item = currentItem;
            if (item == null)
                return;

            String album = item.getAlbum() == null ? "" : item.getAlbum();
            String artist = item.getArtistItems() != null && item.getArtistItems().size() > 0 ? item.getArtistItems().get(0).getName() : "";
            String title = item.getName() == null ? "" : item.getName();
            String itemId = item.getId();
            Notification notification;

            // add notification to status bar
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.icon)
                    .setTicker(title + " - " + artist)
                    .setAutoCancel(false)
                    .setOngoing(true);

            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(Constants.ACTION_SHOW_PLAYER);
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            notificationIntent.putExtra(START_FROM_NOTIFICATION, true);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            boolean isPlaying = hasCurrentMedia();

            if (AndroidUtil.isJellyBeanOrLater()) {

                Intent iBackward = new Intent(Constants.ACTION_PREVIOUS);
                Intent iPlay = new Intent(Constants.ACTION_PLAYPAUSE);
                Intent iForward = new Intent(Constants.ACTION_NEXT);
                Intent iStop = new Intent(Constants.ACTION_STOP);
                PendingIntent piBackward = PendingIntent.getBroadcast(this, 0, iBackward, PendingIntent.FLAG_UPDATE_CURRENT);
                PendingIntent piPlay = PendingIntent.getBroadcast(this, 0, iPlay, PendingIntent.FLAG_UPDATE_CURRENT);
                PendingIntent piForward = PendingIntent.getBroadcast(this, 0, iForward, PendingIntent.FLAG_UPDATE_CURRENT);

                RemoteViews view_expanded = new RemoteViews(getPackageName(), R.layout.notification_expanded);
                if (cover != null)
                    view_expanded.setImageViewBitmap(R.id.cover, cover);
                view_expanded.setTextViewText(R.id.songName, title);
                view_expanded.setTextViewText(R.id.artist, artist);
                view_expanded.setTextViewText(R.id.album, album);
                view_expanded.setImageViewResource(R.id.play_pause, isPlaying ? R.drawable.ic_pause_w : R.drawable.ic_play_w);
                view_expanded.setOnClickPendingIntent(R.id.backward, piBackward);
                view_expanded.setOnClickPendingIntent(R.id.play_pause, piPlay);
                view_expanded.setOnClickPendingIntent(R.id.forward, piForward);
                view_expanded.setOnClickPendingIntent(R.id.content, pendingIntent);

                notification = builder.build();
                notification.contentView = view_expanded;
                notification.bigContentView = view_expanded;
            }
            else {
                if (cover != null) {
                    builder.setLargeIcon(cover);
                }
                builder.setContentTitle(title)
                        .setContentText(artist)
                        .setContentInfo(album)
                        .setContentIntent(pendingIntent);
                notification = builder.build();
            }

            startService(new Intent(this, KitKatMediaService.class));
            startForeground(3, notification);
        }
        catch (NoSuchMethodError e){
            // Compat library is wrong on 3.2
            // http://code.google.com/p/android/issues/detail?id=36359
            // http://code.google.com/p/android/issues/detail?id=36502
        }
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
        if(stopPlayback) {
            stopSelf();
            mServiceStarted = false;
        }
    }

    @MainThread
    public void pause() {
        mHandler.removeMessages(SHOW_PROGRESS);
        // hideNotification(); <-- see event handler
        mMediaPlayer.pause();
        broadcastMetadata();
    }

    @MainThread
    public void play() {
        if(hasCurrentMedia()) {
            mMediaPlayer.play();
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
            showNotification();
            broadcastMetadata();
        }
    }

    @MainThread
    public void stop() {
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
        destroyCurrentMediaInfo();
        setRemoteControlClientPlaybackState(MediaPlayer.Event.Stopped);
        mCurrentIndex = -1;
        mPrevious.clear();
        mHandler.removeMessages(SHOW_PROGRESS);
        hideNotification();
        broadcastMetadata();
        executeUpdate();
        executeUpdateProgress();
        changeAudioFocus(false);
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
            if (mRepeating == RepeatType.Once) {
                mPrevIndex = mNextIndex = mCurrentIndex;
            } else {

                if(mShuffling) {
                    if(mPrevious.size() > 0)
                        mPrevIndex = mPrevious.peek();
                    // If we've played all songs already in shuffle, then either
                    // reshuffle or stop (depending on RepeatType).
                    if(mPrevious.size() + 1 == size) {
                        if(mRepeating == RepeatType.None) {
                            mNextIndex = -1;
                            return;
                        } else {
                            mPrevious.clear();
                        }
                    }
                    if(mRandom == null) mRandom = new Random();
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
                        if(mRepeating == RepeatType.None) {
                            mNextIndex = -1;
                        } else {
                            mNextIndex = 0;
                        }
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void updateRemoteControlClientMetadata() {
        if (!AndroidUtil.isICSOrLater()) // NOP check
            return;

        BaseItemDto item = currentItem;

        String album = item.getAlbum() == null ? "" : item.getAlbum();
        String artist = item.getArtistItems() != null && item.getArtistItems().size() > 0 ? item.getArtistItems().get(0).getName() : "";
        String genre = item.getGenres() != null && item.getGenres().size() > 0 ? item.getGenres().get(0) : "";
        String albumArtist = item.getAlbumArtist() == null ? "" : item.getAlbumArtist();
        String title = item.getName() == null ? "" : item.getName();
        String itemId = item.getId();

        if (mRemoteControlClient != null && item != null) {
            final RemoteControlClient.MetadataEditor editor = mRemoteControlClient.editMetadata(true);
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, album);
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, artist);
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, albumArtist);
            editor.putString(MediaMetadataRetriever.METADATA_KEY_GENRE, genre);
            editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, title);

            // TODO: Add duration
            //editor.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, media.getLength());

            if (posterUrl == null || posterUrl.length() == 0) {
                editor.apply();
                return;
            }

            GetHttpClient().getBitmap(posterUrl, new Response<Bitmap>() {

                @Override
                public void onResponse(Bitmap bitmap) {

                    try {
                        editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, bitmap);
                        editor.apply();
                    } catch (IllegalStateException ex) {

                        // Occasionally seeing this exception: Caused by: java.lang.IllegalStateException: Can't parcel a recycled bitmap
                        logger.ErrorException("Error applying bitmap to notification", ex);

                        editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, null);
                        editor.apply();
                    }
                }

                @Override
                public void onError(Exception ex) {

                    editor.apply();
                }
            });
        }
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
        showNotification();
        broadcastMetadata();
        updateRemoteControlClientMetadata();
    }

    private void onMediaChanged() {
        notifyTrackChanged();

        saveCurrentMedia();
        determinePrevAndNextIndices();
    }

    private void onMediaListChanged() {
        saveMediaList();
        executeUpdate();
        determinePrevAndNextIndices();
    }

    @MainThread
    public void next() {
        mPrevious.push(mCurrentIndex);
        mCurrentIndex = mNextIndex;

        int size = mMediaList.size();
        if (size == 0 || mCurrentIndex < 0 || mCurrentIndex >= size) {
            if (mCurrentIndex < 0)
                saveCurrentMedia();
            Log.w(TAG, "Warning: invalid next index, aborted !");
            stop();
            return;
        }

        playIndex(mCurrentIndex, 0);
        onMediaChanged();
    }

    @MainThread
    public void previous() {
        mCurrentIndex = mPrevIndex;
        if (mPrevious.size() > 0)
            mPrevious.pop();

        int size = mMediaList.size();
        if (size == 0 || mPrevIndex < 0 || mCurrentIndex >= size) {
            Log.w(TAG, "Warning: invalid previous index, aborted !");
            stop();
            return;
        }

        playIndex(mCurrentIndex, 0);
        onMediaChanged();
    }

    @MainThread
    public void shuffle() {
        if (mShuffling)
            mPrevious.clear();
        mShuffling = !mShuffling;
        saveCurrentMedia();
        determinePrevAndNextIndices();
    }

    @MainThread
    public void setRepeatType(RepeatType t) {
        mRepeating = t;
        saveCurrentMedia();
        determinePrevAndNextIndices();
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

        sendBroadcast(broadcast);
    }

    private synchronized void saveCurrentMedia() {
        /*SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString("current_media", mMediaList.getMRL(Math.max(mCurrentIndex, 0)));
        editor.putBoolean("shuffling", mShuffling);
        editor.putInt("repeating", mRepeating.ordinal());
        Util.commitPreferences(editor);*/
    }

    private synchronized void saveMediaList() {
        /*StringBuilder locations = new StringBuilder();
        for (int i = 0; i < mMediaList.size(); i++)
            locations.append(" ").append(Uri.encode(mMediaList.getMRL(i)));
        //We save a concatenated String because putStringSet is APIv11.
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString("media_list", locations.toString().trim());
        Util.commitPreferences(editor);*/
    }

    private synchronized void savePosition(){
        /*SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt("position_in_list", mCurrentIndex);
        editor.putLong("position_in_song", mMediaPlayer.getTime());
        Util.commitPreferences(editor);*/
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
    public boolean isShuffling() {
        return mShuffling;
    }

    @MainThread
    public RepeatType getRepeatType() {
        return mRepeating;
    }

    @MainThread
    public boolean hasMedia()  {
        return hasCurrentMedia();
    }

    @MainThread
    public boolean isVideoPlaying() {
        return mMediaPlayer.getVLCVout().areViewsAttached();
    }

    @MainThread
    public String getTitle() {
        if (hasCurrentMedia())
            return getCurrentMedia().getTitle();
        else
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
        return mMediaPlayer.getTime();
    }

    @MainThread
    public long getLength() {
        return  mMediaPlayer.getLength();
    }

    @MainThread
    public void load(List<MediaWrapper> mediaList, int position) {
        Log.v(TAG, "Loading position " + ((Integer) position).toString() + " in " + mediaList.toString());

        mMediaList.removeEventListener(mListEventListener);
        mMediaList.clear();
        MediaWrapperList currentMediaList = mMediaList;

        mPrevious.clear();

        for (int i = 0; i < mediaList.size(); i++) {
            currentMediaList.add(mediaList.get(i));
        }

        if (mMediaList.size() == 0) {
            Log.w(TAG, "Warning: empty media list, nothing to play !");
            return;
        }
        if (mMediaList.size() > position && position >= 0) {
            mCurrentIndex = position;
        } else {
            Log.w(TAG, "Warning: positon " + position + " out of bounds");
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
            Log.w(TAG, "Warning: empty media list, nothing to play !");
            return;
        }
        if (index >= 0 && index < mMediaList.size()) {
            mCurrentIndex = index;
        } else {
            Log.w(TAG, "Warning: index " + index + " out of bounds");
            mCurrentIndex = 0;
        }

        String mrl = mMediaList.getMRL(index);
        if (mrl == null)
            return;
        final MediaWrapper mw = mMediaList.getMedia(index);
        if (mw == null)
            return;

        final Media media = new Media(VLCInstance.get(getApplicationContext(), logger), mw.getUri());
        VLCOptions.setMediaOptions(media, this, flags | mw.getFlags());
        media.setEventListener(mMediaListener);
        mMediaPlayer.setMedia(media);
        media.release();
        mMediaPlayer.setEqualizer(VLCOptions.getEqualizer(this));
        mMediaPlayer.setVideoTitleDisplay(MediaPlayer.Position.Disable, 0);
        changeAudioFocus(true);
        mMediaPlayer.setEventListener(mMediaPlayerListener);
        mMediaPlayer.play();

        notifyTrackChanged();
        determinePrevAndNextIndices();
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

    /**
     * Use this function to show an URI in the audio interface WITHOUT
     * interrupting the stream.
     *
     * Mainly used by VideoPlayerActivity in response to loss of video track.
     */
    @MainThread
    public void showWithoutParse(int index) {
        String URI = mMediaList.getMRL(index);
        Log.v(TAG, "Showing index " + index + " with playing URI " + URI);
        // Show an URI without interrupting/losing the current stream

        if(URI == null || !mMediaPlayer.isPlaying())
            return;
        mCurrentIndex = index;

        notifyTrackChanged();
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

    @MainThread
    public void remove(int position) {
        mMediaList.remove(position);
        onMediaListChanged();
    }

    @MainThread
    public void removeLocation(String location) {
        mMediaList.remove(location);
        onMediaListChanged();
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
        return KitKatMediaService.this.getCurrentMedia();
    }

    @MainThread
    public void setTime(long time) {
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
    public void setRate(float rate) {
        mMediaPlayer.setRate(rate);
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
    public void setPosition(float pos) {
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
    public boolean addSubtitleTrack(String path) {
        // TODO This is deprecated now?
        return false;
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
            for (int i = 0; i < ml.getCount(); ++i) {
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
            void onConnected(KitKatMediaService service);
            void onDisconnected();
        }

        private boolean mBound = false;
        private final Callback mCallback;
        private final Context mContext;

        private final ServiceConnection mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder iBinder) {
                Log.d(TAG, "Service Connected");
                if (!mBound)
                    return;

                final KitKatMediaService service = KitKatMediaService.getService(iBinder);
                if (service != null)
                    mCallback.onConnected(service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "Service Disconnected");
                mCallback.onDisconnected();
            }
        };

        private static Intent getServiceIntent(Context context) {
            return new Intent(context, KitKatMediaService.class);
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