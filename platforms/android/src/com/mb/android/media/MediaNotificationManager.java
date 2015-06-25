package com.mb.android.media;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;

import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import com.mb.android.R;

/**
 * Created by Luke on 6/22/2015.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MediaNotificationManager extends BroadcastReceiver {

    private static final String TAG = "NotificationManager";

    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    public static final String ACTION_PAUSE = "com.example.android.mediabrowserservice.pause";
    public static final String ACTION_PLAY = "com.example.android.mediabrowserservice.play";
    public static final String ACTION_PREV = "com.example.android.mediabrowserservice.prev";
    public static final String ACTION_NEXT = "com.example.android.mediabrowserservice.next";

    private final Service mService;
    private final IMediaService mediaService;

    private MediaSession.Token mSessionToken;
    private MediaController mController;
    private MediaController.TransportControls mTransportControls;

    private PlaybackState mPlaybackState;
    private MediaMetadata mMetadata;

    private NotificationManager mNotificationManager;

    private PendingIntent mPauseIntent;
    private PendingIntent mPlayIntent;
    private PendingIntent mPreviousIntent;
    private PendingIntent mNextIntent;

    private boolean mStarted = false;
    private VolleyHttpClient httpClient;

    public MediaNotificationManager(Service service, IMediaService mediaService, VolleyHttpClient httpClient) {
        mService = service;
        this.mediaService = mediaService;
        this.httpClient = httpClient;

        updateSessionToken();

        mNotificationManager = (NotificationManager) mService
                .getSystemService(Context.NOTIFICATION_SERVICE);

        String pkg = mService.getPackageName();
        mPauseIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPlayIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPreviousIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mNextIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager.cancelAll();
    }

    private Bitmap currentBitmap = null;

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification(Bitmap bitmap) {

        currentBitmap = bitmap;

        if (!mStarted) {
            mMetadata = mController.getMetadata();
            mPlaybackState = mController.getPlaybackState();

            // The notification must be updated after setting started to true
            Notification notification = createNotification();
            if (notification != null) {
                mController.registerCallback(mCb);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_NEXT);
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_PREV);
                mService.registerReceiver(this, filter);

                mService.startForeground(NOTIFICATION_ID, notification);
                mStarted = true;
            }
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification() {
        if (mStarted) {
            mStarted = false;
            mController.unregisterCallback(mCb);
            try {
                mNotificationManager.cancel(NOTIFICATION_ID);
                mService.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // ignore if the receiver is not registered.
            }
            mService.stopForeground(true);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();

        if (action.equalsIgnoreCase(ACTION_PAUSE)){
            mTransportControls.pause();
        }
        else if (action.equalsIgnoreCase(ACTION_PLAY)){
            mTransportControls.play();
        }
        else if (action.equalsIgnoreCase(ACTION_NEXT)){
            mTransportControls.skipToNext();
        }
        else if (action.equalsIgnoreCase(ACTION_PREV)){
            mTransportControls.skipToPrevious();
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see {@link android.media.session.MediaController.Callback#onSessionDestroyed()})
     */
    private void updateSessionToken() {

        MediaSession.Token freshToken = (MediaSession.Token)mediaService.getSessionToken();
        if (mSessionToken == null || !mSessionToken.equals(freshToken)) {
            if (mController != null) {
                mController.unregisterCallback(mCb);
            }
            mSessionToken = freshToken;
            mController = new MediaController(mService, mSessionToken);
            mTransportControls = mController.getTransportControls();
            if (mStarted) {
                mController.registerCallback(mCb);
            }
        }
    }

    private PendingIntent createContentIntent() {

        Intent openUI = new Intent(mService, mediaService.getServiceClass());
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(mService, REQUEST_CODE, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private final MediaController.Callback mCb = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            mPlaybackState = state;

            if (state != null && (state.getState() == PlaybackState.STATE_STOPPED ||
                    state.getState() == PlaybackState.STATE_NONE)) {
                stopNotification();
            } else {
                Notification notification = createNotification();
                if (notification != null) {
                    mNotificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            mMetadata = metadata;

            Notification notification = createNotification();
            if (notification != null) {
                mNotificationManager.notify(NOTIFICATION_ID, notification);
            }
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();

            updateSessionToken();
        }
    };

    private Notification createNotification() {

        if (mMetadata == null || mPlaybackState == null) {
            return null;
        }

        Notification.Builder notificationBuilder = new Notification.Builder(mService);
        int playPauseButtonPosition = 0;

        // If skip to previous action is enabled
        if ((mPlaybackState.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0) {
            notificationBuilder.addAction(android.R.drawable.ic_media_previous, "Previous", mPreviousIntent);

            // If there is a "skip to previous" button, the play/pause button will
            // be the second one. We need to keep track of it, because the MediaStyle notification
            // requires to specify the index of the buttons (actions) that should be visible
            // when in compact view.
            playPauseButtonPosition = 1;
        }

        addPlayPauseAction(notificationBuilder);

        // If skip to next action is enabled
        if ((mPlaybackState.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) != 0) {
            notificationBuilder.addAction(android.R.drawable.ic_media_next, "Next", mNextIntent);
        }

        MediaDescription description = mMetadata.getDescription();

        notificationBuilder
                .setStyle(new Notification.MediaStyle()
                        .setShowActionsInCompactView(
                                new int[]{playPauseButtonPosition})  // show only play/pause in compact view
                        .setMediaSession(mSessionToken))
                .setSmallIcon(R.drawable.icon)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setUsesChronometer(true)
                .setContentIntent(createContentIntent())
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle());

        setNotificationPlaybackState(notificationBuilder);

        if (currentBitmap != null) {
            notificationBuilder.setLargeIcon(currentBitmap);
        }

        return notificationBuilder.build();
    }

    private void addPlayPauseAction(Notification.Builder builder) {

        String label;
        int icon;
        PendingIntent intent;
        if (mPlaybackState.getState() == PlaybackState.STATE_PLAYING) {
            label = "Pause";
            icon = android.R.drawable.ic_media_pause;
            intent = mPauseIntent;
        } else {
            label = "Play";
            icon = android.R.drawable.ic_media_play;
            intent = mPlayIntent;
        }
        builder.addAction(new Notification.Action(icon, label, intent));
    }

    private void setNotificationPlaybackState(Notification.Builder builder) {

        if (mPlaybackState == null || !mStarted) {

            mService.stopForeground(true);
            return;
        }
        if (mPlaybackState.getState() == PlaybackState.STATE_PAUSED || mPlaybackState.getPosition() == 0) {
            builder
                    .setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(true);
        }
        else if (mPlaybackState.getState() == PlaybackState.STATE_PLAYING && mPlaybackState.getPosition() >= 0) {

            builder
                    .setWhen(System.currentTimeMillis() - mPlaybackState.getPosition())
                    .setShowWhen(true)
                    .setUsesChronometer(true);
        } else {
            builder
                    .setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false);
        }

        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(mPlaybackState.getState() == PlaybackState.STATE_PLAYING);
    }
}