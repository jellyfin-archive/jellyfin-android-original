package com.mb.android.iap;

import tv.emby.iap.ILogger;

/**
 * Created by Luke on 2/19/2016.
 */
public class IapLogger implements ILogger {

    private mediabrowser.model.logging.ILogger logger;

    public IapLogger(mediabrowser.model.logging.ILogger logger) {
        this.logger = logger;
    }

    public void d(String s, String s2, Object... objects) {
        logger.Debug(s2, objects);
    }

    @Override
    public void e(String s, String s2, Object... objects) {
        logger.Error(s2, objects);
    }

    @Override
    public void w(String s, String s2, Object... objects) {
        logger.Warn(s2, objects);
    }
}
