package com.mb.android.logging;

import org.slf4j.Marker;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import mediabrowser.model.extensions.StringHelper;

/**
 * Created by Luke on 5/31/2015.
 */
public class LogFileFilter extends Filter<ILoggingEvent> {

    private boolean isSyncLogger;

    public LogFileFilter(boolean isSyncLogger) {
        this.isSyncLogger = isSyncLogger;
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {

        Marker marker = event.getMarker();

        if (marker == null){
            return FilterReply.ACCEPT;
        }

        if (StringHelper.EqualsIgnoreCase("SyncService", marker.getName())){
            return isSyncLogger ? FilterReply.ACCEPT : FilterReply.DENY;
        }

        return isSyncLogger ? FilterReply.DENY : FilterReply.ACCEPT;
    }
}
