package com.mb.android.iap;

import android.content.Context;

import com.mb.android.webviews.IWebView;

import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 5/27/2015.
 */
public class IapManager {

    private IWebView webView;
    private ILogger logger;
    private Context context;

    public IapManager(Context context, IWebView webView, ILogger logger) {
        this.webView = webView;
        this.logger = logger;
        this.context = context;
    }

    private void RespondToWebView(final String url) {
        logger.Info("Sending url to webView: %s", url);
        webView.sendJavaScript(url);
    }

    @android.webkit.JavascriptInterface
    public void getPurchaseInfos(final String callback) {
        logger.Info("getPurchaseInfos");
        RespondToWebView(String.format("%s(\"%s\", %s, \"%s\")", callback, "UNLOCK SKU", true, "UNLOCK PRICE"));
        RespondToWebView(String.format("%s(\"%s\", %s, \"%s\")", callback, "MONTHLY SKU", true, "MONTHLY PRICE"));
    }

    @android.webkit.JavascriptInterface
    public String getUnlockProductSku() {
        return "UNLOCK SKU";
    }

    @android.webkit.JavascriptInterface
    public String getPremiereMonthlySku() {
        return "MONTHLY SKU";
    }

    @android.webkit.JavascriptInterface
    public void initStore() {
        logger.Info("initStore called");
        RespondToWebView("IapManager.onStoreReady();");
    }
}
