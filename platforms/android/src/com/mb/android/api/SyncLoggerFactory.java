package com.mb.android.api;

import mediabrowser.apiinteraction.android.sync.ISyncLoggerFactory;
import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 5/28/2015.
 */
public class SyncLoggerFactory implements ISyncLoggerFactory {

    private ILogger syncLogger;

    public SyncLoggerFactory(ILogger syncLogger) {
        this.syncLogger = syncLogger;
    }

    @Override
    public ILogger getNewLogger() {
        return syncLogger;
    }
}