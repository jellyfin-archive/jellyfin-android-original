package com.mb.android.io;

import org.xwalk.core.JavascriptInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
            return new File(path).exists();
            //return getFileLength(path) >= 0;
        }
        catch (Exception ex) {
            logger.ErrorException("Error determining if file exists", ex);
            return false;
        }
    }

    @JavascriptInterface
    public String translateFilePath(String path) {

        return path;
    }
}
