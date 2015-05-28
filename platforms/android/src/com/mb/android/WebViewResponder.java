package com.mb.android;

import android.os.Build;
import android.webkit.WebView;

/**
 * Created by Luke on 5/28/2015.
 */
public class WebViewResponder {

    public static void send(final WebView webView, final String url) {

        webView.post(new Runnable() {
            @Override
            public void run() {

                if (Build.VERSION.SDK_INT >= 19){
                    webView.evaluateJavascript(url, new EmptyStringCallback());
                }
                else{
                    final String jsUrl = "javascript:" + url;
                    webView.loadUrl(jsUrl);
                }
            }
        });
    }
}
