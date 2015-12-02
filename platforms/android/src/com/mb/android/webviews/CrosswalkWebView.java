package com.mb.android.webviews;

import com.mb.android.EmptyStringCallback;

import org.crosswalk.engine.XWalkCordovaView;

/**
 * Created by Luke on 5/28/2015.
 */
public class CrosswalkWebView implements IWebView {

    private XWalkCordovaView webView;

    public CrosswalkWebView(XWalkCordovaView view) {
        this.webView = view;
    }

    @Override
    public void addJavascriptInterface(Object context, String name) {

        webView.addJavascriptInterface(context, name);
    }

    @Override
    public void sendJavaScript(final String javascript) {

        webView.post(new Runnable() {
            @Override
            public void run() {

                webView.evaluateJavascript(javascript, new EmptyStringCallback());
            }
        });
    }
}
