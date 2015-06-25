package com.mb.android.media.legacy;

import java.lang.ref.WeakReference;
import java.util.Calendar;

import org.apache.http.protocol.HTTP;
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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.mb.android.MainActivity;
import com.mb.android.R;
import com.mb.android.api.ApiClientBridge;
import com.mb.android.media.Constants;
import com.mb.android.media.MediaWidgetProvider;
import com.mb.android.media.VlcEventHandler;

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

    public static final String WIDGET_CLASS = "com.mb.android.media.MediaWidgetProvider";

    private LibVLC mLibVLC;
    private OnAudioFocusChangeListener audioFocusListener;
    private boolean mDetectHeadset = true;
    private PowerManager.WakeLock mWakeLock;
    private ILogger logger;
    private IJsonSerializer jsonSerializer = new GsonJsonSerializer();

    private static boolean mWasPlayingAudio = false;
    /**
     * Last widget position update timestamp
     */
    private long mWidgetPositionTimestamp = Calendar.getInstance().getTimeInMillis();
    // Indicates whether the service was started.
    private boolean mServiceStarted;
    // Delay stopSelf by using a handler.
    private static final int STOP_DELAY = 30000;
    private DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);

    // RemoteControlClient-related
    /**
     * RemoteControlClient is for lock screen playback control.
     */
    private RemoteControlClient mRemoteControlClient = null;
    private RemoteControlClientReceiver mRemoteControlClientReceiver = null;
    private ComponentName mRemoteControlClientReceiverComponent;

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

        mRemoteControlClientReceiverComponent = new ComponentName(getPackageName(),
                RemoteControlClientReceiver.class.getName());

        // Make sure the audio player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
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
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        filter.addAction(SLEEP_INTENT);
        filter.addAction(INCOMING_CALL_INTENT);
        filter.addAction(CALL_ENDED_INTENT);
        filter.addAction(MediaWidgetProvider.ACTION_WIDGET_INIT);
        registerReceiver(serviceReceiver, filter);
    }


    /**
     * Set up the remote control and tell the system we want to be the default receiver for the MEDIA buttons
     * @see http://android-developers.blogspot.fr/2010/06/allowing-applications-to-play-nicer.html
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void setUpRemoteControlClient() {
        Context context = getApplicationContext();
        AudioManager audioManager = (AudioManager)context.getSystemService(AUDIO_SERVICE);

        if (LibVlcUtil.isICSOrLater()) {
            audioManager.registerMediaButtonEventReceiver(mRemoteControlClientReceiverComponent);

            if (mRemoteControlClient == null) {
                Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                mediaButtonIntent.setComponent(mRemoteControlClientReceiverComponent);
                PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(context, 0, mediaButtonIntent, 0);

                // create and register the remote control client
                mRemoteControlClient = new RemoteControlClient(mediaPendingIntent);
                audioManager.registerRemoteControlClient(mRemoteControlClient);
            }

            mRemoteControlClient.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                            RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                            RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                            RemoteControlClient.FLAG_KEY_MEDIA_STOP);
        } else if (LibVlcUtil.isFroyoOrLater()) {
            audioManager.registerMediaButtonEventReceiver(mRemoteControlClientReceiverComponent);
        }
    }

    /**
     * A function to control the Remote Control Client. It is needed for
     * compatibility with devices below Ice Cream Sandwich (4.0).
     *
     * @param p Playback state
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setRemoteControlClientPlaybackState(int state) {
        if (!LibVlcUtil.isICSOrLater() || mRemoteControlClient == null)
            return;

        switch (state) {
            case EventHandler.MediaPlayerPlaying:
                mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
                break;
            case EventHandler.MediaPlayerPaused:
                mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
                break;
            case EventHandler.MediaPlayerStopped:
                mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void updateRemoteControlClientMetadata() {

        if (!LibVlcUtil.isICSOrLater()) // NOP check
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
                    }
                    catch (IllegalStateException ex){

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

        updateWidget(this);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop(false);
        if (mWakeLock.isHeld())
            mWakeLock.release();
        unregisterReceiver(serviceReceiver);
        if (mRemoteControlClientReceiver != null) {
            unregisterReceiver(mRemoteControlClientReceiver);
            mRemoteControlClientReceiver = null;
        }
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
                            if (hasCurrentMedia())
                                pause();
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

    private void handleNextTrackCommand() {
        MainActivity.RespondToWebView("MediaController.nextTrack();");
    }

    private void handlePreviousTrackCommand() {
        MainActivity.RespondToWebView("MediaController.previousTrack();");
    }

    private void handleIntent(Context context, Intent intent) {

        String action = intent.getAction();
        if (action == null) action = "";

        logger.Debug("KitKatMediaService.handleIntent action=%s", action);

        if (action.equalsIgnoreCase(Constants.ACTION_PREVIOUS)) {
            handlePreviousTrackCommand();
        } else if (action.equalsIgnoreCase(Constants.ACTION_NEXT)) {
            handleNextTrackCommand();
        }

        if( mLibVLC == null ) {
            Log.w(TAG, "Intent received, but VLC is not loaded, skipping.");
            return;
        }

            /*
             * Incoming Call : Pause if VLC is playing audio or video.
             */
        if (action.equalsIgnoreCase(INCOMING_CALL_INTENT)) {
            mWasPlayingAudio = hasCurrentMedia() && mLibVLC.getVideoTracksCount() < 1;
            if (hasCurrentMedia())
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
                pause();
            else if (hasCurrentMedia())
                play();
        } else if (action.equalsIgnoreCase(Constants.ACTION_UNPAUSE)) {
            play();
        } else if (action.equalsIgnoreCase(Constants.ACTION_PAUSE)) {
            pause();
        } else if (action.equalsIgnoreCase(Constants.ACTION_STOP)) {
            stop(intent.getBooleanExtra("stopService", false));

        } else if (action.equalsIgnoreCase(Constants.ACTION_PLAY)) {
            play(context, intent);
        }
        else if (action.equalsIgnoreCase(Constants.ACTION_SEEK)) {

            seek(intent.getLongExtra("position", 0));

        }else if (action.equalsIgnoreCase(MediaWidgetProvider.ACTION_WIDGET_INIT)) {
            updateWidget(context);
        }

            /*
             * headset plug events
             */
        if (mDetectHeadset) {
            int state = intent.getIntExtra("state", 0);
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
            stop(true);
        }
    }

    private BaseItemDto currentItem;
    private String posterUrl;

    private void destroyCurrentMediaInfo() {
        currentMrl = null;
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

        Media media = new Media(mLibVLC, path);
        mLibVLC.playMRL(media.getMrl());
        currentMrl = media.getMrl();

        setUpRemoteControlClient();
        updateRemoteControlClientMetadata();
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
                    service.setRemoteControlClientPlaybackState(EventHandler.MediaPlayerPlaying);
                    service.showNotification();
                    if (!service.mWakeLock.isHeld())
                        service.mWakeLock.acquire();
                    break;
                case EventHandler.MediaPlayerPaused:
                    Log.i(TAG, "MediaPlayerPaused");
                    service.executeUpdate();
                    service.executeUpdateProgress();
                    service.showNotification();
                    service.setRemoteControlClientPlaybackState(EventHandler.MediaPlayerPaused);
                    if (service.mWakeLock.isHeld())
                        service.mWakeLock.release();
                    break;
                case EventHandler.MediaPlayerStopped:
                    Log.i(TAG, "MediaPlayerStopped");
                    service.executeUpdate();
                    service.executeUpdateProgress();
                    service.setRemoteControlClientPlaybackState(EventHandler.MediaPlayerStopped);
                    if (service.mWakeLock.isHeld())
                        service.mWakeLock.release();
                    break;
                case EventHandler.MediaPlayerEndReached:
                    Log.i(TAG, "MediaPlayerEndReached");
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

                    LibVLC vlc = service.mLibVLC;
                    // Make sure it hasn't been disposed
                    if (vlc != null) {
                        long length = vlc.getLength();
                        service.updateWidgetPosition(service, pos, length);
                    }
                    break;
                case EventHandler.MediaPlayerEncounteredError:
                    /*service.showToast(service.getString(
                            R.string.invalid_location,
                            service.mLibVLC.getMediaList().getMRL(
                                    service.mCurrentIndex)), Toast.LENGTH_SHORT);*/
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
        executeUpdate(true);
    }

    private void executeUpdate(Boolean updateWidget) {
        if (updateWidget)
            updateWidget(this);
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
            notificationIntent.setAction(MainActivity.ACTION_SHOW_PLAYER);
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            notificationIntent.putExtra(START_FROM_NOTIFICATION, true);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            boolean isPlaying = hasCurrentMedia();

            if (LibVlcUtil.isJellyBeanOrLater()) {

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

    /**
     * Hides the VLC notification and stops the service.
     *
     * @param stopService True to also stop playback at the same time. Set to false to preserve playback (e.g. for vout events)
     */
    private void hideNotification(boolean stopService) {

        stopForeground(true);
        if(stopService) {
            stopSelf();
            mServiceStarted = false;
        }
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

        LibVLC vlc = mLibVLC;

        if (vlc != null) {
            setUpRemoteControlClient();
            if (vlc.getPlayerState() != 4){
                vlc.pause();
            }
        }
    }

    private void play() {
        if(hasCurrentMedia()) {
            setUpRemoteControlClient();
            mLibVLC.play();
            showNotification();
            updateWidget(this);
        }
    }

    private void seek(long position) {

        LibVLC vlc = mLibVLC;

        if (vlc != null) {
            vlc.setTime(position);
        }
    }

    private void stop(boolean stopService) {

        LibVLC vlc = mLibVLC;

        if (vlc != null) {
            vlc.stop();
        }

        destroyCurrentMediaInfo();
        setRemoteControlClientPlaybackState(EventHandler.MediaPlayerStopped);
        hideNotification(stopService);
        executeUpdate();
        executeUpdateProgress();
        changeAudioFocus(false);
    }

    private void updateWidget(Context context) {

        Log.d(TAG, "Updating widget");
        updateWidgetState(context);
        updateWidgetCover(context);
    }

    private void updateWidgetState(Context context) {

        Intent i = new Intent();
        i.setClassName(MediaWidgetProvider.APP_PACKAGE, WIDGET_CLASS);
        i.setAction(MediaWidgetProvider.ACTION_WIDGET_UPDATE);

        LibVLC vlc = mLibVLC;

        // Make sure vlc hasn't been disposed
        if (hasCurrentMedia() && vlc != null) {

            BaseItemDto item = currentItem;

            String album = item.getAlbum() == null ? "" : item.getAlbum();
            String artist = item.getArtistItems() != null && item.getArtistItems().size() > 0 ? item.getArtistItems().get(0).getName() : "";
            String title = item.getName() == null ? "" : item.getName();
            String itemId = item.getId();

            i.putExtra("title", title);
            i.putExtra("artist", artist);

            int playerState = vlc.getPlayerState();

            // Expected states by web plugins are: IDLE/CLOSE=0, OPENING=1, BUFFERING=2, PLAYING=3, PAUSED=4, STOPPING=5, ENDED=6, ERROR=7
            boolean isPaused = playerState == 4;

            i.putExtra("isplaying", !isPaused);
        }
        else {
            i.putExtra("title", "Emby");
            i.putExtra("artist", "");

            i.putExtra("isplaying", false);
        }

        sendBroadcast(i);
    }

    private void updateWidgetCover(Context context)
    {
        if (posterUrl == null || posterUrl.length() == 0) {
            return;
        }

        GetHttpClient().getBitmap(posterUrl, new Response<Bitmap>(){

            @Override
            public void onResponse(Bitmap bitmap) {
                Intent i = new Intent();
                i.setClassName(MediaWidgetProvider.APP_PACKAGE, WIDGET_CLASS);
                i.setAction(MediaWidgetProvider.ACTION_WIDGET_UPDATE_COVER);
                i.putExtra("cover", bitmap);
                sendBroadcast(i);
            }

            @Override
            public void onError(Exception ex) {

            }
        });
    }

    private void updateWidgetPosition(Context context, float positionMs, float durationMs)
    {
        // no more than one widget update for each 1/50 of the song
        long timestamp = Calendar.getInstance().getTimeInMillis();
        if (!hasCurrentMedia() || timestamp - mWidgetPositionTimestamp < durationMs / 50)
            return;

        updateWidgetState(context);

        mWidgetPositionTimestamp = timestamp;
        Intent i = new Intent();
        i.setClassName(MediaWidgetProvider.APP_PACKAGE, WIDGET_CLASS);
        i.setAction(MediaWidgetProvider.ACTION_WIDGET_UPDATE_POSITION);
        i.putExtra("position", positionMs);
        sendBroadcast(i);
    }

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private static class DelayedStopHandler extends Handler {
        private final WeakReference<KitKatMediaService> mWeakReference;

        private DelayedStopHandler(KitKatMediaService service) {
            mWeakReference = new WeakReference<KitKatMediaService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            KitKatMediaService service = mWeakReference.get();
            if (service != null && service.mLibVLC != null) {
                if (service.hasCurrentMedia()) {
                    service.logger.Debug("Ignoring delayed stop since the media player is in use.");
                    return;
                }
                service.logger.Debug("Stopping service with delay handler.");
                service.stopSelf();
                service.mServiceStarted = false;
            }
        }
    }
}