package org.jellyfin.mobile.exoplayer;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.arch.core.util.Function;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ui.PlayerView;

import org.jellyfin.mobile.R;
import org.jellyfin.mobile.exoplayer.mediaSource.ExoPlayerBaseTrack;
import org.jellyfin.mobile.exoplayer.mediaSource.ExoPlayerMediaSource;
import org.jellyfin.mobile.exoplayer.mediaSource.ExoPlayerTextTrack;
import org.jellyfin.mobile.exoplayer.mediaSource.ExoPlayerTracksGroup;

import java.util.HashMap;
import java.util.Map;

public class ExoPlayerButtons {

    private final int SUBTITLES_MENU_INDEX = 0;
    private final int PLAYBACK_SETTINGS_MENU_INDEX = 1;

    private final int AUDIO_SUBMENU_INDEX = 0;

    private final int SUBTITLES_MENU_CALLBACK_INDEX = 0;
    private final int AUDIO_MENU_CALLBACK_INDEX = 1;

    private final int SUBTITLES_MENU_GROUP = 0;
    private final int AUDIO_MENU_GROUP = 1;

    View subtitlesButton = null;
    View playbackSettingsButton = null;
    View fullscreenButton = null;
    View exitFullscreenButton = null;


    Map<Integer, PopupMenu> menus = new HashMap<>();
    Map<Integer, Function<Integer, Void>> callbacks = new HashMap<>();

    public ExoPlayerButtons(AppCompatActivity context, ExoPlayerMediaSource item) {
        initButtons(context, item);
    }

    public void refreshItem(ExoPlayerMediaSource item) {
        Menu menu = menus.get(SUBTITLES_MENU_INDEX).getMenu();
        buildMenuItems(menu, item.getTracksGroup().get(C.TRACK_TYPE_TEXT), true, SUBTITLES_MENU_GROUP);

        menu = menus.get(SUBTITLES_MENU_INDEX).getMenu().getItem(AUDIO_SUBMENU_INDEX).getSubMenu();
        buildMenuItems(menu, item.getTracksGroup().get(C.TRACK_TYPE_AUDIO), true, AUDIO_MENU_GROUP);
    }

    public void setOnSubtitleTrackSelected(Function<Integer, Void> callback) {
        callbacks.put(SUBTITLES_MENU_CALLBACK_INDEX, callback);
    }

    public void setOnAudioTrackSelected(Function<Integer, Void> callback) {
        callbacks.put(AUDIO_MENU_CALLBACK_INDEX, callback);
    }

    private void initButtons(AppCompatActivity context, ExoPlayerMediaSource item) {
        subtitlesButton = context.findViewById(R.id.subtitles_button);
        playbackSettingsButton = context.findViewById(R.id.playback_settings);

        initSubtitlesMenu(context, item);
        initPlaybackSettingsMenu(context, item);

        subtitlesButton.setOnClickListener(v -> {
            menus.get(SUBTITLES_MENU_INDEX).show();
        });

        playbackSettingsButton.setOnClickListener(v -> {
            menus.get(PLAYBACK_SETTINGS_MENU_INDEX).show();
        });

        fullscreenButton = context.findViewById(R.id.fullscreen);
        exitFullscreenButton = context.findViewById(R.id.exit_fullscreen);

        context.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
            if (ExoPlayerActivity.systemUiVisible(visibility)) {
                fullscreenButton.setVisibility(View.VISIBLE);
                exitFullscreenButton.setVisibility(View.GONE);
            } else {
                fullscreenButton.setVisibility(View.GONE);
                exitFullscreenButton.setVisibility(View.VISIBLE);
            }
        });

        fullscreenButton.setOnClickListener(v -> hideSystemUi(context));
        exitFullscreenButton.setOnClickListener(v -> showSystemUi(context));
    }

    private void initSubtitlesMenu(Context context, ExoPlayerMediaSource item) {
        PopupMenu subtitlesMenu = new PopupMenu(context, subtitlesButton);
        ExoPlayerTracksGroup<ExoPlayerTextTrack> subtitleTracks = item.getTracksGroup().get(C.TRACK_TYPE_TEXT);

        Menu menu = subtitlesMenu.getMenu();
        buildMenuItems(menu, item.getTracksGroup().get(C.TRACK_TYPE_TEXT), false, SUBTITLES_MENU_GROUP);

        subtitlesMenu.setOnMenuItemClickListener(menuItem -> baseMenuClickHandler(menuItem, SUBTITLES_MENU_CALLBACK_INDEX));

        menus.put(SUBTITLES_MENU_INDEX, subtitlesMenu);
    }

    private void initPlaybackSettingsMenu(Context context, ExoPlayerMediaSource item) {
        PopupMenu playbackSettingsMenu = new PopupMenu(context, playbackSettingsButton);
        Menu menu = playbackSettingsMenu.getMenu();

        Menu subMenu = menu.addSubMenu("Audio");
        buildMenuItems(subMenu, item.getTracksGroup().get(C.TRACK_TYPE_AUDIO), false, AUDIO_MENU_GROUP);
        playbackSettingsMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getGroupId() == AUDIO_MENU_GROUP) {
                return baseMenuClickHandler(menuItem, AUDIO_MENU_CALLBACK_INDEX);
            } else {
                //TODO: video tracks here
            }
            return true;
        });

        menus.put(PLAYBACK_SETTINGS_MENU_INDEX, playbackSettingsMenu);
    }

    private void buildMenuItems(Menu menu, ExoPlayerTracksGroup<ExoPlayerBaseTrack> tracksGroup, boolean dontCheckSelectedTrack, Integer groupId) {
        menu.clear();

        for (Map.Entry<Integer, ExoPlayerBaseTrack> trackItem : tracksGroup.getTracks().entrySet()) {
            ExoPlayerBaseTrack track = trackItem.getValue();
            menu.add(groupId, track.getIndex(), 0, track.getTitle());
        }

        menu.setGroupCheckable(groupId, true, true);
        Integer selectedTrack = tracksGroup.getSelectedTrack();

        if (dontCheckSelectedTrack) {
            return;
        }

        if (selectedTrack > -1) {
            for (int index = 0; index < menu.size(); index++) {
                MenuItem menuItem = menu.getItem(index);

                if (menuItem.getItemId() == selectedTrack) {
                    menuItem.setChecked(true);
                    break;
                }
            }
        } else {
            // no selection, lets check the first item
            menu.getItem(0).setChecked(true);
        }
    }

    private boolean baseMenuClickHandler(MenuItem menuItem, Integer callbackType) {
        menuItem.setChecked(true);

        if (callbacks.containsKey(callbackType)) {
            callbacks.get(callbackType).apply(menuItem.getItemId());
        }

        return true;
    }

    private void showSystemUi(AppCompatActivity context) {
        final int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        context.getWindow().getDecorView().setSystemUiVisibility(visibility);
        context.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void hideSystemUi(AppCompatActivity context) {
        int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        context.getWindow().getDecorView().setSystemUiVisibility(visibility);
        context.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}
