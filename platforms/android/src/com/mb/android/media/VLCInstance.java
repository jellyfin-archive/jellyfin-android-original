package com.mb.android.media;

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

    public static void linkCompatLib(Context context) {
        final File outDir = new File(context.getFilesDir(), "compat");
        if (!outDir.exists())
            outDir.mkdir();
        final File outFile = new File(outDir, "libcompat.7.so");

        *//* The file may had been already copied from the asset, try to load it *//*
        if (outFile.exists()) {
            try {
                System.load(outFile.getPath());
                return;
            } catch (UnsatisfiedLinkError ule) {
                *//* the file can be invalid, try to copy it again *//*
            }
        }

        *//* copy libcompat.7.so from assert to a data dir *//*
        InputStream is = null;
        FileOutputStream fos = null;
        boolean success = false;
        try {
            is = VLCApplication.getAppResources().getAssets().open("libcompat.7.so");
            fos = new FileOutputStream(outFile);
            final byte[] buffer = new byte[16*1024];
            int read;
            while ((read = is.read(buffer)) != -1)
                fos.write(buffer, 0, read);
            success = true;
        } catch (IOException e) {
        } finally {
            Util.close(is);
            Util.close(fos);
        }

        *//* load the lib coming from the asset *//*
        if (success) {
            try {
                System.load(outFile.getPath());
            } catch (UnsatisfiedLinkError ule) {
            }
        }
    }*/

    /** A set of utility functions for the VLC application */
    public synchronized static LibVLC get(final Context context, ILogger logger) throws IllegalStateException {
        if (sLibVLC == null) {
            Thread.setDefaultUncaughtExceptionHandler(new VLCCrashHandler(logger));

            if(!VLCUtil.hasCompatibleCPU(context)) {
                logger.Error(VLCUtil.getErrorMsg());
                throw new IllegalStateException("LibVLC initialisation failed: " + VLCUtil.getErrorMsg());
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
                Log.w(TAG, "linking with true compat lib...");
                //linkCompatLib(context);
            }

            sLibVLC = new LibVLC(VLCOptions.getLibOptions(context));
            LibVLC.setOnNativeCrashListener(new LibVLC.OnNativeCrashListener() {
                @Override
                public void onNativeCrash() {
                    //Intent i = new Intent(context, NativeCrashActivity.class);
                    //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    //i.putExtra("PID", android.os.Process.myPid());
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
            sLibVLC = new LibVLC(VLCOptions.getLibOptions(context));
        }
    }

    public static synchronized boolean testCompatibleCPU(Context context) {
        if (sLibVLC == null && !VLCUtil.hasCompatibleCPU(context)) {
            //final Intent i = new Intent(context, CompatErrorActivity.class);
            //context.startActivity(i);
            return false;
        } else
            return true;
    }
}