package com.mb.android.media;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils.TruncateAt;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.mb.android.R;

import org.videolan.libvlc.util.AndroidUtil;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Util {
    public final static String TAG = "VLC/Util";
    public static boolean close(Closeable closeable) {
        if (closeable != null)
            try {
                closeable.close();
                return true;
            } catch (IOException e) {}
        return false;
    }
    public static void removePositionInArray(Object[] array, int position, Object[] destArray) {
        int offset = 0, count = destArray.length;
        for (int i = 0; i<count; ++i) {
            if (i == position)
                ++offset;
            destArray[i] = array[i+offset];
        }
    }
    public static void addItemInArray(Object[] array, int position, Object item, Object[] destArray) {
        int offset = 0, count = destArray.length;
        for (int i = 0; i < count; ++i) {
            if (i == position) {
                ++offset;
                destArray[i] = item;
            } else
                destArray[i] = array[i-offset];
        }
    }
}