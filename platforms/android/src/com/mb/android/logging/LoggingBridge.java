package com.mb.android.logging;

import org.xwalk.core.JavascriptInterface;

import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 6/30/2015.
 */
public class LoggingBridge {

    private ILogger logger;

    public LoggingBridge(ILogger logger) {
        this.logger = logger;
    }

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void log(String message) {

        logger.Info(message);
    }
}
