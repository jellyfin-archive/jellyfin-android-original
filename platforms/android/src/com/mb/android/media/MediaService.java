package com.mb.android.media;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.LibVlcUtil;
import org.videolan.libvlc.Media;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.media.MediaBrowserService;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.mb.android.MainActivity;
import com.mb.android.R;
import com.mb.android.api.ApiClientBridge;
import com.mb.android.logging.AppLogger;
import com.mb.android.media.utils.CarHelper;
import com.mb.android.media.utils.MediaIDHelper;
import com.mb.android.media.utils.QueueHelper;

import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.logging.ConsoleLogger;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;

public class MediaService extends MediaBrowserService implements Playback.Callback, IMediaService {

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

    // "Now playing" queue:
    private List<MediaSession.QueueItem> mPlayingQueue;
    private int mCurrentIndexOnQueue;
    private MediaNotificationManager mMediaNotificationManager;

    // Indicates whether the service was started.
    private boolean mServiceStarted;
    private DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);
    private Playback mPlayback;
    private PackageValidator mPackageValidator;

    // Delay stopSelf by using a handler.
    private static final int STOP_DELAY = 30000;

    @Override
    public void onCreate() {
        super.onCreate();

        logger = AppLogger.getLogger(getApplicationContext());

        // Get libVLC instance
        try {
            mLibVLC = getLibVlcInstance();

            mVlcEventHandler = new VlcServiceEventHandler(logger, mLibVLC, this);

            EventHandler.getInstance().addHandler(mVlcEventHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }

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
        //registerReceiver(serviceReceiver, filter);

        mPlayingQueue = new ArrayList<MediaSession.QueueItem>();
        mMusicProvider = new MusicProvider();
        mPackageValidator = new PackageValidator(this);

        // Start a new MediaSession
        mSession = new MediaSession(this, "MediaService");
        setSessionToken(mSession.getSessionToken());
        mSession.setCallback(new MediaSessionCallback(logger));
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mPlayback = new Playback(this, mMusicProvider);
        mPlayback.setState(PlaybackState.STATE_NONE);
        mPlayback.setCallback(this);
        mPlayback.start();

        /*Context context = getApplicationContext();
        Intent intent = new Intent(context, MusicPlayerActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 99 *//*request code*//*,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mSession.setSessionActivity(pi);*/

        Bundle extras = new Bundle();
        CarHelper.setSlotReservationFlags(extras, true, true, true);
        mSession.setExtras(extras);

        updatePlaybackState(null);

        mMediaNotificationManager = new MediaNotificationManager(this, this, GetHttpClient());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            handleIntent(this, intent);
        }

        return START_STICKY;
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
    public void onDestroy() {
        super.onDestroy();

        logger.Debug("MediaService.onDestroy");

        // Service is being killed, so make sure we release our resources
        handleStopRequest(null);

        if (mWakeLock.isHeld())
            mWakeLock.release();

        EventHandler.getInstance().removeHandler(mVlcEventHandler);

        mDelayedStopHandler.removeCallbacksAndMessages(null);

        if (mSession != null){
            mSession.release();
        }

        if (mLibVLC != null){
            mLibVLC.destroy();
            mLibVLC = null;
        }
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {

        logger.Debug("OnGetRoot: clientPackageName=" + clientPackageName, "; clientUid=" + clientUid + " ; rootHints=", rootHints);

        // To ensure you are not allowing any arbitrary app to browse your app's contents, you
        // need to check the origin:
        if (!mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
            // If the request comes from an untrusted package, return null. No further calls will
            // be made to other media browsing methods.
            logger.Warn("OnGetRoot: IGNORING request from untrusted package " + clientPackageName);
            return null;
        }

        //noinspection StatementWithEmptyBody
        if (CarHelper.isValidCarPackage(clientPackageName)) {
            // Optional: if your app needs to adapt ads, music library or anything else that
            // needs to run differently when connected to the car, this is where you should handle
            // it.
        }

        return new BrowserRoot(MediaIDHelper.MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(final String parentMediaId, final Result<List<MediaBrowser.MediaItem>> result) {

        if (!mMusicProvider.isInitialized()) {
            // Use result.detach to allow calling result.sendResult from another thread:
            result.detach();

            mMusicProvider.retrieveMediaAsync(new MusicProvider.Callback() {
                @Override
                public void onMusicCatalogReady(boolean success) {
                    if (success) {
                        loadChildrenImpl(parentMediaId, result);
                    } else {
                        updatePlaybackState(getString(R.string.error_no_metadata));
                        result.sendResult(Collections.<MediaBrowser.MediaItem>emptyList());
                    }
                }
            });

        } else {
            // If our music catalog is already loaded/cached, load them into result immediately
            loadChildrenImpl(parentMediaId, result);
        }
    }

    /**
     * Actual implementation of onLoadChildren that assumes that MusicProvider is already
     * initialized.
     */
    private void loadChildrenImpl(final String parentMediaId,
                                  final Result<List<MediaBrowser.MediaItem>> result) {

        logger.Debug("OnLoadChildren: parentMediaId=", parentMediaId);

        List<MediaBrowser.MediaItem> mediaItems = new ArrayList<MediaBrowser.MediaItem>();

        if (MediaIDHelper.MEDIA_ID_ROOT.equals(parentMediaId)) {
            logger.Debug("OnLoadChildren.ROOT");
            mediaItems.add(new MediaBrowser.MediaItem(
                    new MediaDescription.Builder()
                            .setMediaId(MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE)
                            .setTitle(getString(R.string.browse_genres))
                            .setIconUri(Uri.parse("android.resource://" +
                                    "com.example.android.mediabrowserservice/drawable/ic_by_genre"))
                            .setSubtitle(getString(R.string.browse_genre_subtitle))
                            .build(), MediaBrowser.MediaItem.FLAG_BROWSABLE
            ));

        } else if (MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE.equals(parentMediaId)) {
            logger.Debug("OnLoadChildren.GENRES");
            for (String genre : mMusicProvider.getGenres()) {
                MediaBrowser.MediaItem item = new MediaBrowser.MediaItem(
                        new MediaDescription.Builder()
                                .setMediaId(MediaIDHelper.createBrowseCategoryMediaID(MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE, genre))
                                .setTitle(genre)
                                .setSubtitle(getString(R.string.browse_musics_by_genre_subtitle, genre))
                                .build(), MediaBrowser.MediaItem.FLAG_BROWSABLE
                );
                mediaItems.add(item);
            }

        } else if (parentMediaId.startsWith(MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE)) {
            String genre = MediaIDHelper.getHierarchy(parentMediaId)[1];
            logger.Debug("OnLoadChildren.SONGS_BY_GENRE  genre=", genre);
            for (MediaMetadata track : mMusicProvider.getMusicsByGenre(genre)) {
                // Since mediaMetadata fields are immutable, we need to create a copy, so we
                // can set a hierarchy-aware mediaID. We will need to know the media hierarchy
                // when we get a onPlayFromMusicID call, so we can create the proper queue based
                // on where the music was selected from (by artist, by genre, random, etc)
                String hierarchyAwareMediaID = MediaIDHelper.createMediaID(
                        track.getDescription().getMediaId(), MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE, genre);
                MediaMetadata trackCopy = new MediaMetadata.Builder(track)
                        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                        .build();
                MediaBrowser.MediaItem bItem = new MediaBrowser.MediaItem(
                        trackCopy.getDescription(), MediaBrowser.MediaItem.FLAG_PLAYABLE);
                mediaItems.add(bItem);
            }
        } else {
            logger.Warn("Skipping unmatched parentMediaId: ", parentMediaId);
        }
        logger.Debug("OnLoadChildren sending ", mediaItems.size(),
                " results for ", parentMediaId);
        result.sendResult(mediaItems);
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

    @Override
    public Class<?> getServiceClass() {
        return MediaService.class;
    }

    private void handleNextTrackCommand() {
        MainActivity.RespondToWebView("MediaController.nextTrack();");
    }

    private void handlePreviousTrackCommand() {
        MainActivity.RespondToWebView("MediaController.previousTrack();");
    }

    private void handleIntent(Context context, Intent intent) {

        String action = intent.getAction();
        if (action == null){
            return;
        }

        logger.Debug("MediaService.handleIntent action=%s", action);

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
        else if (action.equalsIgnoreCase(Constants.ACTION_PREVIOUS)) {
            handlePreviousTrackCommand();
        } else if (action.equalsIgnoreCase(Constants.ACTION_NEXT)) {
            handleNextTrackCommand();
        }else if (action.equalsIgnoreCase(Constants.ACTION_SEEK)) {
            seek(intent.getLongExtra("position", 0));
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

    private void play(Context context, Intent intent) {

        String path = intent.getStringExtra("path");
        String itemJson = intent.getStringExtra("item");
        String mediaSourceJson = intent.getStringExtra("mediaSource");
        final String posterUrl = intent.getStringExtra("posterUrl");

        final BaseItemDto item = jsonSerializer.DeserializeFromString(itemJson, BaseItemDto.class);

        mDelayedStopHandler.removeCallbacksAndMessages(null);

        if (!mServiceStarted) {
            logger.Info("Starting service");
            // The MusicService needs to keep running even after the calling MediaBrowser
            // is disconnected. Call startService(Intent) and then stopSelf(..) when we no longer
            // need to play media.
            startService(new Intent(getApplicationContext(), MediaService.class));
            mServiceStarted = true;
        }

        if (!mSession.isActive()) {
            mSession.setActive(true);
        }

        //if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            updateMetadata();
            setMediaSessionInfo(item, posterUrl);
            LibVLC vlc = getLibVlcInstance();
            Media media = new Media(vlc, path);
            currentMrl = media.getMrl();

            logger.Debug("Vlc playMRL: %s", currentMrl);
            vlc.playMRL(currentMrl);
        //}
    }

    private void setMediaSessionInfo(BaseItemDto item, String posterUrl) {

        mSession.setActive(true);

        String album = item.getAlbum() == null ? "" : item.getAlbum();
        String artist = item.getArtistItems() != null && item.getArtistItems().size() > 0 ? item.getArtistItems().get(0).getName() : "";
        String title = item.getName() == null ? "" : item.getName();
        String itemId = item.getId();

        final MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, itemId);

        if (posterUrl != null && posterUrl.length() > 0) {
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, posterUrl);
        }

        mSession.setMetadata(metadataBuilder.build());

        if (posterUrl != null && posterUrl.length() > 0) {
            GetHttpClient().getBitmap(posterUrl, new Response<Bitmap>() {

                @Override
                public void onResponse(Bitmap bitmap) {
                    currentBitmap = bitmap;
                    metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, currentBitmap);
                    mSession.setMetadata(metadataBuilder.build());
                }
            });
        }
    }

    /**
     * Handle a request to play music
     */
    private void handlePlayRequest() {

        logger.Debug("handlePlayRequest: mState=" + mPlayback.getState());

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        if (!mServiceStarted) {
            logger.Debug("Starting service");
            // The MusicService needs to keep running even after the calling MediaBrowser
            // is disconnected. Call startService(Intent) and then stopSelf(..) when we no longer
            // need to play media.
            startService(new Intent(getApplicationContext(), MediaService.class));
            mServiceStarted = true;
        }

        if (!mSession.isActive()) {
            mSession.setActive(true);
        }

        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            updateMetadata();
            mPlayback.play(mPlayingQueue.get(mCurrentIndexOnQueue));
        }
    }

    /**
     * Handle a request to pause music
     */
    private void handlePauseRequest() {

        logger.Debug("handlePauseRequest: mState=" + mPlayback.getState());
        mPlayback.pause();
        // reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
    }

    /**
     * Handle a request to stop music
     */
    private void handleStopRequest(String withError) {

        logger.Debug("handleStopRequest: mState=" + mPlayback.getState() + " error=", withError);
        mPlayback.stop(true);
        // reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        updatePlaybackState(withError);

        // service is no longer necessary. Will be started again if needed.
        stopSelf();
        mServiceStarted = false;
    }

    private void updateMetadata() {
        /*if (!QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            logger.Error(TAG, "Can't retrieve current metadata.");
            updatePlaybackState(getResources().getString(R.string.error_no_metadata));
            return;
        }
        MediaSession.QueueItem queueItem = mPlayingQueue.get(mCurrentIndexOnQueue);
        String musicId = MediaIDHelper.extractMusicIDFromMediaID(
                queueItem.getDescription().getMediaId());
        MediaMetadata track = mMusicProvider.getMusic(musicId);
        final String trackId = track.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
        if (!musicId.equals(trackId)) {
            IllegalStateException e = new IllegalStateException("track ID should match musicId.");
            logger.Error(TAG, "track ID should match musicId.",
                    " musicId=", musicId, " trackId=", trackId,
                    " mediaId from queueItem=", queueItem.getDescription().getMediaId(),
                    " title from queueItem=", queueItem.getDescription().getTitle(),
                    " mediaId from track=", track.getDescription().getMediaId(),
                    " title from track=", track.getDescription().getTitle(),
                    " source.hashcode from track=", track.getString(
                            MusicProvider.CUSTOM_METADATA_TRACK_SOURCE).hashCode(),
                    e);
            throw e;
        }
        logger.Debug(TAG, "Updating metadata for MusicID= " + musicId);
        mSession.setMetadata(track);

        // Set the proper album artwork on the media session, so it can be shown in the
        // locked screen and in other places.
        if (track.getDescription().getIconBitmap() == null &&
                track.getDescription().getIconUri() != null) {
            String albumUri = track.getDescription().getIconUri().toString();
            AlbumArtCache.getInstance().fetch(albumUri, new AlbumArtCache.FetchListener() {
                @Override
                public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                    MediaSession.QueueItem queueItem = mPlayingQueue.get(mCurrentIndexOnQueue);
                    MediaMetadata track = mMusicProvider.getMusic(trackId);
                    track = new MediaMetadata.Builder(track)

                            // set high resolution bitmap in METADATA_KEY_ALBUM_ART. This is used, for
                            // example, on the lockscreen background when the media session is active.
                            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)

                                    // set small version of the album art in the DISPLAY_ICON. This is used on
                                    // the MediaDescription and thus it should be small to be serialized if
                                    // necessary..
                            .putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, icon)

                            .build();

                    mMusicProvider.updateMusic(trackId, track);

                    // If we are still playing the same music
                    String currentPlayingId = MediaIDHelper.extractMusicIDFromMediaID(
                            queueItem.getDescription().getMediaId());
                    if (trackId.equals(currentPlayingId)) {
                        mSession.setMetadata(track);
                    }
                }
            });
        }*/
    }

    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    private void updatePlaybackState(String error) {

        int state = mPlayback.getState();

        logger.Debug(TAG, "updatePlaybackState, playback state=" + state);
        long position = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
        if (mPlayback != null && mPlayback.isConnected()) {
            position = mPlayback.getCurrentStreamPosition();
        }

        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(getAvailableActions());

        setCustomAction(stateBuilder);
        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackState.STATE_ERROR;
        }
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            MediaSession.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
            stateBuilder.setActiveQueueItemId(item.getQueueId());
        }

        mSession.setPlaybackState(stateBuilder.build());

        if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_PAUSED) {
            mMediaNotificationManager.startNotification();
        }
    }

    private int getState() {

        if (mLibVLC == null){
            return PlaybackState.STATE_NONE;
        }

        int state = mLibVLC.getPlayerState();

        // Expected states by web plugins are: IDLE/CLOSE=0, OPENING=1, BUFFERING=2, PLAYING=3, PAUSED=4, STOPPING=5, ENDED=6, ERROR=7

        switch (state) {

            case 0:
                return PlaybackState.STATE_NONE;
            case 1:
                return PlaybackState.STATE_CONNECTING;
            case 2:
                return PlaybackState.STATE_BUFFERING;
            case 3:
                return PlaybackState.STATE_PLAYING;
            case 4:
                return PlaybackState.STATE_PAUSED;
            case 5:
                return PlaybackState.STATE_STOPPED;
            case 6:
                return PlaybackState.STATE_STOPPED;
            case 7:
                return PlaybackState.STATE_ERROR;
            default:
                return PlaybackState.STATE_NONE;
        }
    }

    private long getCurrentStreamPosition() {

        if (mLibVLC != null){
            return mLibVLC.getTime();
        }

        return 0;
    }

    /**
     * Handle libvlc asynchronous events
     */
    private  Handler mVlcEventHandler;

    private static class VlcServiceEventHandler extends VlcEventHandler {

        private MediaService audioService;
        private long lastReportTime;

        public VlcServiceEventHandler(ILogger logger, LibVLC mLibVLC, MediaService audioService) {
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
                    break;
                case EventHandler.MediaPlayerPlaying:

                    service.changeAudioFocus(true);
                    if (!service.mWakeLock.isHeld())
                        service.mWakeLock.acquire();
                    service.updatePlaybackState(null);
                    break;
                case EventHandler.MediaPlayerPaused:
                    if (service.mWakeLock.isHeld())
                        service.mWakeLock.release();
                    service.updatePlaybackState(null);
                    break;
                case EventHandler.MediaPlayerStopped:
                    service.updatePlaybackState(null);
                    break;
                case EventHandler.MediaPlayerEndReached:
                    service.updatePlaybackState(null);
                    break;
                case EventHandler.MediaPlayerVout:
                    if(msg.getData().getInt("data") > 0) {
                        service.handleVout();
                    }
                    break;
                case EventHandler.MediaPlayerPositionChanged:
                    break;
                case EventHandler.MediaPlayerEncounteredError:
                    /*service.showToast(service.getString(
                            R.string.invalid_location,
                            service.mLibVLC.getMediaList().getMRL(
                                    service.mCurrentIndex)), Toast.LENGTH_SHORT);*/
                    service.updatePlaybackState(null);
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
                    service.updatePlaybackState(null);
                    break;
                default:
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
        //hideNotification(false);

        // Switch to the video player & don't lose the currently playing stream
        //VideoPlayerActivity.start(VLCApplication.getAppContext(), MRL, title, index, true);
    }

    private String currentMrl;
    private Bitmap currentBitmap = null;

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
            if (vlc.getPlayerState() != 4){
                vlc.pause();
            }
        }

        // reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
    }

    private void play() {
        if(hasCurrentMedia()) {
            mLibVLC.play();
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

        // reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        updatePlaybackState(null);

        destroyCurrentMediaInfo();
        changeAudioFocus(false);

        if (mWakeLock.isHeld())
            mWakeLock.release();

        if (stopService) {
            // service is no longer necessary. Will be started again if needed.
            logger.Debug("MediaService.stopSelf");
            stopSelf();

            mServiceStarted = false;
        }
    }

    private void destroyCurrentMediaInfo() {

        logger.Debug("MediaService.destroyCurrentMediaInfo");

        currentMrl = null;
        currentBitmap = null;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private final class MediaSessionCallback extends MediaSession.Callback {

        private ILogger logger;

        private MediaSessionCallback(ILogger logger) {
            this.logger = logger;
        }

        @Override
        public void onPlay() {
            play();
        }

        @Override
        public void onSeekTo(long position) {
            seek(position);
        }

        @Override
        public void onPause() {
            pause();
        }

        @Override
        public void onStop() {

            logger.Debug("MediaSessionCallback.onStop");

            stop(true);
        }

        @Override
        public void onSkipToNext() {
            handleNextTrackCommand();
        }

        @Override
        public void onSkipToPrevious() {
            handlePreviousTrackCommand();
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {

            if (Constants.THUMBS_UP.equals(action)) {
                logger.Info("onCustomAction: favorite for current track");
                MediaMetadata track = getCurrentPlayingMusic();
                if (track != null) {
                    String musicId = track.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
                    mMusicProvider.setFavorite(musicId, !mMusicProvider.isFavorite(musicId));
                }
                // playback state needs to be updated because the "Favorite" icon on the
                // custom action will change to reflect the new favorite state.
                updatePlaybackState(null);
            } else {
                logger.Error("Unsupported action: %", action);
            }
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {

            logger.Debug("playFromSearch  query=", query);

            if (TextUtils.isEmpty(query)) {
                // A generic search like "Play music" sends an empty query
                // and it's expected that we start playing something. What will be played depends
                // on the app: favorite playlist, "I'm feeling lucky", most recent, etc.
                mPlayingQueue = QueueHelper.getRandomQueue(mMusicProvider);
            } else {
                mPlayingQueue = QueueHelper.getPlayingQueueFromSearch(query, mMusicProvider);
            }

            logger.Debug("playFromSearch  playqueue.length=" + mPlayingQueue.size());
            mSession.setQueue(mPlayingQueue);

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                // immediately start playing from the beginning of the search results
                mCurrentIndexOnQueue = 0;

                handlePlayRequest();
            } else {
                // if nothing was found, we need to warn the user and stop playing
                handleStopRequest(getString(R.string.no_search_results));
            }
        }
    }

    private void setCustomAction(PlaybackState.Builder stateBuilder) {
        MediaMetadata currentMusic = getCurrentPlayingMusic();
        if (currentMusic != null) {
            // Set appropriate "Favorite" icon on Custom action:
            String musicId = currentMusic.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
            int favoriteIcon = R.drawable.ic_star_off;
            if (mMusicProvider.isFavorite(musicId)) {
                favoriteIcon = R.drawable.ic_star_on;
            }
            logger.Debug("updatePlaybackState, setting Favorite custom action of music ",
                    musicId, " current favorite=", mMusicProvider.isFavorite(musicId));
            stateBuilder.addCustomAction(Constants.THUMBS_UP, getString(R.string.favorite),
                    favoriteIcon);
        }
    }

    private long getAvailableActions() {
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackState.ACTION_PLAY_FROM_SEARCH;
        if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
            return actions;
        }
        if (mPlayback.isPlaying()) {
            actions |= PlaybackState.ACTION_PAUSE;
        }
        if (mCurrentIndexOnQueue > 0) {
            actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        }
        if (mCurrentIndexOnQueue < mPlayingQueue.size() - 1) {
            actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
        }
        return actions;
    }

    private MediaMetadata getCurrentPlayingMusic() {

        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            MediaSession.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
            if (item != null) {
                logger.Debug("getCurrentPlayingMusic for musicId=",
                        item.getDescription().getMediaId());
                return mMusicProvider.getMusic(
                        MediaIDHelper.extractMusicIDFromMediaID(item.getDescription().getMediaId()));
            }
        }
        return null;
    }

    /**
     * Implementation of the Playback.Callback interface
     */
    @Override
    public void onCompletion() {
        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
            // In this sample, we restart the playing queue when it gets to the end:
            mCurrentIndexOnQueue++;
            if (mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                mCurrentIndexOnQueue = 0;
            }
            handlePlayRequest();
        } else {
            // If there is nothing to play, we stop and release the resources:
            handleStopRequest(null);
        }
    }

    @Override
    public void onPlaybackStatusChanged(int state) {
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error) {
        updatePlaybackState(error);
    }

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private static class DelayedStopHandler extends Handler {
        private final WeakReference<MediaService> mWeakReference;

        private DelayedStopHandler(MediaService service) {
            mWeakReference = new WeakReference<MediaService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaService service = mWeakReference.get();
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