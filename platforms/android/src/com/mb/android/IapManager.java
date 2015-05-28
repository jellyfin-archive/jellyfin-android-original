package com.mb.android;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;

/**
 * Created by Luke on 5/27/2015.
 */
public class IapManager {

    private WebView webView;

    public IapManager(WebView webView) {
        this.webView = webView;
    }

    @JavascriptInterface
    public boolean isStoreAvailable() {

        return true;
    }

    @JavascriptInterface
    public void isPurchased(final String id, final String callback) {

        isPurchasedInternal(id, new Response<Boolean>(){

            @Override
            public void onResponse(final Boolean result){
                RespondToWebView(String.format("javascript:%s(\"%s\", %s)", callback, id, result));
            }
        });
    }

    private void RespondToWebView(final String url) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(url);
            }
        });
    }

    @JavascriptInterface
    public void beginPurchase(String id) {

        beginPurchaseInternal(id, new EmptyResponse());
        // myWebView.loadUrl("javascript:window.MyHandler.setResult( addSomething("+val1+","+val2+") )");
    }

    private void isPurchasedInternal(String id, Response<Boolean> response) {
        response.onResponse(false);
    }

    private void beginPurchaseInternal(String id, EmptyResponse response) {

    }
}
