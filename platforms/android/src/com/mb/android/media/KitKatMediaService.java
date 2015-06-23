package com.mb.android.media;

import java.util.Random;
import java.util.Stack;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.LibVlcUtil;
import org.videolan.libvlc.Media;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.mb.android.MainActivity;
import com.mb.android.R;
import com.mb.android.api.ApiClientBridge;

import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.logging.ConsoleLogger;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;

public class KitKatMediaService extends Service {

    private static final String TAG = "VLC/AudioService";

    public static final String ACTION_REMOTE_GENERIC = "com.mb.android.remote.";
    public static final String START_FROM_NOTIFICATION = "from_notification";
    public static final String SLEEP_INTENT = "com.mb.android.sleep";
    public static final String INCOMING_CALL_INTENT = "com.mb.android.media.incomingcall";
    public static final String CALL_ENDED_INTENT = "com.mb.android.media.callended";

    private LibVLC mLibVLC;
    private OnAudioFocusChangeListener audioFocusListener;
    private boolean mDetectHeadset = true;
    private PowerManager.WakeLock mWakeLock;
    private ILogger logger;
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
             * Launch the activity if needed
             */
        if (action.startsWith(ACTION_REMOTE_GENERIC) && !mLibVLC.isPlaying() && !hasCurrentMedia()) {
            Intent iVlc = new Intent(context, MainActivity.class);
            iVlc.putExtra(START_FROM_NOTIFICATION, true);
            iVlc.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(iVlc);
        }

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

    private BaseItemDto currentItem;
    private String posterUrl;

    private void play(Context context, Intent intent) {

        String path = intent.getStringExtra("path");
        String itemJson = intent.getStringExtra("item");
        String mediaSourceJson = intent.getStringExtra("mediaSource");
        posterUrl = intent.getStringExtra("posterUrl");

        currentItem = jsonSerializer.DeserializeFromString(itemJson, BaseItemDto.class);

        Media media = new Media(mLibVLC, path);
        mLibVLC.playMRL(media.getMrl());
        currentMrl = media.getMrl();
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

        private KitKatMediaService audioService;

        public AudioServiceEventHandler(ILogger logger, LibVLC mLibVLC, KitKatMediaService audioService) {
            super(logger, mLibVLC);
            this.audioService = audioService;
        }

        @Override
        public void handleMessage(Message msg) {

            super.handleMessage(msg);

            KitKatMediaService service = audioService;
            if(service == null) return;

            switch (msg.getData().getInt("event")) {
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

        try {

            BaseItemDto item = currentItem;
            if (item == null)
                return;

            String album = item.getAlbum() == null ? "" : item.getAlbum();
            String artist = item.getArtistItems().size() > 0 ? item.getArtistItems().get(0).getName() : "";
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
            notificationIntent.setAction(MainActivity.ACTION_SHOW_PLAYER);
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            notificationIntent.putExtra(START_FROM_NOTIFICATION, true);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            if (cover != null) {
                builder.setLargeIcon(cover);
            }
            if (LibVlcUtil.isJellyBeanOrLater()) {

                builder.setContentTitle(title)
                        .setContentText(artist)
                        .setContentInfo(album)
                        .setContentIntent(pendingIntent);
                notification = builder.build();
            }
            else {
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
}