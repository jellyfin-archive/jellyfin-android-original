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

import android.os.Bundle;
import android.webkit.WebView;

import org.apache.cordova.*;

import mediabrowser.logging.ConsoleLogger;
import mediabrowser.model.logging.ILogger;

public class MainActivity extends CordovaActivity
{
    private ILogger logger;

    private ILogger getLogger(){
        if (logger == null){
            logger = new ConsoleLogger();
        }

        return logger;
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);
    }

    @Override
    protected CordovaWebViewEngine makeWebViewEngine() {

        CordovaWebViewEngine engine =  super.makeWebViewEngine();

        WebView webView = (WebView)engine.getView();

        ILogger logger = getLogger();

        webView.addJavascriptInterface(new IapManager(webView, logger), "NativeIapManager");
        webView.addJavascriptInterface(new ApiClientBridge(getApplicationContext(), logger), "ApiClientBridge");

        return engine;
    }
}
