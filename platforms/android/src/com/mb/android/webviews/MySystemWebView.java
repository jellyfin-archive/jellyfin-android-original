package com.mb.android.webviews;

import android.content.Context;
import android.util.AttributeSet;

import org.apache.cordova.engine.SystemWebChromeClient;
import org.apache.cordova.engine.SystemWebView;
import org.apache.cordova.engine.SystemWebViewEngine;

import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 12/20/2015.
 */
public class MySystemWebView extends SystemWebView {

    private ILogger logger;

    public MySystemWebView(Context context, ILogger logger) {
        super(context);
        this.logger = logger;
    }

    public MySystemWebView(Context context, AttributeSet attrs, ILogger logger) {
        super(context, attrs);
        this.logger = logger;
    }

    @Override
    public SystemWebChromeClient CreateChromeClient(SystemWebViewEngine parentEngine){
        return new MySystemWebChromeClient(parentEngine, logger);
    }
}
