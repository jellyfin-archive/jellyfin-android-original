package com.mb.android.media;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSession.Callback;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.mb.android.MainActivity;
import com.mb.android.R;
import com.mb.android.api.ApiClientBridge;
import com.mb.android.logging.AppLogger;

import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.android.mediabrowser.Constants;
import mediabrowser.model.logging.ILogger;

public class RemotePlayerService extends Service {

    private static final String TAG = "Emby/AudioService";

    private MediaSessionManager m_objMediaSessionManager;
    private MediaSession m_objMediaSession;
    private MediaController m_objMediaController;

    private String mediaSessionMediaId = "";
    private Bitmap largeItemIcon;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null)
            return;

        String action = intent.getAction();

        if (action.equalsIgnoreCase(Constants.ACTION_REPORT)) {
            notify(intent);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (action.equalsIgnoreCase(Constants.ACTION_PLAY)) {
                m_objMediaController.getTransportControls().play();
            } else if (action.equalsIgnoreCase(Constants.ACTION_PAUSE)) {
                m_objMediaController.getTransportControls().pause();
            } else if (action.equalsIgnoreCase(Constants.ACTION_FAST_FORWARD)) {
                m_objMediaController.getTransportControls().fastForward();
            } else if (action.equalsIgnoreCase(Constants.ACTION_REWIND)) {
                m_objMediaController.getTransportControls().rewind();
            } else if (action.equalsIgnoreCase(Constants.ACTION_PREVIOUS)) {
                m_objMediaController.getTransportControls().skipToPrevious();
            } else if (action.equalsIgnoreCase(Constants.ACTION_NEXT)) {
                m_objMediaController.getTransportControls().skipToNext();
            } else if (action.equalsIgnoreCase(Constants.ACTION_STOP)) {
                m_objMediaController.getTransportControls().stop();
            }
        }
    }

    private VolleyHttpClient httpClient;

    private void notify(final Intent handledIntent) {

        String playerAction = handledIntent.getStringExtra("playerAction");

        if (playerAction.equalsIgnoreCase("playbackstop")) {
            onStopped();
            return;
        }

        String itemId = handledIntent.getStringExtra("itemId");
        String imageUrl = handledIntent.getStringExtra("imageUrl");

        if (largeItemIcon != null && mediaSessionMediaId.equalsIgnoreCase(itemId)) {
            notifyWithBitmap(handledIntent, largeItemIcon);
            return;
        }

        if (imageUrl != null && imageUrl.length() > 0) {

            if (ApiClientBridge.Current != null) {
                httpClient = ApiClientBridge.Current.httpClient;
            }

            ILogger logger = AppLogger.getLogger(getApplicationContext());

            if (httpClient == null) {
                httpClient = new VolleyHttpClient(logger, getApplicationContext());
            }

            httpClient.getBitmap(imageUrl, new Response<Bitmap>() {

                @Override
                public void onResponse(Bitmap bitmap) {
                    largeItemIcon = bitmap;
                    notifyWithBitmap(handledIntent, bitmap);
                }

                @Override
                public void onError(Exception ex) {
                    notifyWithBitmap(handledIntent, null);
                }

            });
        } else {
            notifyWithBitmap(handledIntent, null);
        }
    }

    private void notifyWithBitmap(Intent handledIntent, Bitmap largeIcon) {

        String artist = handledIntent.getStringExtra("artist");
        String album = handledIntent.getStringExtra("artist");
        String title = handledIntent.getStringExtra("title");
        String itemId = handledIntent.getStringExtra("itemId");
        boolean isPaused = handledIntent.getBooleanExtra("isPaused", false);
        boolean canSeek = handledIntent.getBooleanExtra("canSeek", false);
        boolean isLocalPlayer = handledIntent.getBooleanExtra("isLocalPlayer", false);

        if (!mediaSessionMediaId.equalsIgnoreCase(itemId)) {
            setMediaSessionMetadata(m_objMediaSession, itemId, artist, album, title, largeIcon);

            mediaSessionMediaId = itemId;
        }

        int position = handledIntent.getIntExtra("position", 0);
        int duration = handledIntent.getIntExtra("duration", 0);

         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            Notification.Action action = isPaused ?
                         generateAction(android.R.drawable.ic_media_play, "Play", Constants.ACTION_PLAY) :
                         generateAction(android.R.drawable.ic_media_pause, "Pause", Constants.ACTION_PAUSE);

            Notification.MediaStyle style = new Notification.MediaStyle();
            style.setMediaSession(m_objMediaSession.getSessionToken());

            Intent intent = new Intent(getApplicationContext(), RemotePlayerService.class);
            intent.setAction(Constants.ACTION_STOP);

            PlaybackState.Builder stateBuilder = new PlaybackState.Builder();
            stateBuilder.setActiveQueueItemId(MediaSession.QueueItem.UNKNOWN_ID);
            long actions = PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_STOP | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SEEK_TO | PlaybackState.ACTION_SET_RATING;
            stateBuilder.setActions(actions);

            if (isPaused) {
                stateBuilder.setState(PlaybackState.STATE_PAUSED, position, 1.0f);
            } else {
                stateBuilder.setState(PlaybackState.STATE_PLAYING, position, 1.0f);
            }

            m_objMediaSession.setPlaybackState(stateBuilder.build());

            PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
            Notification.Builder builder = new Notification.Builder(this)
                    .setContentTitle(title)
                    .setContentText(artist)
                    .setDeleteIntent(pendingIntent)
                    .setContentIntent(createContentIntent())
                    .setProgress(duration, position, duration == 0)
                    .setStyle(style);

            builder.setOngoing(true);
            builder.setShowWhen(true);
            builder.setUsesChronometer(true);
            builder.setWhen(System.currentTimeMillis() - position);
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);

            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon);
                builder.setSmallIcon(R.drawable.icon);
            } else {
                builder.setSmallIcon(R.drawable.icon);
            }

            builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", Constants.ACTION_PREVIOUS));
            builder.addAction( generateAction( android.R.drawable.ic_media_rew, "Rewind", Constants.ACTION_REWIND ) );
            builder.addAction(action);
            builder.addAction( generateAction( android.R.drawable.ic_media_ff, "Fast Foward", Constants.ACTION_FAST_FORWARD ) );
            builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", Constants.ACTION_NEXT));

            //final TransportControls controls = m_objMediaSession.getController().getTransportControls();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

             try {
                 notificationManager.notify(1, builder.build());
             }
             catch (Exception ex){
                // this will throw if the bitmap fails to load
             }
        }
    }

    private PendingIntent createContentIntent() {

        Intent openUI = new Intent(this, MainActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openUI.setAction(Constants.ACTION_SHOW_PLAYER);
        return PendingIntent.getActivity(this, 100, openUI, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void setMediaSessionMetadata(MediaSession mediaSession, String itemId, String artist, String album, String title, Bitmap largeIcon) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                    .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, itemId);

            if (largeIcon != null) {
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, largeIcon);
            }

            mediaSession.setMetadata(metadataBuilder.build());
        }
    }

    private void cleanupAfterMediaStop() {

    }

    private void onStopped() {

        cleanupAfterMediaStop();

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
        Intent intent = new Intent(getApplicationContext(), RemotePlayerService.class);
        stopService(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        cleanupAfterMediaStop();
    }

    private Notification.Action generateAction(int icon, String title, String intentAction) {

        Intent intent = new Intent(getApplicationContext(), RemotePlayerService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);

        return new Notification.Action(icon, title, pendingIntent);
    }

    private PendingIntent retreivePlaybackAction(int which) {
        Intent action;
        PendingIntent pendingIntent;
        final ComponentName serviceName = new ComponentName(this, RemotePlayerService.class);
        switch (which) {
            case 1:
                // Play and pause
                action = new Intent(Constants.ACTION_PLAY);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(this, 1, action, 0);
                return pendingIntent;
            case 2:
                // Skip tracks
                action = new Intent(Constants.ACTION_NEXT);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(this, 2, action, 0);
                return pendingIntent;
            case 3:
                // Previous tracks
                action = new Intent(Constants.ACTION_PREVIOUS);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(this, 3, action, 0);
                return pendingIntent;
            case 4:
                //fast forward tracks
                action = new Intent(Constants.ACTION_FAST_FORWARD);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(this, 4, action, 0);
                return pendingIntent;
            case 5:
                //rewind tracks
                action = new Intent(Constants.ACTION_REWIND);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(this, 5, action, 0);
                return pendingIntent;
            default:
                break;
        }
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        if (m_objMediaSessionManager == null) {
            initMediaSessions();
        }

        handleIntent(intent);

        return super.onStartCommand(intent, flags, startId);
    }

    private void responseToWebView(String js) {
        MainActivity.RespondToWebView(js);
    }

    private void initMediaSessions() {

        mediaSessionMediaId = "";
        largeItemIcon = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            m_objMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            m_objMediaSession = new MediaSession(getApplicationContext(), "sample session");
            m_objMediaController = new MediaController(getApplicationContext(), m_objMediaSession.getSessionToken());
            m_objMediaSession.setActive(true);
            m_objMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);

            m_objMediaSession.setCallback(new Callback() {
                @Override
                public void onPlay() {
                    super.onPlay();
                    Log.e(Constants.LOG_TAG, "onPlay");
                    //buildNotification( generateAction( android.R.drawable.ic_media_pause, "Pause", Constants.ACTION_PAUSE ) );
                    MainActivity.sendCommand("playpause");
                }

                @Override
                public void onPause() {
                    super.onPause();
                    Log.e(Constants.LOG_TAG, "onPause");
                    //buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", Constants.ACTION_PLAY));
                    MainActivity.sendCommand("playpause");
                }

                @Override
                public void onSkipToNext() {
                    super.onSkipToNext();
                    Log.e(Constants.LOG_TAG, "onSkipToNext");
                    //buildNotification( generateAction( android.R.drawable.ic_media_pause, "Pause", Constants.ACTION_PAUSE ) );
                    MainActivity.sendCommand("next");
                }

                @Override
                public void onSkipToPrevious() {
                    super.onSkipToPrevious();
                    Log.e(Constants.LOG_TAG, "onSkipToPrevious");
                    //buildNotification( generateAction( android.R.drawable.ic_media_pause, "Pause", Constants.ACTION_PAUSE ) );
                    MainActivity.sendCommand("previous");
                }

                @Override
                public void onFastForward() {
                    super.onFastForward();
                    MainActivity.sendCommand("fastforward");
                }

                @Override
                public void onRewind() {
                    super.onRewind();
                    MainActivity.sendCommand("rewind");
                }

                @Override
                public void onStop() {
                    super.onStop();
                    MainActivity.sendCommand("stop");
                    onStopped();
                }

                @Override
                public void onSeekTo(long pos) {

                    super.onSeekTo(pos);
                }

                @Override
                public void onSetRating(Rating rating) {
                    super.onSetRating(rating);
                }
            });
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            m_objMediaSession.release();
        }

        return super.onUnbind(intent);
    }
}