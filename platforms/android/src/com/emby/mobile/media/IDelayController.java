package com.emby.mobile.media;

public interface IDelayController {
    public enum DelayState {OFF, AUDIO, SUBS};

    public void showAudioDelaySetting();
    public void showSubsDelaySetting();
    public void endDelaySetting();
}