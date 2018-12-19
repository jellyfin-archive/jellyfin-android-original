package com.mb.android.io;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.v4.provider.DocumentFile;

import java.io.File;

import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 5/29/2015.
 */
public class NativeFileSystem {

    private ILogger logger;
    private Context context;

    public NativeFileSystem(ILogger logger, Context context) {
        this.logger = logger;
        this.context = context;
    }

    @android.webkit.JavascriptInterface
    public boolean fileExists(String path) {

        logger.Info("Checking file exists: %s", path);
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                DocumentFile documentFile = DocumentFile.fromSingleUri(context, Uri.parse(path));
                if (documentFile != null && documentFile.exists()){
                    return false;
                }
            }

            return new File(path).exists();
            //return getFileLength(path) >= 0;
        }
        catch (Exception ex) {
            logger.ErrorException("Error determining if file exists", ex);
            return false;
        }
    }

    @android.webkit.JavascriptInterface
    public String translateFilePath(String path){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            DocumentFile documentFile = DocumentFile.fromSingleUri(context, Uri.parse(path));
            if (documentFile != null && documentFile.exists()){

                logger.Info("Converting DocumentUri path %s", path);
                String localPath = StorageAccessFrameworkHelper.getPath(context, documentFile.getUri());

                if (localPath != null){
                    path = localPath;
                }
            }
        }

        if (path.indexOf(":/") == -1){
            path = "file://"+path;
        }
        return path;
    }
}
