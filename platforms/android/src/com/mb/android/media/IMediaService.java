package com.mb.android.media;
import android.os.Parcelable;

/**
 * Created by Luke on 6/22/2015.
 */
public interface IMediaService {

    Parcelable getSessionToken();

    Class<?> getServiceClass();
}
