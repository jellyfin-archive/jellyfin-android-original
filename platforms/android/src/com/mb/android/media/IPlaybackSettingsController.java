package com.mb.android.media;

public interface IPlaybackSettingsController {
    enum DelayState {OFF, AUDIO, SUBS, SPEED};
    void showAudioDelaySetting();
    void showSubsDelaySetting();
    void showPlaybackSpeedSetting();
    void endPlaybackSetting();
}