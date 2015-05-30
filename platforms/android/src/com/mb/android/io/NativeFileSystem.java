package com.mb.android.io;

import com.mb.android.smbstreamer.Streamer;

import org.xwalk.core.JavascriptInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 5/29/2015.
 */
public class NativeFileSystem {

    private ILogger logger;

    public NativeFileSystem(ILogger logger) {
        this.logger = logger;
    }

    @JavascriptInterface
    public boolean fileExists(String path) {

        File file = new File(path);

        if (file.exists()) {
            return true;
        }

        if (path.startsWith("\\\\")){
            // convert unc to smb
            path = path.replace("\\", "/");
            path = "smb:" + path;
        }

        try {
            SmbFile sFile = new SmbFile(path);

            if (sFile.exists()) {
                return true;
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            logger.ErrorException("MalformedURLException", e);
        }catch (SmbException e) {
            e.printStackTrace();
            logger.ErrorException("SmbException", e);
        }

        return false;
    }

    public long getFileLength(String path) throws IOException {

        File file = new File(path);

        if (file.exists()) {
            return file.length();
        }

        if (path.startsWith("\\\\")){
            // convert unc to smb
            path = path.replace("\\", "/");
            path = "smb:" + path;
        }

        SmbFile sFile = new SmbFile(path);

        if (sFile.exists()) {
            return sFile.length();
        }

        throw new FileNotFoundException(path);
    }

    public String getFileName(String path) throws IOException {

        File file = new File(path);

        if (file.exists()) {
            return file.getName();
        }

        if (path.startsWith("\\\\")){
            // convert unc to smb
            path = path.replace("\\", "/");
            path = "smb:" + path;
        }

        SmbFile sFile = new SmbFile(path);

        if (sFile.exists()) {
            return sFile.getName();
        }

        throw new FileNotFoundException(path);
    }

    public InputStream getFileInputStream(String path) throws IOException {

        File file = new File(path);

        if (file.exists()) {
            return new FileInputStream(path);
        }

        if (path.startsWith("\\\\")){
            // convert unc to smb
            path = path.replace("\\", "/");
            path = "smb:" + path;
        }

        return new SmbFileInputStream(path);
    }

    @JavascriptInterface
    public String translateFilePath(String path) {

        Streamer streamer = Streamer.getInstance(logger);

        return streamer.getUrl(path);
    }
}
