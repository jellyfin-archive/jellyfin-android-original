package org.jellyfin.mobile.exoplayer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

import org.jellyfin.mobile.Constants;
import org.jellyfin.mobile.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ExoPlayerActivity extends Activity {

    private SimpleExoPlayer player = null;
    private ExoPlayerEventListener eventListener = null;
    private Handler timeUpdatesHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ExoPlayer.setPlayer(this);

        eventListener = new ExoPlayerEventListener(this);

        // toggle fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // initialize exoplayer
        player = ExoPlayerFactory.newSimpleInstance(getApplicationContext());



        // set player view layout
        setContentView(R.layout.exo_player);

        PlayerView playerView = findViewById(R.id.exoPlayer);
        playerView.setPlayer(player);

        MediaSource mediaSource = null;
        long mediaStartTicks = 0;

        try {
            JSONObject item = new JSONObject(getIntent().getStringExtra("item"));

            mediaStartTicks = item.getLong("playerStartPositionTicks");
            mediaSource = fetchMediaSources(item);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (mediaSource == null) {
            //TODO send error to the web client
            this.onDestroy();
        } else {
            if (mediaStartTicks > 0) {
                player.seekTo(mediaStartTicks / Constants.TICKS_PER_MILLISECOND);
            }

            player.addListener(eventListener);
            player.addAudioListener(eventListener);
            player.prepare(mediaSource, false, false);
            player.setPlayWhenReady(true);

            notifyEvent(Constants.EVENT_VOLUME_CHANGE, getVolume());

            startTimeUpdates();
        }
    }

    public void notifyEvent(String event, String... arguments) {
        ExoPlayer.callWebMethod("notify" + event, arguments);
    }

    public String getVolume() {
        return player == null ? "0" : new Float(player.getAudioComponent().getVolume()).toString();
    }

    public boolean setVolume(float volume) {
        if (player == null) {
            return false;
        }

        player.getAudioComponent().setVolume(volume);
        return true;
    }

    /**
     * builds a media source to feed the player being loaded
     * @param item json object containing all necessary info about the item to be played.
     * @return     a MediaSource object. This could be a result of a MergingMediaSource or a ProgressiveMediaSource, between others
     */
    private MediaSource fetchMediaSources(JSONObject item) throws JSONException {
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "Jellyfin android"));
        MediaSource mediasource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(item.getString("url")));

        // add subtitles if they exist
        JSONArray subtitleTracks = item.getJSONArray("textTracks");

        for (int i = 0; i < subtitleTracks.length(); i++) {
            JSONObject track = subtitleTracks.getJSONObject(i);

            MediaSource subtitleMediaSource = fetchSubtitleMediaSource(track, dataSourceFactory);
            if (subtitleMediaSource != null) {
                mediasource = new MergingMediaSource(mediasource, subtitleMediaSource);
            }
        }

        return mediasource;
    }

    /**
     * creates a subtitle media source based on the parametrized track
     * @param track extracted subtitle track to use
     * @return      a MediaSource object representing the subtitle track
     */
    private MediaSource fetchSubtitleMediaSource(JSONObject track, DataSource.Factory dataSourceFactory) throws JSONException {
        String format = fetchSubtitleFormat(track.getString("format"));
        String trackIndex = track.getString("index");
        String language = track.getString("language");
        String uri = track.getString("url");
        boolean isDefault = track.getBoolean("isDefault");
        int selectionFlag = isDefault ? C.SELECTION_FLAG_FORCED : C.SELECTION_FLAG_AUTOSELECT;

        if (format != null) {
            Format subtitleFormat = Format.createTextSampleFormat(trackIndex, format, selectionFlag, language);

            return new SingleSampleMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(uri), subtitleFormat, C.TIME_UNSET);
        }

        return null;
    }

    /**
     * fetch the exoplayer subtitle format, if supported, otherwise null
     * @param format subtitle format given by jellyfin
     * @return       exoplayer subtitle format, otherwise null if not supported
     */
    private String fetchSubtitleFormat(String format) {
        switch (format) {
            case "ssa":
            case "ass":
                return MimeTypes.TEXT_SSA;
            case "vtt":
                return MimeTypes.TEXT_VTT;
            case "ttml":
                return MimeTypes.APPLICATION_TTML;
            case "srt":
            case "sub":
                return MimeTypes.APPLICATION_SUBRIP;
            default:
                return null;
        }
    }

    @Override
    protected void onDestroy() {
        stopTimeUpdates();
        ExoPlayer.unsetPlayer();
        player.release();
        super.onDestroy();
    }

    private void startTimeUpdates() {
        timeUpdatesHandler = new Handler();
        timeUpdatesHandler.postDelayed(timeUpdateCallabck, 0);
    }

    private void stopTimeUpdates() {
        timeUpdatesHandler.removeCallbacks(timeUpdateCallabck);
        notifyEvent(Constants.EVENT_PAUSE); // notifies that the video is paused, kind of stopped
    }

    private void processTimeUpdate() {
        notifyEvent(Constants.EVENT_TIME_UPDATE, new Long(player.getCurrentPosition()).toString());
        timeUpdatesHandler.postDelayed(timeUpdateCallabck, 1000);
    }

    private final Runnable timeUpdateCallabck = new Runnable() {
        @Override
        public void run() {
            processTimeUpdate();
        }
    };
}
