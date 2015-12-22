package com.mb.android.webviews;

import android.annotation.TargetApi;
import android.content.Context;
import android.webkit.ConsoleMessage;

import com.mb.android.logging.AppLogger;

import org.apache.cordova.LOG;
import org.apache.cordova.engine.SystemWebChromeClient;
import org.apache.cordova.engine.SystemWebViewEngine;

/**
 * Created by Luke on 12/20/2015.
 */
public class MySystemWebChromeClient extends SystemWebChromeClient {

    private Context context;

    public MySystemWebChromeClient(SystemWebViewEngine parentEngine, Context context) {
        super(parentEngine);
        this.context = context;
    }

    @TargetApi(8)
    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage)
    {
        if (consoleMessage.message() != null) {

            AppLogger.getLogger(context).Info("%s: Line %d : %s", consoleMessage.sourceId(), consoleMessage.lineNumber(), consoleMessage.message());
        }

        return super.onConsoleMessage(consoleMessage);
    }
}
