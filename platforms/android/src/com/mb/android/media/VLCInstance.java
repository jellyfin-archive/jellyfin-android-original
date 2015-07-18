package com.mb.android.media;


import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mb.android.R;
import com.mb.android.api.SyncLoggerFactory;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.LibVlcUtil;

import mediabrowser.model.logging.ILogger;

public class VLCInstance {
    public final static String TAG = "VLC/Util/VLCInstance";
    private static LibVLC sLibVLC = null;

    /** A set of utility functions for the VLC application */
    public synchronized static LibVLC get(Application app, Context context, ILogger logger) throws IllegalStateException {
        if (sLibVLC == null) {
            Thread.setDefaultUncaughtExceptionHandler(new VLCCrashHandler(logger));

            sLibVLC = new LibVLC();
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            VLCInstance.updateLibVlcSettings(pref, app);
            try {
                sLibVLC.init(context);
            } catch (LibVlcException e) {
                throw new IllegalStateException("LibVLC initialisation failed: " + LibVlcUtil.getErrorMsg());
            }
            LibVLC.setOnNativeCrashListener(new LibVLC.OnNativeCrashListener() {
                @Override
                public void onNativeCrash() {
                    /*Intent i = new Intent(context, NativeCrashActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.putExtra("PID", android.os.Process.myPid());
                    context.startActivity(i);*/
                }
            });
        }
        return sLibVLC;
    }

    public static synchronized void restart(Context context) throws IllegalStateException {
        if (sLibVLC != null) {
            try {
                sLibVLC.destroy();
                sLibVLC.init(context);
            } catch (LibVlcException lve) {
                throw new IllegalStateException("LibVLC initialisation failed: " + LibVlcUtil.getErrorMsg());
            }
        }
    }

    public static synchronized boolean testCompatibleCPU(Context context) {
        if (sLibVLC == null && !LibVlcUtil.hasCompatibleCPU(context)) {
            /*final Intent i = new Intent(context, CompatErrorActivity.class);
            context.startActivity(i);*/
            return false;
        } else
            return true;
    }

    public static synchronized void updateLibVlcSettings(SharedPreferences pref, Application app) {
        if (sLibVLC == null)
            return;

        boolean time_streching_default = app.getResources().getBoolean(R.bool.time_stretching_default);

        sLibVLC.setSubtitlesEncoding(pref.getString("subtitle_text_encoding", ""));
        sLibVLC.setTimeStretching(pref.getBoolean("enable_time_stretching_audio", time_streching_default));
        sLibVLC.setFrameSkip(pref.getBoolean("enable_frame_skip", false));
        sLibVLC.setChroma(pref.getString("chroma_format", ""));
        sLibVLC.setVerboseMode(pref.getBoolean("enable_verbose_mode", true));

        if (pref.getBoolean("equalizer_enabled", false))
            sLibVLC.setEqualizer(Preferences.getFloatArray(pref, "equalizer_values"));

        int aout;
        try {
            aout = Integer.parseInt(pref.getString("aout", "-1"));
        }
        catch (NumberFormatException nfe) {
            aout = -1;
        }
        int vout;
        try {
            vout = Integer.parseInt(pref.getString("vout", "-1"));
        }
        catch (NumberFormatException nfe) {
            vout = -1;
        }
        int deblocking;
        try {
            deblocking = Integer.parseInt(pref.getString("deblocking", "-1"));
        }
        catch(NumberFormatException nfe) {
            deblocking = -1;
        }
        int hardwareAcceleration;
        try {
            hardwareAcceleration = Integer.parseInt(pref.getString("hardware_acceleration", "-1"));
        }
        catch(NumberFormatException nfe) {
            hardwareAcceleration = -1;
        }
        int devHardwareDecoder;
        try {
            devHardwareDecoder = Integer.parseInt(pref.getString("dev_hardware_decoder", "-1"));
        }
        catch(NumberFormatException nfe) {
            devHardwareDecoder = -1;
        }
        int networkCaching = pref.getInt("network_caching_value", 0);
        if(networkCaching > 60000)
            networkCaching = 60000;
        else if(networkCaching < 0)
            networkCaching = 0;
        networkCaching = 7000;
        sLibVLC.setAout(aout);
        sLibVLC.setVout(vout);
        sLibVLC.setDeblocking(deblocking);
        sLibVLC.setNetworkCaching(networkCaching);
        sLibVLC.setHardwareAcceleration(LibVLC.HW_ACCELERATION_DISABLED);
        sLibVLC.setDevHardwareDecoder(devHardwareDecoder);
    }

    public static synchronized void setAudioHdmiEnabled(Context context, boolean enabled) {
        if (sLibVLC != null && sLibVLC.isHdmiAudioEnabled() != enabled) {
            sLibVLC.setHdmiAudioEnabled(enabled);
            restart(context);
        }
    }
}