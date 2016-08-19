package com.mb.android;

import com.mb.android.media.MediaRes;

import mediabrowser.apiinteraction.android.mediabrowser.IMediaRes;

/**
 * Created by lukep on 8/18/2016.
 */
public class MediaSyncService extends mediabrowser.apiinteraction.android.sync.MediaSyncService {
    @Override
    protected IMediaRes GetMediaRes() {
        return new MediaRes();
    }

    @Override
    public Class getMainActivityClass() {
        return MainActivity.class;
    }
}
