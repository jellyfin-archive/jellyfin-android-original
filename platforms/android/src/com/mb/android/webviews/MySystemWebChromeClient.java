package com.mb.android.webviews;

import android.annotation.TargetApi;
import android.content.Context;
import android.webkit.ConsoleMessage;

import com.mb.android.logging.AppLogger;

import org.apache.cordova.LOG;
import org.apache.cordova.engine.SystemWebChromeClient;
import org.apache.cordova.engine.SystemWebViewEngine;

import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 12/20/2015.
 */
public class MySystemWebChromeClient extends SystemWebChromeClient {

    private ILogger logger;
    private static final String LOG_TAG = "SystemWebChromeClient";

    public MySystemWebChromeClient(SystemWebViewEngine parentEngine, ILogger logger) {
        super(parentEngine);
        this.logger = logger;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onConsoleMessage(String message, int lineNumber, String sourceID)
    {
        //This is only for Android 2.1
        if(android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.ECLAIR_MR1)
        {
            LOG.d(LOG_TAG, "%s: Line %d : %s", sourceID, lineNumber, message);
            logger.Info("%s: Line %d : %s", sourceID, lineNumber, message);
            super.onConsoleMessage(message, lineNumber, sourceID);
        }
    }

    @TargetApi(8)
    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage)
    {
        if (consoleMessage.message() != null) {

            logger.Info("%s: Line %s : %s", consoleMessage.sourceId(), consoleMessage.lineNumber(), consoleMessage.message());
        }

        return super.onConsoleMessage(consoleMessage);
    }
}
