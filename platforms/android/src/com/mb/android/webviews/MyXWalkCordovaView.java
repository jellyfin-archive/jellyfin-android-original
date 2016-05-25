package com.mb.android.webviews;

import android.content.Context;
import android.util.AttributeSet;

import org.apache.cordova.CordovaPreferences;
import org.crosswalk.engine.XWalkCordovaResourceClient;
import org.crosswalk.engine.XWalkCordovaView;
import org.crosswalk.engine.XWalkWebViewEngine;

/**
 * Created by lukep on 5/25/2016.
 */
public class MyXWalkCordovaView extends XWalkCordovaView {
    public MyXWalkCordovaView(Context context, CordovaPreferences preferences) {
        super(context, preferences);
    }

    public MyXWalkCordovaView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public XWalkCordovaResourceClient CreateXWalkCordovaResourceClient(XWalkWebViewEngine parentEngine){
        return new MyXWalkCordovaResourceClient(parentEngine);
    }
}
