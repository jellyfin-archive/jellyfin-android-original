package org.jellyfin.mobile.exoplayer;


import android.media.MediaFormat;

import java.util.ArrayList;
import java.util.HashMap;

public class ExoPlayerFormats {

    public static ArrayList<String> getAudioCodecs(String mimeType) {
        ArrayList<String> codecs = new ArrayList<String>();

        switch (mimeType) {
            case MediaFormat.MIMETYPE_AUDIO_AAC:
                codecs.add("aac");
             break;
            case MediaFormat.MIMETYPE_AUDIO_AC3:
                codecs.add("ac3");
                break;
            case MediaFormat.MIMETYPE_AUDIO_AMR_WB:
            case MediaFormat.MIMETYPE_AUDIO_AMR_NB:
                codecs.add("3gpp");
                break;
            case MediaFormat.MIMETYPE_AUDIO_EAC3:
                codecs.add("eac3");
                break;
            case MediaFormat.MIMETYPE_AUDIO_FLAC:
                codecs.add("flac");
                break;
            case MediaFormat.MIMETYPE_AUDIO_MPEG:
                codecs.add("mp3");
                codecs.add("mp2");
                break;
            case MediaFormat.MIMETYPE_AUDIO_OPUS:
                codecs.add("opus");
                break;
            case MediaFormat.MIMETYPE_AUDIO_RAW:
                codecs.add("raw");
                break;
            case MediaFormat.MIMETYPE_AUDIO_VORBIS:
                codecs.add("vorbis");
                break;
            case MediaFormat.MIMETYPE_AUDIO_QCELP:
            case MediaFormat.MIMETYPE_AUDIO_MSGSM:
            case MediaFormat.MIMETYPE_AUDIO_G711_MLAW:
            case MediaFormat.MIMETYPE_AUDIO_G711_ALAW:
            default:
                break;
        }

        return codecs;
    }

    public static ArrayList<String> getVideoCodecs(String mimeType) {
        ArrayList<String> codecs = new ArrayList<String>();

        switch (mimeType) {
            case MediaFormat.MIMETYPE_VIDEO_AVC:
                codecs.add("avc");
                break;
            case MediaFormat.MIMETYPE_VIDEO_H263:
                codecs.add("h263");
                break;
            case MediaFormat.MIMETYPE_VIDEO_HEVC:
            case MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION:
                codecs.add("hevc");
                codecs.add("h265");
                break;
            case MediaFormat.MIMETYPE_VIDEO_MPEG2:
                codecs.add("mpeg2video");
                break;
            case MediaFormat.MIMETYPE_VIDEO_MPEG4:
                codecs.add("h264");
                break;
            case MediaFormat.MIMETYPE_VIDEO_VP8:
                codecs.add("vp8");
                break;
            case MediaFormat.MIMETYPE_VIDEO_VP9:
                codecs.add("vp9");
                break;
            default:
                break;
        }

        return codecs;
    }
}
