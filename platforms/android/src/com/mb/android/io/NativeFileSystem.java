package com.mb.android.io;

import com.mb.android.smbstreamer.Streamer;

import org.xwalk.core.JavascriptInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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

        try {
            return getFileLength(path) >= 0;
        }
        catch (IOException ex) {
            return false;
        }
    }

    public long getFileLength(String path) throws IOException {

        try {

            File file = new File(path);

            if (file.exists()) {
                return file.length();
            }

        } catch (Exception  e) {
            logger.ErrorException("Exception", e);
        }

        try {
            SmbFile sFile = new SmbFile(path);

            if (sFile.exists()) {

                return sFile.length();
            }

        } catch (SmbException e) {
            logger.ErrorException("SmbException", e);
        }
        catch (IOException  e) {
            logger.ErrorException("IOException", e);
        }
        catch (NullPointerException ex){

            // Seeing an internal error in SmbFile when the path is not valid. Exists should have returned false
            logger.ErrorException("SmbException", ex);
        }

        throw new FileNotFoundException(path);
    }

    public String getFileName(String path) throws IOException {

        File file = new File(path);

        return file.getName();
    }

    public InputStream getFileInputStream(String path) throws IOException {

        File file = new File(path);

        if (file.exists()) {
            return new FileInputStream(path);
        }

        return new SmbFileInputStream(path);
    }

    @JavascriptInterface
    public String translateFilePath(String path) {

        Streamer streamer = Streamer.getInstance(logger);

        return streamer.getUrl(path);
    }
}
