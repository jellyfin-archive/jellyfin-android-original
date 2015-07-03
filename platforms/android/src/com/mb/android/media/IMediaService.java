package com.mb.android.media;
import android.media.session.MediaSession;
import android.os.Parcelable;

/**
 * Created by Luke on 6/22/2015.
 */
public interface IMediaService {

    MediaSession.Token getSessionToken();

    Class<?> getServiceClass();
}
