package org.jellyfin.mobile.exoplayer;

import android.media.MediaCodecInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ExoPlayerCodec {

    private boolean valid;
    private boolean isAudio = false;
    private String mimeType;
    private String codec;
    private List<String> profiles;
    private List<Integer> levels;
    private Integer maxBitrate;

    public ExoPlayerCodec(MediaCodecInfo.CodecCapabilities codecCapabilities) {
        mimeType = codecCapabilities.getMimeType();
        codec = ExoPlayerFormats.getAudioCodec(mimeType);

        if (codec != null) {
            isAudio = valid = true;
            maxBitrate = codecCapabilities.getAudioCapabilities().getBitrateRange().getUpper();
        } else {
            codec = ExoPlayerFormats.getVideoCodec(mimeType);
            valid = codec != null;

            if (valid) {
                maxBitrate = codecCapabilities.getVideoCapabilities().getBitrateRange().getUpper();
            }
        }

        if (valid) {
            profiles = new ArrayList<>();
            levels = new ArrayList<>();
            processCodec(codecCapabilities);
        }
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isAudio() {
        return isAudio;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public List<Integer> getLevels() {
        return levels;
    }

    public Integer getMaxBitrate() {
        return maxBitrate;
    }

    public JSONObject getJSONObject() {
        JSONObject result = new JSONObject();
        JSONArray profiles = new JSONArray(this.profiles);
        JSONArray levels = new JSONArray(this.levels);

        try {
            result.put("mimeType", this.mimeType);
            result.put("codec", this.codec);
            result.put("isAudio", this.isAudio);
            result.put("profiles", profiles);
            result.put("levels", levels);
            result.put("maxBitrate", maxBitrate);
        } catch (JSONException e) {
            return null;
        }

        return result;
    }

    public void mergeCodec(ExoPlayerCodec codecToMerge) {
        for (String profile : codecToMerge.getProfiles()) {
            if (!this.profiles.contains(profile)) {
                this.profiles.add(profile);
            }
        }

        for (Integer level : codecToMerge.getLevels()) {
            if (!this.levels.contains(level)) {
                this.levels.add(level);
            }
        }
    }

    private void processCodec(MediaCodecInfo.CodecCapabilities codecCapabilities) {
        MediaCodecInfo.CodecProfileLevel[] profileLevels = codecCapabilities.profileLevels;

        for (MediaCodecInfo.CodecProfileLevel profileLevel : profileLevels) {
            String profile = null;
            String level = null;

            if (isAudio) {
                //TODO: profiles and levels
            } else {
                profile = ExoPlayerFormats.getVideoProfile(codec, profileLevel.profile);
                level = ExoPlayerFormats.getVideoLevel(codec, profileLevel.level);
            }

            if (profile != null && !profiles.contains(profile)) {
                profiles.add(profile);
            }

            if (level != null && !levels.contains(level)) {
                levels.add(Integer.valueOf(level));
            }
        }
    }
}
