package com.mb.android.webviews;

import android.content.Context;
import android.util.AttributeSet;

import org.apache.cordova.engine.SystemWebChromeClient;
import org.apache.cordova.engine.SystemWebView;
import org.apache.cordova.engine.SystemWebViewEngine;

/**
 * Created by Luke on 12/20/2015.
 */
public class MySystemWebView extends SystemWebView {
    public MySystemWebView(Context context) {
        super(context);
    }

    public MySystemWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public SystemWebChromeClient CreateChromeClient(SystemWebViewEngine parentEngine){
        return new MySystemWebChromeClient(parentEngine, getContext());
    }
}
