package org.jellyfin.mobile.exoplayer;


import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ExoPlayerFormats {

    public static String getAudioCodec(String mimeType) {
        switch (mimeType) {
            case MediaFormat.MIMETYPE_AUDIO_AAC:
                return "aac";
            case MediaFormat.MIMETYPE_AUDIO_AC3:
                return "ac3";
            case MediaFormat.MIMETYPE_AUDIO_AMR_WB:
            case MediaFormat.MIMETYPE_AUDIO_AMR_NB:
                return "3gpp";
            case MediaFormat.MIMETYPE_AUDIO_EAC3:
                return "eac3";
            case MediaFormat.MIMETYPE_AUDIO_FLAC:
                return "flac";
            case MediaFormat.MIMETYPE_AUDIO_MPEG:
                return "mp3";
            case MediaFormat.MIMETYPE_AUDIO_OPUS:
                return  "opus";
            case MediaFormat.MIMETYPE_AUDIO_RAW:
                return "raw";
            case MediaFormat.MIMETYPE_AUDIO_VORBIS:
                return "vorbis";
            case MediaFormat.MIMETYPE_AUDIO_QCELP:
            case MediaFormat.MIMETYPE_AUDIO_MSGSM:
            case MediaFormat.MIMETYPE_AUDIO_G711_MLAW:
            case MediaFormat.MIMETYPE_AUDIO_G711_ALAW:
            default:
                return null;
        }
    }

    public static String getVideoCodec(String mimeType) {
        switch (mimeType) {
            case MediaFormat.MIMETYPE_VIDEO_AVC:
                return "avc";
            case MediaFormat.MIMETYPE_VIDEO_H263:
                return "h263";
            case MediaFormat.MIMETYPE_VIDEO_HEVC:
            case MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION:
                return "hevc";
            case MediaFormat.MIMETYPE_VIDEO_MPEG2:
                return "mpeg2video";
            case MediaFormat.MIMETYPE_VIDEO_MPEG4:
                return "h264";
            case MediaFormat.MIMETYPE_VIDEO_VP8:
                return "vp8";
            case MediaFormat.MIMETYPE_VIDEO_VP9:
                return "vp9";
            default:
                return null;
        }
    }

    public static ExoPlayerCodec getCodecCapabilities(MediaCodecInfo.CodecCapabilities codedCapabilities) {
        ExoPlayerCodec codec = new ExoPlayerCodec(codedCapabilities);
        return codec.isValid() ? codec : null;
    }

    public static String getVideoProfile(String codec, int profile) {
        switch (codec) {
            case "avc":
            case "h264":
                return getAVCProfile(profile);
            case "h263":
                return getH263Profile(profile);
            case "hevc":
                return getHEVCProfile(profile);
            case "mpeg2video":
                return getMpeg2videoProfile(profile);
            case "vp8":
                return getVP8Profile(profile);
            case "vp9":
                return getVP9Profile(profile);
            default:
                return null;
        }
    }

    public static String getVideoLevel(String codec, int level) {
        switch (codec) {
            case "avc":
            case "h264":
                return getAVCLevel(level);
            case "h263":
                return getH263Level(level);
            case "hevc":
                return getHEVCLevel(level);
            case "mpeg2video":
                return getMpeg2videoLevel(level);
            case "vp8":
                return getVP8Level(level);
            case "vp9":
                return getVP9Level(level);
            default:
                return null;
        }
    }

    private static String getVP9Level(int level) {
        switch (level) {
            case MediaCodecInfo.CodecProfileLevel.VP9Level1:
                return "1";
            case MediaCodecInfo.CodecProfileLevel.VP9Level11:
                return "11";
            case MediaCodecInfo.CodecProfileLevel.VP9Level2:
                return "2";
            case MediaCodecInfo.CodecProfileLevel.VP9Level21:
                return "21";
            case MediaCodecInfo.CodecProfileLevel.VP9Level3:
                return "3";
            case MediaCodecInfo.CodecProfileLevel.VP9Level31:
                return "31";
            case MediaCodecInfo.CodecProfileLevel.VP9Level4:
                return "4";
            case MediaCodecInfo.CodecProfileLevel.VP9Level41:
                return "41";
            case MediaCodecInfo.CodecProfileLevel.VP9Level5:
                return "5";
            case MediaCodecInfo.CodecProfileLevel.VP9Level51:
                return "51";
            case MediaCodecInfo.CodecProfileLevel.VP9Level52:
                return "52";
            case MediaCodecInfo.CodecProfileLevel.VP9Level6:
                return "6";
            case MediaCodecInfo.CodecProfileLevel.VP9Level61:
                return "61";
            case MediaCodecInfo.CodecProfileLevel.VP9Level62:
                return "62";
            default:
                return null;
        }
    }

    private static String getVP8Level(int level) {
        switch (level) {
            case MediaCodecInfo.CodecProfileLevel.VP8Level_Version0:
                return "0";
            case MediaCodecInfo.CodecProfileLevel.VP8Level_Version1:
                return "1";
            case MediaCodecInfo.CodecProfileLevel.VP8Level_Version2:
                return "2";
            case MediaCodecInfo.CodecProfileLevel.VP8Level_Version3:
                return "3";
            default:
                return null;
        }
    }

    private static String getMpeg2videoLevel(int level) {
        return null;
        //FIXME: server does not handle non numeric levels.
        /*switch (level) {
            case MediaCodecInfo.CodecProfileLevel.MPEG2LevelLL:
                return "ll";
            case MediaCodecInfo.CodecProfileLevel.MPEG2LevelML:
                return "ml";
            case MediaCodecInfo.CodecProfileLevel.MPEG2LevelH14:
                return "h14";
            case MediaCodecInfo.CodecProfileLevel.MPEG2LevelHL:
                return "hl";
            case MediaCodecInfo.CodecProfileLevel.MPEG2LevelHP:
                return "hp";
            default:
                return null;
        }*/
    }

    private static String getHEVCLevel(int level) {
        switch (level) {
            case MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel1:
            case MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel1:
                return "1";
            case MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel2:
            case MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel2:
                return "2";
            case MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel21:
            case MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel21:
                return "21";
            case MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel3:
            case MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel3:
                return "3";
            case MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel31:
            case MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel31:
                return "31";
            case MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel4:
            case MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel4:
                return "4";
            case MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel41:
            case MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel41:
                return "41";
            case MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel5:
            case MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel5:
                return "5";
            case MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel51:
            case MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel51:
                return "51";
            case MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel52:
            case MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel52:
                return "52";
            case MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel6:
            case MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel6:
                return "6";
            case MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel61:
            case MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel61:
                return "61";
            case MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel62:
            case MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel62:
                return "62";
            default:
                return null;
        }
    }

    private static String getH263Level(int level) {
        switch (level) {
            case MediaCodecInfo.CodecProfileLevel.H263Level10:
                return "10";
            case MediaCodecInfo.CodecProfileLevel.H263Level20:
                return "20";
            case MediaCodecInfo.CodecProfileLevel.H263Level30:
                return "30";
            case MediaCodecInfo.CodecProfileLevel.H263Level40:
                return "40";
            case MediaCodecInfo.CodecProfileLevel.H263Level45:
                return "45";
            case MediaCodecInfo.CodecProfileLevel.H263Level50:
                return "50";
            case MediaCodecInfo.CodecProfileLevel.H263Level60:
                return "60";
            case MediaCodecInfo.CodecProfileLevel.H263Level70:
                return "70";
            default:
                return null;
        }
    }

    private static String getAVCLevel(int level) {
        switch (level) {
            case MediaCodecInfo.CodecProfileLevel.AVCLevel1:
                return "1";
            /*case MediaCodecInfo.CodecProfileLevel.AVCLevel1b:
                return "1b";*/ //FIXME: server does not handle non numeric levels
            case MediaCodecInfo.CodecProfileLevel.AVCLevel11:
                return "11";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel12:
                return "12";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel13:
                return "13";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel2:
                return "2";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel21:
                return "21";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel22:
                return "22";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel3:
                return "3";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel31:
                return "31";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel32:
                return "32";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel4:
                return "4";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel41:
                return "41";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel42:
                return "42";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel5:
                return "5";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel51:
                return "51";
            case MediaCodecInfo.CodecProfileLevel.AVCLevel52:
                return "52";
            default:
                return null;
        }
    }

    private static String getVP9Profile(int profile) {
        switch (profile) {
            case MediaCodecInfo.CodecProfileLevel.VP9Profile0:
                return "0";
            case MediaCodecInfo.CodecProfileLevel.VP9Profile1:
                return "1";
            case MediaCodecInfo.CodecProfileLevel.VP9Profile2:
                return "2";
            case MediaCodecInfo.CodecProfileLevel.VP9Profile3:
                return "3";
            case MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR:
                return "2 hdr";
            case MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR:
                return "3 hdr";
            default:
                return null;
        }
    }

    private static String getVP8Profile(int profile) {
        if (profile == MediaCodecInfo.CodecProfileLevel.VP8ProfileMain) {
            return "main";
        }
        return null;
    }

    private static String getMpeg2videoProfile(int profile) {
        switch (profile) {
            case MediaCodecInfo.CodecProfileLevel.MPEG2ProfileSimple:
                return "simple";
            case MediaCodecInfo.CodecProfileLevel.MPEG2ProfileMain:
                return "main";
            case MediaCodecInfo.CodecProfileLevel.MPEG2Profile422:
                return "422";
            case MediaCodecInfo.CodecProfileLevel.MPEG2ProfileSNR:
                return "snr";
            case MediaCodecInfo.CodecProfileLevel.MPEG2ProfileSpatial:
                return "spatial";
            case MediaCodecInfo.CodecProfileLevel.MPEG2ProfileHigh:
                return "high";
            default:
                return null;
        }
    }

    private static String getHEVCProfile(int profile) {
        switch (profile) {
            case MediaCodecInfo.CodecProfileLevel.HEVCProfileMain:
                return "main";
            case MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10:
                return "main 10";
            case MediaCodecInfo.CodecProfileLevel.HEVCProfileMainStill:
                return "main still";
            case MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10:
                return "main 10 hdr 10";
            default:
                return null;
        }
    }

    private static String getH263Profile(int profile) {
        switch (profile) {
            case MediaCodecInfo.CodecProfileLevel.H263ProfileBaseline:
                return "baseline";
            case MediaCodecInfo.CodecProfileLevel.H263ProfileH320Coding:
                return "h320 coding";
            case MediaCodecInfo.CodecProfileLevel.H263ProfileBackwardCompatible:
                return "backward compatible";
            case MediaCodecInfo.CodecProfileLevel.H263ProfileISWV2:
                return "isw v2";
            case MediaCodecInfo.CodecProfileLevel.H263ProfileISWV3:
                return "isw v3";
            case MediaCodecInfo.CodecProfileLevel.H263ProfileHighCompression:
                return "high compression";
            case MediaCodecInfo.CodecProfileLevel.H263ProfileInternet:
                return "internet";
            case MediaCodecInfo.CodecProfileLevel.H263ProfileInterlace:
                return "interlace";
            case MediaCodecInfo.CodecProfileLevel.H263ProfileHighLatency:
                return "high latency";
            default:
                return null;
        }
    }

    public static String getAVCProfile(int profile) {
        switch (profile) {
            case MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline:
                return "baseline";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileMain:
                return "main";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileExtended:
                return "extended";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh:
                return "high";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10:
                return "high 10";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422:
                return "high 422";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444:
                return "high 444";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline:
                return "constrained baseline";
            case MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedHigh:
                return "constrained high";
            default:
                return null;
        }
    }
}
