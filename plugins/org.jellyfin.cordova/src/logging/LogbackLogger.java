package com.mb.android.logging;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import mediabrowser.model.logging.ILogger;
import mediabrowser.model.logging.LogSeverity;

/**
 * Created by Luke on 3/12/2015.
 */
public class LogbackLogger implements ILogger {

    private org.slf4j.Logger internalLogger;
    private String loggerName;
    private Marker marker;

    public LogbackLogger(Logger internalLogger, String loggerName) {
        this.internalLogger = internalLogger;
        this.loggerName = loggerName;
        this.marker = MarkerFactory.getMarker(loggerName);
    }

    @Override
    public void Info(String message, Object... paramList) {

        String msg = paramList == null || paramList.length == 0 ?
                message :
                String.format(message, paramList);

        internalLogger.info(marker, msg);
    }


    @Override
    public void Error(String message, Object... paramList) {

        String msg = paramList == null || paramList.length == 0 ?
                message :
                String.format(message, paramList);

        internalLogger.error(marker, msg);
    }


    @Override
    public void Warn(String message, Object... paramList) {

        String msg = paramList == null || paramList.length == 0 ?
                message :
                String.format(message, paramList);

        internalLogger.warn(marker, msg);
    }


    @Override
    public void Debug(String message, Object... paramList) {

        String msg = paramList == null || paramList.length == 0 ?
                message :
                String.format(message, paramList);

        internalLogger.debug(marker, msg);
    }


    @Override
    public void Fatal(String message, Object... paramList) {

        String msg = paramList == null || paramList.length == 0 ?
                message :
                String.format(message, paramList);

        internalLogger.error(marker, msg);
    }


    @Override
    public void FatalException(String message, Exception exception, Object... paramList) {

        String msg = paramList == null || paramList.length == 0 ?
                message :
                String.format(message, paramList);

        logException(msg, exception, LogSeverity.Fatal);
    }


    @Override
    public void ErrorException(String message, Exception exception, Object... paramList) {

        String msg = paramList == null || paramList.length == 0 ?
                message :
                String.format(message, paramList);

        logException(msg, exception, LogSeverity.Error);
    }

    private void logException(String message, Exception exception, LogSeverity severity) {

        try {
            message += "\r" + stackTraceToString(exception);
            if (exception.getCause() != null) {
                message += "caused by " + stackTraceToString(exception.getCause());
            }
        } catch (Exception e) {
            Error("FileLogger", "failed to parse exception");
        }

        internalLogger.error(marker, message);
    }

    private String stackTraceToString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName());
        sb.append("\r");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString());
            sb.append("\r");
        }
        return sb.toString();
    }
}