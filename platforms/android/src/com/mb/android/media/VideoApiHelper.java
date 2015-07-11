package com.mb.android.media;

import org.videolan.libvlc.LibVLC;

import java.util.ArrayList;
import java.util.HashMap;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.playback.PlaybackManager;
import mediabrowser.model.dlna.DeviceProfile;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dlna.VideoOptions;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.mediainfo.PlaybackInfoResponse;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.PlayMethod;
import mediabrowser.model.session.PlaybackProgressInfo;

/**
 * Created by Luke on 7/10/2015.
 */
public class VideoApiHelper {

    private DeviceProfile deviceProfile;
    private PlaybackProgressInfo playbackStartInfo;
    private ILogger logger;
    private IJsonSerializer jsonSerializer;
    private ApiClient apiClient;
    private String serverId;
    private boolean isOffline;

    public boolean enableProgressReporting;

    public VideoApiHelper(ILogger logger, IJsonSerializer jsonSerializer) {
        this.logger = logger;
        this.jsonSerializer = jsonSerializer;
    }

    public void ReportPlaybackProgress(Long duration, Long time, int volume, boolean isPaused) {

        if (!enableProgressReporting){
            return;
        }

        PlaybackProgressInfo info = playbackStartInfo;

        info.setVolumeLevel(volume);
        info.setIsPaused(isPaused);
        info.setPositionTicks(time * 10000);

        apiClient.ReportPlaybackProgressAsync(info, new EmptyResponse());
    }

    public void setInitialInfo(String serverId, boolean isOffline, ApiClient apiClient, DeviceProfile deviceProfile, PlaybackProgressInfo playbackStartInfo )
    {
        this.apiClient = apiClient;
        this.playbackStartInfo = playbackStartInfo;
        this.deviceProfile = deviceProfile;
        this.serverId = serverId;
        this.isOffline = isOffline;
    }

    public void setAudioStreamIndex(LibVLC vlc, int index, MediaSourceInfo currentMediaSource){

        if (playbackStartInfo.getPlayMethod() == PlayMethod.Transcode){

            // If transcoding, we're going to need to stop and restart the stream

            long positionTicks = vlc.getTime() * 10000;
            changeStream(currentMediaSource, positionTicks, index, null, null);
        }
        else{
            vlc.setAudioTrack(index);
            playbackStartInfo.setAudioStreamIndex(index);
        }
    }

    public void setSubtitleStreamIndex(int index){

    }

    private void changeStream(MediaSourceInfo currentMediaSource, final long positionTicks, Integer newAudioStreamIndex, Integer newSubtitleStreamIndex, Long newMaxBitrate){

        final String playSessionId = playbackStartInfo.getPlaySessionId();
        String liveStreamId = playbackStartInfo.getLiveStreamId();

        Integer audioStreamIndex = playbackStartInfo.getAudioStreamIndex();
        final Integer subtitleStreamIndex = playbackStartInfo.getSubtitleStreamIndex();

        String itemId = playbackStartInfo.getItemId();

        getPlaybackInfo(itemId, positionTicks, currentMediaSource, audioStreamIndex, subtitleStreamIndex, liveStreamId, new Response<PlaybackInfoResponse>() {

            @Override
            public void onResponse(PlaybackInfoResponse response) {

                MediaSourceInfo newMediaSource = response.getMediaSources().get(0);

                // TODO: Is this falsely assuming the new playback method is transcoding?
                String newMediaPath = apiClient.GetApiUrl(newMediaSource.getTranscodingUrl());

                setNewPlaybackInfo(newMediaSource, newMediaPath, playSessionId, positionTicks);
            }
        });
    }

    private void setNewPlaybackInfo(MediaSourceInfo newMediaSource, String newMediaPath, String playSessionId, long positionTicks) {

        // First stop transcoding
        apiClient.StopTranscodingProcesses(apiClient.getDeviceId(), playSessionId, new EmptyResponse(){

            @Override
            public void onResponse(){

                // Tell VideoPlayerActivity to changeStream

                // self.updateTextStreamUrls(newPositionTicks || 0);
            }
        });
    }

    private void getPlaybackInfo(String itemId, long startPosition, MediaSourceInfo mediaSource, Integer audioStreamIndex, Integer subtitleStreamIndex, String liveStreamId, Response<PlaybackInfoResponse> response) {

/*        var serverInfo = ApiClient.serverInfo();

        if (serverInfo.Id) {
            var localMediaSource = window.LocalAssetManager.getLocalMediaSource(serverInfo.Id, itemId);

            // Use the local media source if a specific one wasn't requested, or the smae one was requested
            if (localMediaSource && (!mediaSource || mediaSource.Id == localMediaSource.Id)) {

                var playbackInfo = getPlaybackInfoFromLocalMediaSource(itemId, deviceProfile, startPosition, localMediaSource);

                deferred.resolveWith(null, [playbackInfo]);
                return;
            }
        }*/

        PostClass postData = new PostClass();

        postData.DeviceProfile = deviceProfile;
        postData.UserId = apiClient.getCurrentUserId();
        postData.StartTimeTicks = startPosition;
        postData.AudioStreamIndex = audioStreamIndex;
        postData.SubtitleStreamIndex = subtitleStreamIndex;
        postData.MediaSourceId = mediaSource.getId();
        postData.LiveStreamId = playbackStartInfo.getLiveStreamId();
    }

    private class PostClass{
        public DeviceProfile DeviceProfile;

        public String UserId;

        public long StartTimeTicks;

        public Integer AudioStreamIndex;

        public Integer SubtitleStreamIndex;

        public String MediaSourceId;

        public String LiveStreamId;
    }
}
