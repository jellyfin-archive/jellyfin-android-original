package com.emby.mobile.webviews;

import android.os.Build;
import android.webkit.WebView;

import com.emby.mobile.EmptyStringCallback;

/**
 * Created by Luke on 5/28/2015.
 */
public class NativeWebView implements  IWebView {

    private WebView webView;

    public NativeWebView(WebView webView) {
        this.webView = webView;
    }

    @Override
    public void addJavascriptInterface(Object context, String name) {
        webView.addJavascriptInterface(context, name);
    }

    @Override
    public void sendJavaScript(final String javascript) {

        if (webView == null){
            throw new IllegalArgumentException("webView");
        }

        if (javascript == null){
            throw new IllegalArgumentException("url");
        }

        webView.post(new Runnable() {
            @Override
            public void run() {

                if (Build.VERSION.SDK_INT >= 19){
                    webView.evaluateJavascript(javascript, new EmptyStringCallback());
                }
                else{
                    final String jsUrl = "javascript:" + javascript;
                    webView.loadUrl(jsUrl);
                }
            }
        });
    }
}
