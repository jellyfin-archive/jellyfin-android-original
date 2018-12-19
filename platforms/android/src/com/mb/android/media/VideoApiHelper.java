package com.mb.android.media;

import com.mb.android.preferences.PreferencesProvider;

import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.sync.data.AndroidAssetManager;
import mediabrowser.apiinteraction.sync.data.ILocalAssetManager;
import mediabrowser.model.dlna.CodecProfile;
import mediabrowser.model.dlna.CodecType;
import mediabrowser.model.dlna.DeviceProfile;
import mediabrowser.model.dlna.ProfileCondition;
import mediabrowser.model.dlna.ProfileConditionType;
import mediabrowser.model.dlna.ProfileConditionValue;
import mediabrowser.model.dlna.SubtitleDeliveryMethod;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.MediaStreamType;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.mediainfo.PlaybackInfoRequest;
import mediabrowser.model.mediainfo.PlaybackInfoResponse;
import mediabrowser.model.mediainfo.SubtitleTrackInfo;
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
    private String serverId;
    private boolean isOffline;
    private Integer originalMaxBitrate;

    private ILocalAssetManager localAssetManager;
    private PreferencesProvider preferencesProvider;

    public boolean enableProgressReporting;
    private VideoPlayerActivity activity;
    private MediaSourceInfo currentMediaSource;
    private ApiClient apiClient;

    public VideoApiHelper(VideoPlayerActivity context, ILogger logger, IJsonSerializer jsonSerializer) {
        this.logger = logger;
        this.jsonSerializer = jsonSerializer;
        this.activity = context;
        localAssetManager = new AndroidAssetManager(context, logger, jsonSerializer);
        preferencesProvider = new PreferencesProvider(context, logger);
    }

    private void updateMediaSourceSubtitleUrls(MediaSourceInfo mediaSource, Long transcodingOffsetTicks){

        long transcodingOffsetTicksValue = transcodingOffsetTicks == null ? 0 : transcodingOffsetTicks;

        for (MediaStream stream : mediaSource.getMediaStreams()){
            if (stream.getType() == MediaStreamType.Subtitle){
                if (stream.getDeliveryUrl() != null){
                    stream.setDeliveryUrl(stream.getDeliveryUrl().replace(String.valueOf(transcodingOffsetTicksValue), "0"));
                }
            }
        }
    }


    public void ReportPlaybackProgress(Long positionTicks, int volume, boolean isPaused) {

        if (!enableProgressReporting){
            return;
        }

        PlaybackProgressInfo info = playbackStartInfo;

        info.setVolumeLevel(volume);
        info.setIsPaused(isPaused);
        info.setPositionTicks(positionTicks);

        apiClient.ReportPlaybackProgressAsync(info, new EmptyResponse());
    }

    public int getMaxBitrate() {
        return originalMaxBitrate;
    }

    public PlaybackProgressInfo getPlaybackProgressInfo() {
        return playbackStartInfo;
    }

    public MediaSourceInfo getMediaSource() {
        return currentMediaSource;
    }

    public void setInitialInfo(String serverId, boolean isOffline, ApiClient apiClient, DeviceProfile deviceProfile, PlaybackProgressInfo playbackStartInfo, MediaSourceInfo mediaSourceInfo)
    {
        this.apiClient = apiClient;
        this.playbackStartInfo = playbackStartInfo;
        this.deviceProfile = deviceProfile;
        this.serverId = serverId;
        this.isOffline = isOffline;
        currentMediaSource = mediaSourceInfo;
        originalMaxBitrate = deviceProfile.getMaxStreamingBitrate();
        updateMediaSourceSubtitleUrls(mediaSourceInfo, activity.getTranscodingOffsetTicks());
    }

    public void seekTranscode(long newPositionTicks){

        changeStream(currentMediaSource, newPositionTicks, null, null, null);
    }

    public void setAudioStreamIndex(MediaPlayer vlc, PlaybackService mService, int index){

        logger.Info("setAudioStreamIndex %s", index);

        if (playbackStartInfo.getPlayMethod() == PlayMethod.Transcode){

            if (playbackStartInfo.getAudioStreamIndex() != null && index == playbackStartInfo.getAudioStreamIndex()) {
                return;
            }

            // If transcoding, we're going to need to stop and restart the stream

            long positionTicks = mService.getTimeTicks();
            changeStream(currentMediaSource, positionTicks, index, null, null);
        }
        else{

            // We need to figure out the index in vlc
            int trackNumber = 0;
            for (MediaStream stream : currentMediaSource.getMediaStreams()) {
                if (stream.getType() == MediaStreamType.Audio){
                    if (index == stream.getIndex()){
                        break;
                    }
                    trackNumber++;
                }
            }

            // Increment by one to account for the "Disabled" entry
            trackNumber++;

            MediaPlayer.TrackDescription[] vlcTracks = vlc.getAudioTracks();

            if (vlcTracks.length <= trackNumber) {
                logger.Error("Cannot set subtitle stream index because the track doesn't exist in the track list");
                return;
            }

            boolean success = vlc.setAudioTrack(vlcTracks[trackNumber].id);

            if (!success){
                logger.Error("Vlc returned an error when attempting to set the audioTrack");
            }

            playbackStartInfo.setAudioStreamIndex(index);
        }
    }

    public void setSubtitleStreamIndex(MediaPlayer vlc, PlaybackService mService, int index, boolean isInitialPlayback){

        logger.Info("setSubtitleStreamIndex %s", index);

        MediaStream stream = null;
        for (MediaStream current : currentMediaSource.getMediaStreams()){
            if (current.getType() == MediaStreamType.Subtitle && current.getIndex() == index){

                stream = current;
                break;
            }
        }

        // Disable subtitles
        if (index == -1){

            logger.Info("Disabling subtitles");

            if (stream != null && stream.getDeliveryMethod() == SubtitleDeliveryMethod.Encode) {

                // if the current subtitle stream is being burned in, we're going to have to change the transcoding stream
                long positionTicks = mService.getTimeTicks();
                changeStream(currentMediaSource, positionTicks, null, index, null);
                return;
            }

            // Just tell vlc to shut off subs
            vlc.setSpuTrack(index);
            activity.updateExternalSubtitles(null);
            playbackStartInfo.setSubtitleStreamIndex(index);
            return;
        }

        if (stream == null) {
            return;
        }

        if (!isInitialPlayback && playbackStartInfo.getSubtitleStreamIndex() != null && index == playbackStartInfo.getSubtitleStreamIndex()) {
            logger.Info("returning from setSubtitleStreamIndex because index is already set");
            return;
        }

        if (stream.getDeliveryMethod() == SubtitleDeliveryMethod.Embed) {
            activity.updateExternalSubtitles(null);
            enableEmbeddedSubtitleTrack(vlc, currentMediaSource, stream.getIndex());
        }
        else if (stream.getDeliveryMethod() == SubtitleDeliveryMethod.External) {

            enableManualExternalSubtitleTrack(vlc, stream);
        }
        else {

            if (isInitialPlayback && playbackStartInfo.getSubtitleStreamIndex() != null && index == playbackStartInfo.getSubtitleStreamIndex()) {
                logger.Info("returning from setSubtitleStreamIndex because index is already set");
                return;
            }

            // Subs have to be burned in
            activity.updateExternalSubtitles(null);
            long positionTicks = mService.getTimeTicks();
            changeStream(currentMediaSource, positionTicks, null, index, null);
        }
    }

    private void enableEmbeddedSubtitleTrack(MediaPlayer vlc, MediaSourceInfo mediaSourceInfo, int newIndex) {

        // We need to figure out the index in vlc
        int trackNumber = 0;
        for (MediaStream stream : mediaSourceInfo.getMediaStreams()) {
            if (stream.getType() == MediaStreamType.Subtitle && !stream.getIsExternal()){
                if (newIndex == stream.getIndex()){
                    break;
                }
                trackNumber++;
            }
        }

        logger.Info("enableEmbeddedSubtitleTrack. newIndex: %s, vlc track number: %s", newIndex, trackNumber);

        MediaPlayer.TrackDescription[] vlcTracks = vlc.getSpuTracks();

        if (vlcTracks == null) {
            logger.Error("Cannot set subtitle stream index because vlc.getSpuTracks returned null");
            return;
        }

        // Increment by one to account for the "Disabled" entry
        trackNumber++;

        if (vlcTracks.length <= trackNumber) {
            logger.Error("Cannot set subtitle stream index because the track doesn't exist in the track list");
            return;
        }

        int newTrackId = vlcTracks[trackNumber].id;

        logger.Info("vlc.setSpuTrack newTrackId: %s", newTrackId);

        boolean success = vlc.setSpuTrack(newTrackId);

        if (success){
            logger.Info("setSpuTrack succeeded");
        }
        else {
            logger.Error("Vlc returned an error when attempting to set the spuTrack");
        }

        playbackStartInfo.setSubtitleStreamIndex(newIndex);
    }

    private void enableManualExternalSubtitleTrack(final MediaPlayer vlc, final MediaStream stream) {

        // If vlc is currently displaying subtitles, stop it now
        vlc.setSpuTrack(-1);

        final String url = apiClient.GetApiUrl(stream.getDeliveryUrl()).replace("srt", "JSON");

        logger.Info("Downloading subtitles from %s", url);

        apiClient.getSubtitles(url, new Response<SubtitleTrackInfo>() {

            @Override
            public void onResponse(final SubtitleTrackInfo trackInfo) {

                logger.Info("Downloaded subtitles from %s. Sending subtitles to UI.", url);

                activity.updateExternalSubtitles(trackInfo);
                playbackStartInfo.setSubtitleStreamIndex(stream.getIndex());
            }

            @Override
            public void onError(Exception ex) {
                logger.ErrorException("Error downloading subtitles", ex);
            }
        });
    }

    public void setQuality(PlaybackService mService, int bitrate, int maxHeight) {

        preferencesProvider.set("preferredVideoBitrate", String.valueOf(bitrate));

        for (CodecProfile codecProfile : deviceProfile.getCodecProfiles()) {

            if (codecProfile.getType() == CodecType.Video) {
                for (ProfileCondition profileCondition : codecProfile.getConditions()) {
                    if (profileCondition.getProperty() == ProfileConditionValue.Height && profileCondition.getCondition() == ProfileConditionType.LessThanEqual) {
                        profileCondition.setValue(String.valueOf(maxHeight));
                    }
                }
            }
        }

        long positionTicks = mService.getTimeTicks();

        logger.Info("Changing quality to %s", bitrate);
        changeStream(currentMediaSource, positionTicks, null, null, bitrate);

        originalMaxBitrate = bitrate;
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
                    playbackStartInfo.setSubtitleStreamIndex(subtitleStreamIndex);
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

    private void setNewPlaybackInfo(String itemId, final MediaSourceInfo newMediaSource, final String previousPlaySessionId, final String newPlaySessionId, long positionTicks) {

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

        // Tell VideoPlayerActivity to changeStream
        activity.changeLocation(path, newMediaSource, playMethod, positionTicks);

        updateMediaSourceSubtitleUrls(newMediaSource, positionTicks);
        currentMediaSource = newMediaSource;
        playbackStartInfo.setPlayMethod(method);
        playbackStartInfo.setPlaySessionId(newPlaySessionId);

        // First stop transcoding
        apiClient.StopTranscodingProcesses(apiClient.getDeviceId(), previousPlaySessionId, new EmptyResponse());
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

        // Work around vlc 1080p stutter for now
        if (maxBitrate != null) {

            int maxHeight = 1080;

            for (CodecProfile codecProfile : deviceProfile.getCodecProfiles()) {

                if (codecProfile.getType() == CodecType.Video) {
                    for (ProfileCondition profileCondition : codecProfile.getConditions()) {
                        if (profileCondition.getProperty() == ProfileConditionValue.Height && profileCondition.getCondition() == ProfileConditionType.LessThanEqual) {
                            if (profileCondition.getValue() != null && profileCondition.getValue().length() > 0) {
                                maxHeight = Integer.parseInt(profileCondition.getValue());
                            }
                        }
                    }
                }
            }
        }

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
