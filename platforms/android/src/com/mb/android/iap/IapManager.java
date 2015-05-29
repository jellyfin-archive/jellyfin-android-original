package com.mb.android.iap;

import com.mb.android.webviews.IWebView;

import org.xwalk.core.JavascriptInterface;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 5/27/2015.
 */
public class IapManager {

    private IWebView webView;
    private ILogger logger;

    public IapManager(IWebView webView, ILogger logger) {
        this.webView = webView;
        this.logger = logger;
    }

    @JavascriptInterface
    public boolean isStoreAvailable() {

        logger.Info("isStoreAvailable called");

        return true;
    }

    @JavascriptInterface
    public void isPurchased(final String id, final String callback) {

        logger.Info("isPurchased: %s", id);

        isPurchasedInternal(id, new Response<Boolean>(){

            @Override
            public void onResponse(final Boolean result){
                RespondToWebView(String.format("%s(\"%s\", %s)", callback, id, result));
            }
        });
    }

    private void RespondToWebView(final String url) {

        logger.Info("Sending url to webView: %s", url);
        webView.sendJavaScript(url);
    }

    @JavascriptInterface
    public void beginPurchase(String id) {

        logger.Info("beginPurchase: %s", id);

        beginPurchaseInternal(id, new EmptyResponse());
        // myWebView.loadUrl("javascript:window.MyHandler.setResult( addSomething("+val1+","+val2+") )");
    }

    private void isPurchasedInternal(String id, Response<Boolean> response) {
        response.onResponse(false);
    }

    private void beginPurchaseInternal(String id, EmptyResponse response) {

    }
}
