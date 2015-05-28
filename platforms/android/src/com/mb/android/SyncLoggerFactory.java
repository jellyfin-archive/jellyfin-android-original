package com.mb.android;

import mediabrowser.apiinteraction.android.sync.ISyncLoggerFactory;
import mediabrowser.logging.ConsoleLogger;
import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 5/28/2015.
 */
public class SyncLoggerFactory implements ISyncLoggerFactory {

    public SyncLoggerFactory() {
    }

    @Override
    public ILogger getNewLogger() {
        return new ConsoleLogger();
    }
}
