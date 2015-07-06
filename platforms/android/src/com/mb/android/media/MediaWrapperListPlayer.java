package com.mb.android.media;

import java.util.ArrayList;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;


public class MediaWrapperListPlayer {

    private int mPlayerIndex = 0;
    final private LibVLC mLibVLC;
    final private MediaWrapperList mMediaList;

    private static MediaWrapperListPlayer sMediaWrapperListPlayer = null;

    public static synchronized MediaWrapperListPlayer getInstance(LibVLC libVLC) {
        if (sMediaWrapperListPlayer == null)
            sMediaWrapperListPlayer = new MediaWrapperListPlayer(libVLC);
        return sMediaWrapperListPlayer;
    }

    public MediaWrapperListPlayer(LibVLC libVLC) {
        mLibVLC = libVLC;
        mMediaList = new MediaWrapperList(libVLC);
    }

    public MediaWrapperList getMediaList() {
        return mMediaList;
    }

    /**
     * Play a media from the media list (playlist)
     *
     * @param position The index of the media
     * @param flags LibVLC.MEDIA_* flags
     */
    public void playIndex(int position, int flags) {
        String mrl = mMediaList.getMRL(position);
        if (mrl == null)
            return;
        final MediaWrapper media = mMediaList.getMedia(position);
        String[] options = mLibVLC.getMediaOptions(flags | (media != null ? media.getFlags() : 0));
        mPlayerIndex = position;
        mLibVLC.playMRL(mrl, options);
    }

    /**
     * Play a media from the media list (playlist)
     *
     * @param position The index of the media
     * @param paused start the media paused
     */
    public void playIndex(int position, boolean paused) {
        playIndex(position, paused ? LibVLC.MEDIA_PAUSED : 0);
    }

    public void playIndex(int position) {
        playIndex(position, 0);
    }

    /**
     * Expand and continue playing the current media.
     *
     * @return the index of the media was expanded, and -1 if no media was expanded
     */
    public int expandAndPlay() {
        int r = expand();
        if(r == 0)
            playIndex(mPlayerIndex);
        return r;
    }

    /**
     * Expand the current media.
     * @return the index of the media was expanded, and -1 if no media was expanded
     */
    public int expand() {
        ArrayList<String> children = new ArrayList<String>();
        int ret = mLibVLC.expandMedia(children);
        if(ret == 0) {
            mMediaList.remove(mPlayerIndex);
            for(String mrl : children) {
                final Media media = new Media(mLibVLC, mrl);
                media.parse(); // FIXME: parse should be done asynchronously
                media.release();
                mMediaList.insert(mPlayerIndex, new MediaWrapper(media));
            }
        }
        return ret;
    }

    public int expand(int index) {
        mPlayerIndex = index;
        return expand();
    }
}