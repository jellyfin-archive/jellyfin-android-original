package org.jellyfin.mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
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
import android.os.Bundle;
import android.os.IBinder;

import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.interaction.VolleyHttpClient;
import org.jellyfin.apiclient.logging.AndroidLogger;
import org.jellyfin.apiclient.model.logging.ILogger;

public class RemotePlayerService extends Service {
    private VolleyHttpClient httpClient;
    private MediaController mediaController;
    private MediaSessionManager mediaSessionManager;
    private MediaSession mediaSession;
    private Bitmap largeItemIcon;

    private String mediaSessionId;
    private String channelId;
    private int notifyId = 84;

    // only trip this flag if the user switches from headphones to speaker
    // prevent stopping music when inserting headphones for the first time
    public static boolean headphoneFlag = false;
    public static BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", 2);
                if (state == 0) {
                    sendCommand("playpause");
                    headphoneFlag = true;
                } else if (headphoneFlag) {
                    sendCommand("playpause");
                }
            } else if (intent.getAction().equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }
                int state = extras.getInt(BluetoothA2dp.EXTRA_STATE);
                int previousState = extras.getInt(BluetoothA2dp.EXTRA_PREVIOUS_STATE);
                if ((state == BluetoothA2dp.STATE_DISCONNECTED || state == BluetoothA2dp.STATE_DISCONNECTING) && previousState == BluetoothA2dp.STATE_CONNECTED) {
                    sendCommand("pause");
                }
            } else if (intent.getAction().equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }
                int state = extras.getInt(BluetoothHeadset.EXTRA_STATE);
                int previousState = extras.getInt(BluetoothHeadset.EXTRA_PREVIOUS_STATE);
                if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED && previousState == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                    sendCommand("pause");
                }
            }
        }
    };

    private static void sendCommand(String action) {
        String url = "javascript:require(['inputManager'], function(inputManager){inputManager.trigger('" + action + "'" + ");});";
        loadUrl(url);
    }

    private static void sendSeekCommand(long pos) {
        String url = "javascript:require(['inputManager'], function(inputManager){inputManager.trigger('seek', " + pos + ");});";
        loadUrl(url);
    }

    private static void loadUrl(String url) {
        NativeShell.cordovaWebView.loadUrlIntoView(url, false);
    }

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            channelId = "JellyfinChannelId";
            String name = "JellyfinChannelName";
            String description = "JellyfinChannelDescription";
            int importance = NotificationManager.IMPORTANCE_LOW;

            NotificationChannel notificationChannel = new NotificationChannel(channelId, name, importance);
            notificationChannel.setDescription(description);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        onStopped();
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (mediaSessionManager == null) {
            initMediaSessions();
        }

        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void startWakelock() {
        if (NativeShell.wakeLock != null && !NativeShell.wakeLock.isHeld()) {
            NativeShell.wakeLock.acquire();
        }
    }

    private void stopWakelock() {
        if (NativeShell.wakeLock != null && NativeShell.wakeLock.isHeld()) {
            NativeShell.wakeLock.release();
        }
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
                    startWakelock();
                    break;
                case Constants.ACTION_PAUSE:
                    mediaController.getTransportControls().pause();
                    stopWakelock();
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
        if (largeItemIcon != null && mediaSessionId.equals(itemId)) {
            notifyWithBitmap(handledIntent, largeItemIcon);
            return;
        }

        if (imageUrl != null && imageUrl.length() > 0) {
            ILogger logger = new AndroidLogger("Jellyfin-RemotePlayerService");
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
        String album = handledIntent.getStringExtra("album");
        String title = handledIntent.getStringExtra("title");
        String itemId = handledIntent.getStringExtra("itemId");
        boolean isPaused = handledIntent.getBooleanExtra("isPaused", false);
        boolean canSeek = handledIntent.getBooleanExtra("canSeek", false);
        boolean isLocalPlayer = handledIntent.getBooleanExtra("isLocalPlayer", false);
        int position = handledIntent.getIntExtra("position", 0);
        int duration = handledIntent.getIntExtra("duration", 0);

        // system will recognize notification as media playback
        // show cover art and controls on lock screen
        if (mediaSessionId == null || !mediaSessionId.equals(itemId)) {
            setMediaSessionMetadata(mediaSession, itemId, artist, album, title, duration, largeIcon);
            mediaSessionId = itemId;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Notification.Action action = isPaused ?
                    generateAction(android.R.drawable.ic_media_play, "Play", Constants.ACTION_PLAY) :
                    generateAction(android.R.drawable.ic_media_pause, "Pause", Constants.ACTION_PAUSE);

            Notification.MediaStyle style = new Notification.MediaStyle();
            style.setMediaSession(mediaSession.getSessionToken());
            style.setShowActionsInCompactView(0, 2, 4);

            PlaybackState.Builder stateBuilder = new PlaybackState.Builder();
            stateBuilder.setActiveQueueItemId(MediaSession.QueueItem.UNKNOWN_ID);
            long actions = PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_STOP | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SEEK_TO | PlaybackState.ACTION_SET_RATING | PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE;
            stateBuilder.setActions(actions);

            if (isPaused) {
                stateBuilder.setState(PlaybackState.STATE_PAUSED, position, 1.0f);
            } else {
                stateBuilder.setState(PlaybackState.STATE_PLAYING, position, 1.0f);
            }

            mediaSession.setPlaybackState(stateBuilder.build());
            Notification.Builder builder = new Notification.Builder(this)
                    .setContentTitle(title)
                    .setContentText(artist)
                    .setSubText(album)
                    .setPriority(Notification.PRIORITY_LOW)
                    .setDeleteIntent(createDeleteIntent())
                    .setContentIntent(createContentIntent())
                    .setProgress(duration, position, duration == 0)
                    .setStyle(style);

            // newer versions of android require notification channel to display
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(channelId);
                // color notification based on cover art
                builder.setColorized(true);
            }

            // swipe to dismiss if paused
            builder.setOngoing(!isPaused);

            // show current position in "when" field pre-O
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                builder.setShowWhen(!isPaused);
                builder.setUsesChronometer(!isPaused);
                builder.setWhen(System.currentTimeMillis() - position);
            }
            // privacy value for lock screen
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);

            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon);
                builder.setSmallIcon(R.drawable.ic_notification);
            } else {
                builder.setSmallIcon(R.drawable.ic_notification);
            }

            builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", Constants.ACTION_PREVIOUS));
            builder.addAction(generateAction(android.R.drawable.ic_media_rew, "Rewind", Constants.ACTION_REWIND));
            builder.addAction(action);
            builder.addAction(generateAction(android.R.drawable.ic_media_ff, "Fast Forward", Constants.ACTION_FAST_FORWARD));
            builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", Constants.ACTION_NEXT));

            try {
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(notifyId, builder.build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private PendingIntent createDeleteIntent() {
        Intent intent = new Intent(getApplicationContext(), RemotePlayerService.class);
        intent.setAction(Constants.ACTION_STOP);
        return PendingIntent.getService(getApplicationContext(), 1, intent, 0);
    }

    private PendingIntent createContentIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setAction(Constants.ACTION_SHOW_PLAYER);
        return PendingIntent.getActivity(this, 100, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private Notification.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), RemotePlayerService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), notifyId, intent, 0);
        return new Notification.Action(icon, title, pendingIntent);
    }

    private void setMediaSessionMetadata(MediaSession mediaSession, String itemId, String artist, String album, String title, int duration, Bitmap largeIcon) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                    .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
                    .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, itemId);

            if (largeIcon != null) {
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, largeIcon);
            }
            mediaSession.setMetadata(metadataBuilder.build());
        }
    }

    private void onStopped() {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notifyId);
        Intent intent = new Intent(getApplicationContext(), RemotePlayerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession.release();
        }
        headphoneFlag = false;
        stopWakelock();
        stopService(intent);
    }

    private void initMediaSessions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            mediaSession = new MediaSession(getApplicationContext(), getClass().toString());
            mediaController = new MediaController(getApplicationContext(), mediaSession.getSessionToken());

            mediaSession.setActive(true);
            mediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
            mediaSession.setCallback(new Callback() {
                @Override
                public void onPlay() {
                    sendCommand("playpause");
                }

                @Override
                public void onPause() {
                    sendCommand("playpause");
                }

                @Override
                public void onSkipToNext() {
                    sendCommand("next");
                }

                @Override
                public void onSkipToPrevious() {
                    sendCommand("previous");
                }

                @Override
                public void onFastForward() {
                    sendCommand("fastforward");
                }

                @Override
                public void onRewind() {
                    sendCommand("rewind");
                }

                @Override
                public void onStop() {
                    sendCommand("stop");
                    onStopped();
                }

                @Override
                public void onSeekTo(long pos) {
                    sendSeekCommand(pos);
                }

                @Override
                public void onSetRating(Rating rating) {
                }
            });
        }
    }
}
