package com.mb.android.media;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;

import mediabrowser.model.logging.ILogger;

public class VLCCrashHandler implements UncaughtExceptionHandler {
    private static final String TAG = "VLC/VlcCrashHandler";
    private UncaughtExceptionHandler defaultUEH;
    private ILogger logger;

    public VLCCrashHandler(ILogger logger) {
        this.logger = logger;
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        // Inject some info about android version and the device, since google can't provide them in the developer console
        StackTraceElement[] trace = ex.getStackTrace();
        StackTraceElement[] trace2 = new StackTraceElement[trace.length+3];
        System.arraycopy(trace, 0, trace2, 0, trace.length);
        trace2[trace.length+0] = new StackTraceElement("Android", "MODEL", android.os.Build.MODEL, -1);
        trace2[trace.length+1] = new StackTraceElement("Android", "VERSION", android.os.Build.VERSION.RELEASE, -1);
        trace2[trace.length+2] = new StackTraceElement("Android", "FINGERPRINT", android.os.Build.FINGERPRINT, -1);
        ex.setStackTrace(trace2);
        ex.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();
        logger.Error(stacktrace);
        defaultUEH.uncaughtException(thread, ex);
    }
}