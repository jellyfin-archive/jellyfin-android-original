package com.mb.android.media;

import android.content.Context;
import android.util.Log;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.util.VLCUtil;

import mediabrowser.model.logging.ILogger;

public class VLCInstance {
    public final static String TAG = "VLC/UiTools/VLCInstance";
    private static LibVLC sLibVLC = null;
    private static Runnable sCopyLua = new Runnable() {
        @Override
        public void run() {

        }
    };

    /** A set of utility functions for the VLC application */
    public synchronized static LibVLC get(final Context context, final ILogger logger) throws IllegalStateException {
        if (sLibVLC == null) {
            Thread.setDefaultUncaughtExceptionHandler(new VLCCrashHandler(logger));
            if(!VLCUtil.hasCompatibleCPU(context)) {
                Log.e(TAG, VLCUtil.getErrorMsg());
                throw new IllegalStateException("LibVLC initialisation failed: " + VLCUtil.getErrorMsg());
            }
            sLibVLC = new LibVLC(context, VLCOptions.getLibOptions(context));
            //VLCApplication.runBackground(sCopyLua);
        }
        return sLibVLC;
    }
    public static synchronized void restart(Context context) throws IllegalStateException {
        if (sLibVLC != null) {
            sLibVLC.release();
            sLibVLC = new LibVLC(context, VLCOptions.getLibOptions(context));
        }
    }
    public static synchronized boolean testCompatibleCPU(Context context) {
        if (sLibVLC == null && !VLCUtil.hasCompatibleCPU(context)) {
            return false;
        } else
            return true;
    }
}
