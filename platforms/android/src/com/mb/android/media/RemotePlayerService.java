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

import com.mb.android.api.ApiClientBridge;
import com.mb.android.logging.AppLogger;

import org.jellyfin.mobile.MainActivity;
import org.jellyfin.mobile.R;

import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.android.mediabrowser.Constants;
import mediabrowser.model.logging.ILogger;

public class RemotePlayerService extends Service {

    private VolleyHttpClient httpClient;
    private MediaController mediaController;
    private MediaSessionManager mediaSessionManager;
    private MediaSession mediaSession;
    private String mediaSessionMediaId = "";
    private Bitmap largeItemIcon;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession.release();
            onStopped();
        }

        return super.onUnbind(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();

        if (action.equals(Constants.ACTION_REPORT)) {
            notify(intent);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switch (action) {
                case Constants.ACTION_PLAY:
                    mediaController.getTransportControls().play();
                    break;
                case Constants.ACTION_PAUSE:
                    mediaController.getTransportControls().pause();
                    break;
                case Constants.ACTION_FAST_FORWARD:
                    mediaController.getTransportControls().fastForward();
                    break;
                case Constants.ACTION_REWIND:
                    mediaController.getTransportControls().rewind();
                    break;
                case Constants.ACTION_PREVIOUS:
                    mediaController.getTransportControls().skipToPrevious();
                    break;
                case Constants.ACTION_NEXT:
                    mediaController.getTransportControls().skipToNext();
                    break;
                case Constants.ACTION_STOP:
                    mediaController.getTransportControls().stop();
                    break;
            }
        }
    }

    private void notify(final Intent handledIntent) {
        String playerAction = handledIntent.getStringExtra("playerAction");
        if (playerAction.equals("playbackstop")) {
            onStopped();
            return;
        }

        String itemId = handledIntent.getStringExtra("itemId");
        String imageUrl = handledIntent.getStringExtra("imageUrl");
        if (largeItemIcon != null && mediaSessionMediaId.equals(itemId)) {
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

        if (!mediaSessionMediaId.equals(itemId)) {
            setMediaSessionMetadata(mediaSession, itemId, artist, album, title, largeIcon);
            mediaSessionMediaId = itemId;
        }

        int position = handledIntent.getIntExtra("position", 0);
        int duration = handledIntent.getIntExtra("duration", 0);

         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Notification.Action action = isPaused ?
                         generateAction(android.R.drawable.ic_media_play, "Play", Constants.ACTION_PLAY) :
                         generateAction(android.R.drawable.ic_media_pause, "Pause", Constants.ACTION_PAUSE);

            Notification.MediaStyle style = new Notification.MediaStyle();
            style.setMediaSession(mediaSession.getSessionToken());

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

            mediaSession.setPlaybackState(stateBuilder.build());

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
                builder.setSmallIcon(R.drawable.ic_notification);
            } else {
                builder.setSmallIcon(R.drawable.ic_notification);
            }

            builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", Constants.ACTION_PREVIOUS));
            builder.addAction(generateAction( android.R.drawable.ic_media_rew, "Rewind", Constants.ACTION_REWIND));
            builder.addAction(action);
            builder.addAction(generateAction( android.R.drawable.ic_media_ff, "Fast Forward", Constants.ACTION_FAST_FORWARD));
            builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", Constants.ACTION_NEXT));

            try {
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(1, builder.build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private PendingIntent createContentIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setAction(Constants.ACTION_SHOW_PLAYER);
        return PendingIntent.getActivity(this, 100, intent, PendingIntent.FLAG_CANCEL_CURRENT);
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
        if (mediaSessionManager == null) {
            initMediaSessions();
        }

        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void initMediaSessions() {
        mediaSessionMediaId = "";
        largeItemIcon = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            mediaSession = new MediaSession(getApplicationContext(), "sample session");
            mediaController = new MediaController(getApplicationContext(), mediaSession.getSessionToken());
            mediaSession.setActive(true);
            int flags = MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS;
            mediaSession.setFlags(flags);

            mediaSession.setCallback(new Callback() {
                @Override
                public void onPlay() {
                    super.onPlay();
                    Log.i(Constants.LOG_TAG, "onPlay");
                    MainActivity.sendCommand("playpause");
                }

                @Override
                public void onPause() {
                    super.onPause();
                    Log.i(Constants.LOG_TAG, "onPause");
                    MainActivity.sendCommand("playpause");
                }

                @Override
                public void onSkipToNext() {
                    super.onSkipToNext();
                    Log.i(Constants.LOG_TAG, "onSkipToNext");
                    MainActivity.sendCommand("next");
                }

                @Override
                public void onSkipToPrevious() {
                    super.onSkipToPrevious();
                    Log.i(Constants.LOG_TAG, "onSkipToPrevious");
                    MainActivity.sendCommand("previous");
                }

                @Override
                public void onFastForward() {
                    super.onFastForward();
                    Log.i(Constants.LOG_TAG, "onFastForward");
                    MainActivity.sendCommand("fastforward");
                }

                @Override
                public void onRewind() {
                    super.onRewind();
                    Log.i(Constants.LOG_TAG, "onRewind");
                    MainActivity.sendCommand("rewind");
                }

                @Override
                public void onStop() {
                    super.onStop();
                    Log.i(Constants.LOG_TAG, "onStop");
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
}