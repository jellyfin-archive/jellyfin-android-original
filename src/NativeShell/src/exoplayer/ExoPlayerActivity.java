package org.jellyfin.mobile.exoplayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.neovisionaries.i18n.LanguageAlpha3Code;
import com.neovisionaries.i18n.LocaleCode;

import org.jellyfin.mobile.Constants;
import org.jellyfin.mobile.R;
import org.jellyfin.mobile.exoplayer.mediaSource.ExoPlayerMediaSource;
import org.jellyfin.mobile.exoplayer.mediaSource.ExoPlayerTextTrack;
import org.jellyfin.mobile.exoplayer.mediaSource.ExoPlayerTracksGroup;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static com.google.android.exoplayer2.ExoPlayer.EventListener;

public class ExoPlayerActivity extends AppCompatActivity implements EventListener {

    private SimpleExoPlayer player = null;
    private ExoPlayerEventListener eventListener = null;
    private Handler timeUpdatesHandler = null;
    private DefaultTrackSelector trackSelector = null;
    private boolean playbackEnded = false;
    private View progressIndicator;
    private ExoPlayerMediaSource item = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ExoPlayer.setPlayer(this);
        eventListener = new ExoPlayerEventListener(this);

        // initialize the track selector
        trackSelector = new DefaultTrackSelector(getApplicationContext());

        // initialize exoplayer
        player = new SimpleExoPlayer.Builder(getApplicationContext()).setTrackSelector(trackSelector).build();

        // set player view layout
        setContentView(R.layout.exo_player);

        progressIndicator = findViewById(R.id.progress_indicator);

        View captionsIcon = findViewById(R.id.subtitles_button);
        captionsIcon.setOnClickListener(this::showCaptionsMenu);

        View playbackSettings = findViewById(R.id.playback_settings);
        playbackSettings.setOnClickListener(v -> {
//            new TrackSelectionDialogBuilder(this, "Things", trackSelector, 0)
//                    .build()
//                    .show();

//            PopupMenu menu = new PopupMenu(this, v, Gravity.TOP);
//            menu.getMenu()
//                    .add("Quality");
//            menu.show();
        });

        View fullscreenIcon = findViewById(R.id.fullscreen);
        View exitFullscreenIcon = findViewById(R.id.exit_fullscreen);

        PlayerView playerView = findViewById(R.id.exoPlayer);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
            if (systemUiVisible(visibility)) {
                fullscreenIcon.setVisibility(View.VISIBLE);
                exitFullscreenIcon.setVisibility(View.GONE);
            } else {
                fullscreenIcon.setVisibility(View.GONE);
                exitFullscreenIcon.setVisibility(View.VISIBLE);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(playerView, (view, insets) -> {
            WindowInsetsCompat windowInsets = insets;

            int systemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            if (systemUiVisible(systemUiVisibility)) {
                ViewCompat.onApplyWindowInsets(view, windowInsets);
            } else {
                windowInsets = insets.consumeSystemWindowInsets();
                ViewCompat.onApplyWindowInsets(view, windowInsets);
            }

            return windowInsets;
        });

        fullscreenIcon.setOnClickListener(v -> hideSystemUi());
        exitFullscreenIcon.setOnClickListener(v -> showSystemUi());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        playerView.setPlayer(player);

        player.addListener(eventListener);
        player.addAudioListener(eventListener);

        try {
            prepareMediaSource(new JSONObject(getIntent().getStringExtra("item")), false);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        startTimeUpdates();
    }

    private void prepareMediaSource(JSONObject webItem, boolean changeStream) {
        try {
            item = new ExoPlayerMediaSource(webItem);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MediaSource mediaSource = prepareStreams(item);
        playbackEnded = false;

        if (mediaSource == null) {
            //TODO send error to the web client
            this.onDestroy();
        } else {
            long startTicks = item.getMediaStartTicks();

            if (!changeStream && startTicks > 0) {
                player.seekTo(startTicks);
            }

            player.prepare(mediaSource, false, false);
            player.setPlayWhenReady(true);

            notifyEvent(Constants.EVENT_VOLUME_CHANGE, getVolume());
        }
    }

    private void showCaptionsMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            Map<Integer, ExoPlayerTextTrack> tracks = item.getTracksGroup().get(C.TRACK_TYPE_TEXT).getTracks();
            ExoPlayerTextTrack subtitle = tracks.get(menuItem.getItemId());

            Integer subtitleIndex = subtitle.getPlayerIndex();

            if (subtitleIndex == null || (subtitleIndex == -1 && item.isTranscoding())) {
                ExoPlayer.callWebMethod("changeSubtitleStream", String.valueOf(subtitle.getIndex()));
            } else {
                int renderedIndex = getRendererIndex(C.TRACK_TYPE_TEXT);
                MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
                TrackGroupArray trackGroupArray = info.getTrackGroups(renderedIndex);

                DefaultTrackSelector.ParametersBuilder parameters = trackSelector.buildUponParameters();

                if (subtitleIndex == -1) {
                    parameters = parameters.clearSelectionOverride(renderedIndex, trackGroupArray)
                                           .setRendererDisabled(renderedIndex, true);
                } else {
                    DefaultTrackSelector.SelectionOverride newSelection = new DefaultTrackSelector.SelectionOverride(subtitleIndex, 0);
                    parameters = parameters.setSelectionOverride(renderedIndex, trackGroupArray, newSelection)
                                           .setRendererDisabled(renderedIndex, false);
                }

                trackSelector.setParameters(parameters);
            }

            return true;
        });

        Menu menu = popupMenu.getMenu();
        Map<Integer, ExoPlayerTextTrack> tracks = item.getTracksGroup().get(C.TRACK_TYPE_TEXT).getTracks();

        for (Map.Entry<Integer, ExoPlayerTextTrack> trackItem : tracks.entrySet()) {
            ExoPlayerTextTrack track = trackItem.getValue();
            menu.add(0, track.getIndex(), 0, track.getTitle());
        }

        popupMenu.show();
    }

    private boolean systemUiVisible(int visibility) {
        return (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0;
    }

    private void showSystemUi() {
        final int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        getWindow().getDecorView().setSystemUiVisibility(visibility);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void hideSystemUi() {
        int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(visibility);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        int renderedIndex = getRendererIndex(C.TRACK_TYPE_TEXT);

        if (info == null) {
            return;
        }

        TrackGroupArray trackGroupArray = info.getTrackGroups(renderedIndex);
        Map<Integer, ExoPlayerTextTrack> tracks = item.getTracksGroup().get(C.TRACK_TYPE_TEXT).getTracks();

        if (trackGroupArray.length > 0) {
            for (int i = 0; i < trackGroupArray.length; i++) {
                Format subtitle = trackGroupArray.get(i).getFormat(0);
                LocaleCode localeCode = LocaleCode.getByCode(subtitle.language);

                if (localeCode != null) {
                    LanguageAlpha3Code alpha3Language = localeCode.getLanguage().getAlpha3();
                    String alpha2 = alpha3Language.getAlpha2().toString();
                    String alpha3T = alpha3Language.getAlpha3T().toString();
                    String alpha3B = alpha3Language.getAlpha3B().toString();

                    for (Map.Entry<Integer, ExoPlayerTextTrack> entry: tracks.entrySet()) {
                        ExoPlayerTextTrack track = entry.getValue();

                        String language = track.getLanguage();
                        if (track.getPlayerIndex() == null && (alpha2.equals(language) || alpha3T.equals(language) || alpha3B.equals(language))) {
                            track.setPlayerIndex(i);
                            break;
                        }
                    }
                } else if (subtitle.language != null && subtitle.language.equals("und")) { // undefined track
                    for (Map.Entry<Integer, ExoPlayerTextTrack> entry: tracks.entrySet()) {
                        ExoPlayerTextTrack track = entry.getValue();

                        if (track.getPlayerIndex() == null && track.getLanguage().equals("und")) {
                            track.setPlayerIndex(i);
                            break;
                        }
                    }
                }
            }
        }
    }

    private int getRendererIndex(int trackIndex) {
        for (int i = 0; i < player.getRendererCount(); i++) {
            int rendererType = player.getRendererType(i);
            if (rendererType == trackIndex) return i;
        }

        return -1;
    }

    /**
     * builds a media source to feed the player being loaded
     *
     * @param item ExoPlayerMediaSource object containing all necessary info about the item to be played.
     * @return a MediaSource object. This could be a result of a MergingMediaSource or a ProgressiveMediaSource, between others
     */
    private MediaSource prepareStreams(ExoPlayerMediaSource item) {
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "Jellyfin android"));
        MediaSource mediaSource = getVideoMediaSource(item, dataSourceFactory);

        mediaSource = addSubtitleMediaSources(item.getTracksGroup().get(C.TRACK_TYPE_TEXT), mediaSource, dataSourceFactory);

        return mediaSource;
    }

    private MediaSource getVideoMediaSource(ExoPlayerMediaSource item, DataSource.Factory dataSourceFactory) {
        MediaSource mediaSource;
        Uri uri = Uri.parse(item.getUrl());

        if (item.isTranscoding()) {
            mediaSource = new HlsMediaSource.Factory(dataSourceFactory).setAllowChunklessPreparation(true).createMediaSource(uri);
        } else {
            mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
        }

        return mediaSource;
    }

    /**
     * fetches all subtitle streams present in item, adding them to mediaSource
     *
     * @param tracksGroup       ExoPlayerTracksGroup object containing selected index and all subtitle tracks
     * @param mediaSource       media source to add parsed subtitles
     * @param dataSourceFactory data source factory instance
     * @return media source with parsed subtitles
     * @throws JSONException
     */
    private MediaSource addSubtitleMediaSources(ExoPlayerTracksGroup<ExoPlayerTextTrack> tracksGroup, MediaSource mediaSource, DataSource.Factory dataSourceFactory) {

        for (Map.Entry<Integer, ExoPlayerTextTrack> trackItem : tracksGroup.getTracks().entrySet()) {
            ExoPlayerTextTrack track = trackItem.getValue();

            if (track.getUri() != null && track.getFormat() != null) {
                Format subtitleFormat = Format.createTextSampleFormat(String.valueOf(trackItem.getKey()), track.getFormat(), C.SELECTION_FLAG_AUTOSELECT, track.getLanguage());
                MediaSource subtitleMediaSource = new SingleSampleMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(track.getUri()), subtitleFormat, C.TIME_UNSET);
                mediaSource = new MergingMediaSource(mediaSource, subtitleMediaSource);
            }
        }

        return mediaSource;
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

    public void onIsPlayingChanged(boolean isPlaying) {
        if (!isPlaying && player.isLoading()) {
            progressIndicator.setVisibility(View.VISIBLE);
        } else {
            progressIndicator
                    .animate()
                    .alpha(0f)
                    .setDuration(150)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationCancel(Animator animation) {
                            progressIndicator.setVisibility(View.GONE);
                            progressIndicator.setAlpha(1f);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            progressIndicator.setVisibility(View.GONE);
                            progressIndicator.setAlpha(1f);
                        }
                    })
                    .start();
        }
    }

    public void changeStream(JSONObject item) {
        prepareMediaSource(item, true);
    }
}

