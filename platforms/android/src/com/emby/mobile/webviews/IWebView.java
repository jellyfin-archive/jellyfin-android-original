package com.emby.mobile.webviews;

/**
 * Created by Luke on 5/28/2015.
 */
public interface IWebView {
    void addJavascriptInterface(Object context, String name);
    void sendJavaScript(String javascript);
}
