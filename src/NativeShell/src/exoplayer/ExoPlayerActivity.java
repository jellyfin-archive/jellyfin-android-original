package org.jellyfin.mobile.exoplayer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArrayMap;
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
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
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

import java.util.Map;

public class ExoPlayerActivity extends Activity {

    private SimpleExoPlayer player = null;
    private ExoPlayerEventListener eventListener = null;
    private Handler timeUpdatesHandler = null;
    private DefaultTrackSelector trackSelector = null;
    private Map<Integer, Integer> selections;
    private boolean playbackEnded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ExoPlayer.setPlayer(this);
        eventListener = new ExoPlayerEventListener(this);

        // toggle fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // initialize the track selector
        trackSelector = new DefaultTrackSelector();

        // initialize exoplayer
        player = ExoPlayerFactory.newSimpleInstance(getApplicationContext(), trackSelector);

        // set player view layout
        setContentView(R.layout.exoplayer);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PlayerView playerView = findViewById(R.id.exoPlayer);
        playerView.setPlayer(player);

        MediaSource mediaSource = null;
        long mediaStartTicks = 0;

        selections = new ArrayMap();
        selections.put(C.TRACK_TYPE_VIDEO, -1);
        selections.put(C.TRACK_TYPE_AUDIO, -1);
        selections.put(C.TRACK_TYPE_TEXT, -1);

        playbackEnded = false;

        try {
            JSONObject item = new JSONObject(getIntent().getStringExtra("item"));
            JSONObject mediaSourceInfo = item.getJSONObject("mediaSource");

            mediaStartTicks = item.getLong("playerStartPositionTicks");
            if (!mediaSourceInfo.isNull("DefaultSubtitleStreamIndex")) {
                selections.put(C.TRACK_TYPE_TEXT, mediaSourceInfo.getInt("DefaultSubtitleStreamIndex"));
            }

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

        if (event.equals(Constants.EVENT_ENDED)) {
            playbackEnded = true;
            this.finish();
        }
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

    public void pause() {
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    public void resume() {
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    public void stop() {
        if (player != null) {
            player.stop();
            notifyEvent(Constants.EVENT_ENDED);
        }
    }

    public void processGroupTracks(TrackSelectionArray selections) {
        /*DefaultTrackSelector.ParametersBuilder parameters = trackSelector.buildUponParameters();
        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();

        for (int index = 0; index < info.getRendererCount(); index++) {
            int rendererType = player.getRendererType(index);
            TrackSelection currentSelected = selections.get(index);
            int currentSelectedIndex = currentSelected != null ? currentSelected.getSelectedIndexInTrackGroup() : -1;
            int selectedTrackIndex = this.selections.containsKey(rendererType) ? this.selections.get(rendererType) : -1;

            // change current Track
            if (selectedTrackIndex > -1 && selectedTrackIndex != currentSelectedIndex) {
                DefaultTrackSelector.SelectionOverride newSelection = new DefaultTrackSelector.SelectionOverride(selectedTrackIndex, selectedTrackIndex);
                parameters = parameters.setSelectionOverride(index, info.getTrackGroups(index), newSelection);
            }
        }

        trackSelector.setParameters(parameters);*/
    }

    /**
     * builds a media source to feed the player being loaded
     * @param item json object containing all necessary info about the item to be played.
     * @return     a MediaSource object. This could be a result of a MergingMediaSource or a ProgressiveMediaSource, between others
     */
    private MediaSource fetchMediaSources(JSONObject item) throws JSONException {
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "Jellyfin android"));
        MediaSource mediaSource = getVideoMediaSource(item, dataSourceFactory);

        mediaSource = fetchSubtitleStreams(item, mediaSource, dataSourceFactory);

        return mediaSource;
    }

    private MediaSource getVideoMediaSource(JSONObject item, DataSource.Factory dataSourceFactory) throws JSONException {
        boolean bHls = false;
        Uri uri = Uri.parse(item.getString("url"));
        MediaSource mediaSource;

        try {
            bHls = item.getJSONObject("mediaSource").getString("TranscodingSubProtocol").equals("hls");
        } catch (JSONException e) {
            // it's not hls, assume default media source
        }

        if (bHls) {
            mediaSource = new HlsMediaSource.Factory(dataSourceFactory).setAllowChunklessPreparation(true).createMediaSource(uri);
        } else {
            mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
        }

        return mediaSource;
    }

    /**
     * fetches all subtitle streams present in item, adding them to mediaSource
     * @param item              item being played
     * @param mediaSource       media source to add parsed subtitles
     * @param dataSourceFactory data source factory instance
     * @return                  media source with parsed subtitles
     * @throws JSONException
     */
    private MediaSource fetchSubtitleStreams(JSONObject item, MediaSource mediaSource, DataSource.Factory dataSourceFactory) throws JSONException {
        JSONArray textTracks = item.getJSONArray("textTracks");
        ArrayMap<Integer, String> externalTracks = new ArrayMap<>();

        for (int i = 0; i < textTracks.length(); i++) {
            JSONObject track = textTracks.getJSONObject(i);
            externalTracks.put(track.getInt("index"), track.getString("url"));
        }

        JSONObject mediaSourceObject = item.getJSONObject("mediaSource");
        JSONArray streams = mediaSourceObject.getJSONArray("MediaStreams");
        int selectedSubtitleIndex = -1;

        for (int i = 0; i < streams.length(); i++) {
            JSONObject stream = streams.getJSONObject(i);

            if (stream.getString("Type").equals("Subtitle")) {
                selectedSubtitleIndex++;
                String trackIndex = stream.getString("Index");
                String language = stream.has("Language") ? stream.getString("Language") : null;
                String deliveryMethod = stream.getString("DeliveryMethod");

                if (selections.get(C.TRACK_TYPE_TEXT) == (new Integer(trackIndex)).intValue()) {
                    selections.put(C.TRACK_TYPE_TEXT, selectedSubtitleIndex);
                }

                if (deliveryMethod.equals("External")) {
                    String uri = externalTracks.get(trackIndex);
                    String format = fetchSubtitleFormat(stream.getString("Codec"));

                    if (format != null) {
                        Format subtitleFormat = Format.createTextSampleFormat(trackIndex, format, C.SELECTION_FLAG_AUTOSELECT, language);
                        MediaSource subtitleMediaSource = new SingleSampleMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(uri), subtitleFormat, C.TIME_UNSET);
                        mediaSource = new MergingMediaSource(mediaSource, subtitleMediaSource);
                    }
                }
            }
        }

        return mediaSource;
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

        if (!playbackEnded) {
            notifyEvent(Constants.EVENT_ENDED);
        }

        player.release();
        super.onDestroy();
    }

    private void startTimeUpdates() {
        timeUpdatesHandler = new Handler();
        timeUpdatesHandler.postDelayed(timeUpdateCallabck, 0);
    }

    private void stopTimeUpdates() {
        timeUpdatesHandler.removeCallbacks(timeUpdateCallabck);
        // notifies that the video is paused, kind of stopped
        notifyEvent(Constants.EVENT_PAUSE);
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