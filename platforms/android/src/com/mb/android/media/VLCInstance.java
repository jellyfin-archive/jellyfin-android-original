package com.mb.android.media;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.util.VLCUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import mediabrowser.model.logging.ILogger;

public class VLCInstance {
    public final static String TAG = "VLC/UiTools/VLCInstance";

    private static LibVLC sLibVLC = null;

    /*private static Runnable sCopyLua = new Runnable() {
        @Override
        public void run() {
            String destinationFolder = AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY+
                    "/Android/data/"+VLCApplication.getAppContext().getPackageName()+"/lua";
            AssetManager am = VLCApplication.getAppResources().getAssets();
            FileUtils.copyAssetFolder(am, "lua", destinationFolder);
        }
    };
*/
    /** A set of utility functions for the VLC application */
    public synchronized static LibVLC get(final Context context, final ILogger logger) throws IllegalStateException {
        if (sLibVLC == null) {
            Thread.setDefaultUncaughtExceptionHandler(new VLCCrashHandler(logger));

            if(!VLCUtil.hasCompatibleCPU(context)) {
                Log.e(TAG, VLCUtil.getErrorMsg());
                throw new IllegalStateException("LibVLC initialisation failed: " + VLCUtil.getErrorMsg());
            }

            sLibVLC = new LibVLC(context, VLCOptions.getLibOptions(context));
            logger.Info("libVlc version %s", sLibVLC.version());
            LibVLC.setOnNativeCrashListener(new LibVLC.OnNativeCrashListener() {
                @Override
                public void onNativeCrash() {

                    logger.Error("libVlc native crash !");
                    //Intent i = new Intent(context, NativeCrashActivity.class);
                    //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    //context.startActivity(i);
                }
            });

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
            /*if (context instanceof Activity) {
                final Intent i = new Intent(context, CompatErrorActivity.class);
                context.startActivity(i);
            }*/
            return false;
        } else
            return true;
    }
}