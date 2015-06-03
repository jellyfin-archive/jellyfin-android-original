/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.mb.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import com.mb.android.api.ApiClientBridge;
import com.mb.android.iap.IapManager;
import com.mb.android.io.NativeFileSystem;
import com.mb.android.webviews.CrosswalkWebView;
import com.mb.android.webviews.IWebView;
import com.mb.android.webviews.NativeWebView;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaWebViewEngine;
import org.crosswalk.engine.XWalkCordovaView;
import org.xwalk.core.JavascriptInterface;

import java.net.URL;

import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.logging.ConsoleLogger;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;
import tv.emby.iap.ResultType;
import tv.emby.iap.UnlockActivity;

public class MainActivity extends CordovaActivity
{
    private final int PURCHASE_UNLOCK_REQUEST = 999;
    private ILogger logger;
    private IWebView webView;

    private ILogger getLogger(){
        if (logger == null){
            //logger = AppLogger.createLogger(this);
            logger = new ConsoleLogger();
        }

        return logger;
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        OkHttpClient okHttpClient = new OkHttpClient();

        try {
            URL.setURLStreamHandlerFactory(new OkUrlFactory(okHttpClient));
        }
        catch (Exception ex){
            // Occasionally seeing factory already set error
        }

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);
    }

    @Override
    protected CordovaWebViewEngine makeWebViewEngine() {

        CordovaWebViewEngine engine =  super.makeWebViewEngine();

        View engineView = engine.getView();
        ILogger logger = getLogger();
        IJsonSerializer jsonSerializer = new GsonJsonSerializer();

        webView = null;

        if (engineView instanceof WebView){

            WebView webkitView = (WebView)engine.getView();
            webView = new NativeWebView(webkitView);
        }
        else{

            XWalkCordovaView xView = (XWalkCordovaView)engine.getView();
            webView = new CrosswalkWebView(xView);
        }

        webView.addJavascriptInterface(new IapManager(getApplicationContext(), webView, logger), "NativeIapManager");
        webView.addJavascriptInterface(new ApiClientBridge(getApplicationContext(), logger, webView, jsonSerializer), "ApiClientBridge");
        webView.addJavascriptInterface(new NativeFileSystem(logger), "NativeFileSystem");
        webView.addJavascriptInterface(this, "MainActivity");

        return engine;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == PURCHASE_UNLOCK_REQUEST) {
            if (resultCode == ResultType.Success.ordinal()) {
                RespondToWebView(String.format("window.IapManager.onPurchaseComplete(true);"));
            } else {
                String data = intent.getStringExtra("data");
                RespondToWebView(String.format("window.IapManager.onPurchaseComplete(false);"));
            }
        }
    }

    @JavascriptInterface
    public void beginPurchase(String id) {

        try {
            Intent purchaseIntent = new Intent(this, UnlockActivity.class);
            purchaseIntent.putExtra("googleKey", IapManager.GOOGLE_KEY);
            purchaseIntent.putExtra("sku", "com.mb.android.unlock");
            startActivityForResult(purchaseIntent, PURCHASE_UNLOCK_REQUEST);
        }
        catch (Exception ex) {
            logger.ErrorException("Error launching activity", ex);
            RespondToWebView(String.format("window.IapManager.onPurchaseComplete(false);"));
        }
    }

    private void RespondToWebView(final String url) {

        logger.Info("Sending url to webView: %s", url);
        webView.sendJavaScript(url);
    }
}
