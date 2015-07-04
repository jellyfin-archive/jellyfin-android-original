package com.mb.android.media;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import org.videolan.libvlc.LibVlcUtil;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.telephony.TelephonyManager;

public class AndroidDevices {
    public final static String TAG = "VLC/Util/AndroidDevices";

    final static boolean hasNavBar;

    static {
        HashSet<String> devicesWithoutNavBar = new HashSet<String>();
        devicesWithoutNavBar.add("HTC One V");
        devicesWithoutNavBar.add("HTC One S");
        devicesWithoutNavBar.add("HTC One X");
        devicesWithoutNavBar.add("HTC One XL");
        hasNavBar = LibVlcUtil.isICSOrLater()
                && !devicesWithoutNavBar.contains(android.os.Build.MODEL);
    }

    public static boolean hasExternalStorage() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static boolean hasNavBar()
    {
        return hasNavBar;
    }

    /** hasCombBar test if device has Combined Bar : only for tablet with Honeycomb or ICS */
    public static boolean hasCombBar(Context context) {
        return (!AndroidDevices.isPhone(context)
                && ((VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) &&
                (VERSION.SDK_INT <= VERSION_CODES.JELLY_BEAN)));
    }

    public static boolean isPhone(Context context){
        TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        if(manager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE){
            return false;
        }else{
            return true;
        }
    }

    public static String[] getStorageDirectories() {
        String[] dirs = null;
        BufferedReader bufReader = null;
        ArrayList<String> list = new ArrayList<String>();
        list.add(Environment.getExternalStorageDirectory().getPath());

        List<String> typeWL = Arrays.asList("vfat", "exfat", "sdcardfs", "fuse");
        List<String> typeBL = Arrays.asList("tmpfs");
        String[] mountWL = { "/mnt", "/Removable" };
        String[] mountBL = {
                "/mnt/secure",
                "/mnt/shell",
                "/mnt/asec",
                "/mnt/obb",
                "/mnt/media_rw/extSdCard",
                "/mnt/media_rw/sdcard",
                "/storage/emulated" };
        String[] deviceWL = {
                "/dev/block/vold",
                "/dev/fuse",
                "/mnt/media_rw/extSdCard" };

        try {
            bufReader = new BufferedReader(new FileReader("/proc/mounts"));
            String line;
            while((line = bufReader.readLine()) != null) {

                StringTokenizer tokens = new StringTokenizer(line, " ");
                String device = tokens.nextToken();
                String mountpoint = tokens.nextToken();
                String type = tokens.nextToken();

                // skip if already in list or if type/mountpoint is blacklisted
                if (list.contains(mountpoint) || typeBL.contains(type) || Strings.StartsWith(mountBL, mountpoint))
                    continue;

                // check that device is in whitelist, and either type or mountpoint is in a whitelist
                if (Strings.StartsWith(deviceWL, device) && (typeWL.contains(type) || Strings.StartsWith(mountWL, mountpoint)))
                    list.add(mountpoint);
            }

            dirs = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                dirs[i] = list.get(i);
            }
        }
        catch (FileNotFoundException e) {}
        catch (IOException e) {}
        finally {
            if (bufReader != null) {
                try {
                    bufReader.close();
                }
                catch (IOException e) {}
            }
        }
        return dirs;
    }

}