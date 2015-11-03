package com.emby.mobile.api;

import android.content.Context;

import com.emby.mobile.logging.AppLogger;

import mediabrowser.apiinteraction.android.sync.ISyncLoggerFactory;
import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 5/28/2015.
 */
public class SyncLoggerFactory implements ISyncLoggerFactory {

    private ILogger syncLogger;
    private Context context;

    public SyncLoggerFactory(ILogger syncLogger, Context context) {
        this.syncLogger = syncLogger;
        this.context = context;
    }

    @Override
    public ILogger getNewLogger() {
        AppLogger.ResetSyncLogger(context);
        return syncLogger;
    }

}