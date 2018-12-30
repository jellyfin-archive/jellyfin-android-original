package com.mb.android.media;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

import org.videolan.libvlc.util.AndroidUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;

public class FileUtils {

    /**
     * Size of the chunks that will be hashed in bytes (64 KB)
     */
    private static final int HASH_CHUNK_SIZE = 64 * 1024;

    public interface Callback {
        void onResult(boolean success);
    }

    public static String getFileNameFromPath(String path){
        if (path == null)
            return "";
        int index = path.lastIndexOf('/');
        if (index> -1)
            return path.substring(index+1);
        else
            return path;
    }

    public static String getParent(String path){
        if (TextUtils.equals("/", path))
            return path;
        String parentPath = path;
        if (parentPath.endsWith("/"))
            parentPath = parentPath.substring(0, parentPath.length()-1);
        int index = parentPath.lastIndexOf('/');
        if (index > 0){
            parentPath = parentPath.substring(0, index);
        } else if (index == 0)
            parentPath = "/";
        return parentPath;
    }

    // convert file:// uri from real path to emulated filesystem path
    public static Uri convertLocalUri(Uri uri) {
        if (!TextUtils.equals(uri.getScheme(), "file") || !uri.getPath().startsWith("/sdcard"))
            return uri;
        String path = uri.toString();
        return Uri.parse(path.replace("/sdcard", AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY));
    }

    public static String getPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            close(cursor);
        }
    }

    public static boolean copyAssetFolder(AssetManager assetManager, String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            if (files.length == 0)
                return false;
            new File(toPath).mkdirs();
            boolean res = true;
            for (String file : files)
                if (file.contains("."))
                    res &= copyAsset(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
                else
                    res &= copyAssetFolder(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean copyAsset(AssetManager assetManager, String fromAssetPath, String toPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            out.flush();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            close(in);
            close(out);
        }
    }

    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public static boolean copyFile(File src, File dst){
        boolean ret = true;
        if (src.isDirectory()) {
            File[] filesList = src.listFiles();
            dst.mkdirs();
            for (File file : filesList)
                ret &= copyFile(file, new File(dst, file.getName()));
        } else if (src.isFile()) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = new BufferedInputStream(new FileInputStream(src));
                out = new BufferedOutputStream(new FileOutputStream(dst));

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close(in);
                close(out);
            }
            return false;
        }
        return ret;
    }

    public static boolean canSave(MediaWrapper mw){
        if (mw == null || mw.getUri() == null)
            return false;
        String scheme = mw.getUri().getScheme();
        if (TextUtils.equals(scheme, "file"))
            return false;
        return TextUtils.equals(scheme, "smb")   ||
                TextUtils.equals(scheme, "nfs")  ||
                TextUtils.equals(scheme, "ftp")  ||
                TextUtils.equals(scheme, "ftps") ||
                TextUtils.equals(scheme, "sftp");
    }

    public static boolean canWrite(Context context, Uri uri){
        if (uri == null)
            return false;
        if (TextUtils.equals("file", uri.getScheme()))
            return canWrite(uri.toString());
        if (TextUtils.equals("content", uri.getScheme()))
            return canWrite(getPathFromURI(context, uri));
        return false;
    }

    public static boolean canWrite(String path){
        if (path == null)
            return false;
        if (path.startsWith("file://"))
            path = path.substring(7);
        if (!path.startsWith("/"))
            return false;
        if (path.startsWith(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY))
            return true;
        if (AndroidUtil.isKitKatOrLater())
            return false;
        File file = new File(path);
        return (file.exists() && file.canWrite());
    }

    public static String computeHash(File file) {
        long size = file.length();
        long chunkSizeForFile = Math.min(HASH_CHUNK_SIZE, size);
        long head = 0;
        long tail = 0;
        FileInputStream fis = null;
        FileChannel fileChannel = null;
        try {
            fis = new FileInputStream(file);
            fileChannel = fis.getChannel();
            head = computeHashForChunk(fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, chunkSizeForFile));

            //Alternate way to calculate tail hash for files over 4GB.
            ByteBuffer bb = ByteBuffer.allocateDirect((int)chunkSizeForFile);
            int read;
            long position = Math.max(size - HASH_CHUNK_SIZE, 0);
            while ((read = fileChannel.read(bb, position)) > 0) {
                position += read;
            }
            bb.flip();
            tail = computeHashForChunk(bb);

            return String.format("%016x", size + head + tail);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }finally {
            close(fileChannel);
            close(fis);
        }
    }

    public static boolean close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static long computeHashForChunk(ByteBuffer buffer) {
        LongBuffer longBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
        long hash = 0;
        while (longBuffer.hasRemaining())
            hash += longBuffer.get();
        return hash;
    }
}