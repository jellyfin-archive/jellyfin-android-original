package com.mb.android.logging;

import mediabrowser.model.logging.ILogger;

public class AppLogger {
    public static ILogger logger;

    public static ILogger getLogger() {
        // TODO remove custom logging once api client is updated
        if (logger == null) logger = new LogbackLogger();
        return logger;
    }
}