package com.mb.android.media;

import android.content.Context;

import com.mb.android.preferences.PreferencesProvider;

import org.videolan.libvlc.LibVLC;

import java.util.ArrayList;
import java.util.HashMap;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.sync.data.AndroidAssetManager;
import mediabrowser.apiinteraction.playback.PlaybackManager;
import mediabrowser.apiinteraction.sync.data.ILocalAssetManager;
import mediabrowser.model.dlna.DeviceProfile;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dlna.VideoOptions;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.mediainfo.PlaybackInfoRequest;
import mediabrowser.model.mediainfo.PlaybackInfoResponse;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.PlayMethod;
import mediabrowser.model.session.PlaybackProgressInfo;
import mediabrowser.model.sync.LocalItem;

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
    private Integer originalMaxBitrate;

    private ILocalAssetManager localAssetManager;
    private PreferencesProvider preferencesProvider;

    public boolean enableProgressReporting;
    private VideoPlayerActivity activity;

    public VideoApiHelper(VideoPlayerActivity context, ILogger logger, IJsonSerializer jsonSerializer) {
        this.logger = logger;
        this.jsonSerializer = jsonSerializer;
        this.activity = context;
        localAssetManager = new AndroidAssetManager(context, logger, jsonSerializer);
        preferencesProvider = new PreferencesProvider(context, logger);
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

    public int getMaxBitrate() {
        return originalMaxBitrate;
    }

    public PlaybackProgressInfo getPlaybackProgressInfo() {
        return playbackStartInfo;
    }

    public void setInitialInfo(String serverId, boolean isOffline, ApiClient apiClient, DeviceProfile deviceProfile, PlaybackProgressInfo playbackStartInfo )
    {
        this.apiClient = apiClient;
        this.playbackStartInfo = playbackStartInfo;
        this.deviceProfile = deviceProfile;
        this.serverId = serverId;
        this.isOffline = isOffline;
        originalMaxBitrate = deviceProfile.getMaxStreamingBitrate();
    }

    public void setAudioStreamIndex(LibVLC vlc, int index, MediaSourceInfo currentMediaSource){

        if (playbackStartInfo.getAudioStreamIndex() != null && index == playbackStartInfo.getAudioStreamIndex()) {
            return;
        }

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

    public void setQuality(LibVLC vlc, int bitrate, int maxHeight, MediaSourceInfo currentMediaSource) {

        long positionTicks = vlc.getTime() * 10000;

        logger.Info("Changing quality to %s", bitrate);
        changeStream(currentMediaSource, positionTicks, null, null, bitrate);

        originalMaxBitrate = bitrate;
        preferencesProvider.set("preferredVideoBitrate", String.valueOf(bitrate));
    }

    private void changeStream(MediaSourceInfo currentMediaSource, final long positionTicks, Integer newAudioStreamIndex, Integer newSubtitleStreamIndex, final Integer newMaxBitrate){

        final String playSessionId = playbackStartInfo.getPlaySessionId();
        String liveStreamId = playbackStartInfo.getLiveStreamId();

        final Integer audioStreamIndex = newAudioStreamIndex != null ? newAudioStreamIndex : playbackStartInfo.getAudioStreamIndex();
        final Integer subtitleStreamIndex = newSubtitleStreamIndex != null ? newSubtitleStreamIndex : playbackStartInfo.getSubtitleStreamIndex();

        final Integer maxBitrate = newMaxBitrate != null ? newMaxBitrate : originalMaxBitrate;
        final String itemId = playbackStartInfo.getItemId();

        getPlaybackInfo(itemId, positionTicks, currentMediaSource, audioStreamIndex, subtitleStreamIndex, maxBitrate, liveStreamId, new Response<PlaybackInfoResponse>() {

            @Override
            public void onResponse(PlaybackInfoResponse response) {

                if (validatePlaybackInfoResult(response)) {

                    MediaSourceInfo newMediaSource = response.getMediaSources().get(0);

                    setNewPlaybackInfo(itemId, newMediaSource, playSessionId, response.getPlaySessionId(), positionTicks);

                    playbackStartInfo.setAudioStreamIndex(audioStreamIndex);
                }
            }
        });
    }

    private boolean validatePlaybackInfoResult(PlaybackInfoResponse result) {

        // TODO
        if (result.getErrorCode() != null) {

            return false;
        }

        return true;
    }

    private void setNewPlaybackInfo(String itemId, MediaSourceInfo newMediaSource, final String previousPlaySessionId, final String newPlaySessionId, long positionTicks) {

        String newMediaPath = null;
        PlayMethod playMethod = PlayMethod.Transcode;

        // TODO: Direct play ??

        if (newMediaPath == null && newMediaSource.getSupportsDirectStream()){
            newMediaPath =  apiClient.GetApiUrl("Videos/" + itemId + "/stream." + newMediaSource.getContainer() + "?Static=true&api_key="+apiClient.getAccessToken()+"&MediaSourceId=" + newMediaSource.getId());
            playMethod = PlayMethod.DirectStream;
        }

        if (newMediaPath == null){
            newMediaPath = apiClient.GetApiUrl(newMediaSource.getTranscodingUrl());
        }

        final String path = newMediaPath;
        final PlayMethod method = playMethod;

        // First stop transcoding
        apiClient.StopTranscodingProcesses(apiClient.getDeviceId(), previousPlaySessionId, new EmptyResponse(){

            @Override
            public void onResponse(){

                // Tell VideoPlayerActivity to changeStream
                activity.changeLocation(path);

                playbackStartInfo.setPlayMethod(method);
                playbackStartInfo.setPlaySessionId(newPlaySessionId);
            }
        });
    }

    private void getPlaybackInfo(String itemId, long startPosition, MediaSourceInfo mediaSource, Integer audioStreamIndex, Integer subtitleStreamIndex, Integer maxBitrate, String liveStreamId, Response<PlaybackInfoResponse> response) {

        MediaSourceInfo localMediaSource = getLocalMediaSource(serverId, itemId);

        // Use the local media source if a specific one wasn't requested, or the same one was requested
        if (localMediaSource != null && (mediaSource == null || mediaSource.getId() == localMediaSource.getId())) {

            localMediaSource.setSupportsDirectPlay(true);

            PlaybackInfoResponse playbackInfoResponse = new PlaybackInfoResponse();
            ArrayList<MediaSourceInfo> mediaSourceInfos = new ArrayList<MediaSourceInfo>();
            mediaSourceInfos.add(localMediaSource);
            playbackInfoResponse.setPlaySessionId(playbackStartInfo.getPlaySessionId());
            playbackInfoResponse.setMediaSources(mediaSourceInfos);
            response.onResponse(playbackInfoResponse);
            return;
        }

        getPlaybackInfoInternal(itemId, startPosition, mediaSource, audioStreamIndex, subtitleStreamIndex, maxBitrate, liveStreamId, response);
    }

    private void getPlaybackInfoInternal(String itemId, long startPosition, MediaSourceInfo mediaSource, Integer audioStreamIndex, Integer subtitleStreamIndex, Integer maxBitrate, String liveStreamId, Response<PlaybackInfoResponse> response) {

        PlaybackInfoRequest request = new PlaybackInfoRequest();

        request.setDeviceProfile(deviceProfile);
        request.setUserId(apiClient.getCurrentUserId());
        request.setStartTimeTicks(startPosition);
        request.setAudioStreamIndex(audioStreamIndex);
        request.setSubtitleStreamIndex(subtitleStreamIndex);
        request.setMediaSourceId(mediaSource.getId());
        request.setLiveStreamId(liveStreamId);
        request.setId(itemId);

        // If null, that's ok, the value in the profile will be used
        request.setMaxStreamingBitrate(maxBitrate);

        apiClient.GetPlaybackInfoWithPost(request, response);
    }

    private MediaSourceInfo getLocalMediaSource(String serverId, String itemId) {

        LocalItem item = localAssetManager.getLocalItem(serverId, itemId);

        if (item != null && item.getItem().getMediaSources().size() > 0) {

            MediaSourceInfo mediaSourceInfo = item.getItem().getMediaSources().get(0);

            boolean fileExists = localAssetManager.fileExists(mediaSourceInfo.getPath());

            if (fileExists) {
                return mediaSourceInfo;
            }

        }

        return null;
    }
}
