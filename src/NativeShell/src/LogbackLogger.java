package org.jellyfin.mobile;

import mediabrowser.model.logging.ILogger;

public class LogbackLogger implements ILogger {

    public LogbackLogger() {
    }

    @Override
    public void Info(String message, Object... paramList) {
    }

    @Override
    public void Error(String message, Object... paramList) {
    }

    @Override
    public void Warn(String message, Object... paramList) {
    }

    @Override
    public void Debug(String message, Object... paramList) {
    }

    @Override
    public void Fatal(String message, Object... paramList) {
    }

    @Override
    public void FatalException(String message, Exception exception, Object... paramList) {
    }

    @Override
    public void ErrorException(String message, Exception exception, Object... paramList) {
    }
}
