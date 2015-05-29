package com.mb.android.io;

import org.xwalk.core.JavascriptInterface;

import java.io.File;
import java.net.MalformedURLException;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
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
}
