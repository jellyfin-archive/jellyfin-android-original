package com.mb.android.webviews;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;

import org.apache.cordova.engine.SystemWebChromeClient;
import org.apache.cordova.engine.SystemWebView;
import org.apache.cordova.engine.SystemWebViewClient;
import org.apache.cordova.engine.SystemWebViewEngine;

import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 12/20/2015.
 */
public class MySystemWebView extends SystemWebView {

    private ILogger logger;

    private final Activity activity;

    public MySystemWebView(Context context, ILogger logger, Activity activity) {
        super(context);
        this.logger = logger;
        this.activity = activity;
    }

    public MySystemWebView(Context context, AttributeSet attrs, ILogger logger, Activity activity) {
        super(context, attrs);
        this.logger = logger;
        this.activity = activity;
    }

    @Override
    public SystemWebChromeClient CreateChromeClient(SystemWebViewEngine parentEngine){
        return new MySystemWebChromeClient(parentEngine, logger, activity);
    }

    @Override
    public SystemWebViewClient CreateSystemWebViewClient(SystemWebViewEngine parentEngine){
        return new MySystemWebViewClient(parentEngine);
    }
}
