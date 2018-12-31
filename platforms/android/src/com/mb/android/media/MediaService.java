package com.mb.android.media;

import android.annotation.TargetApi;

import com.mb.android.api.ApiClientBridge;
import com.mb.android.logging.AppLogger;

import org.jellyfin.mobile.MainActivity;

import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.android.mediabrowser.BaseMediaBrowserService;
import mediabrowser.apiinteraction.android.mediabrowser.IMediaRes;
import mediabrowser.apiinteraction.android.mediabrowser.IPlayback;
import mediabrowser.apiinteraction.android.mediabrowser.IPlaybackCallback;
import mediabrowser.logging.ConsoleLogger;
import mediabrowser.model.logging.ILogger;

@TargetApi(21)
public class MediaService extends BaseMediaBrowserService implements IPlaybackCallback {

    @Override
    protected ILogger createLogger() {
        return AppLogger.getLogger(getApplicationContext());
    }

    @Override
    protected IPlayback createPlayback() {
        return new Playback(this, mMediaProvider, logger);
    }

    @Override
    protected IMediaRes createMediaRes() {
        return new MediaRes();
    }

    @Override
    protected VolleyHttpClient getHttpClient() {

        VolleyHttpClient httpClient = null;
        if (ApiClientBridge.Current != null) {
            httpClient = ApiClientBridge.Current.httpClient;
        }

        if (httpClient == null) {
            httpClient = new VolleyHttpClient(new ConsoleLogger(), getApplicationContext());
        }

        return httpClient;
    }

    @Override
    public Class<?> getServiceClass() {
        return MediaService.class;
    }

    @Override
    public Class getAudioPlayerActivityClass() {
        return MainActivity.class;
    }

    @Override
    protected void handleNextTrackRequest() {
        MainActivity.sendCommand("next");
    }

    @Override
    protected void handlePreviousTrackRequest() {
        MainActivity.sendCommand("previous");
    }
}