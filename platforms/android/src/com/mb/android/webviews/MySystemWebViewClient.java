package com.mb.android.webviews;

import android.annotation.TargetApi;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;

import org.apache.cordova.engine.SystemWebViewClient;
import org.apache.cordova.engine.SystemWebViewEngine;
import org.jellyfin.mobile.MainActivity;

import mediabrowser.apiinteraction.Response;

/**
 * Created by lukep on 5/25/2016.
 */
public class MySystemWebViewClient extends SystemWebViewClient {

    public MySystemWebViewClient(SystemWebViewEngine parentEngine) {
        super(parentEngine);
    }

    @TargetApi(8)
    @Override
    public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
        MainActivity.Current.handleSslError(error, new SslResponse(handler));
    }

    public class SslResponse extends Response<Boolean>{

        private final SslErrorHandler handler;

        public SslResponse(SslErrorHandler handler) {
            this.handler = handler;
        }

        public void onResponse(Boolean result){
            if (result){
                handler.proceed();
            } else{
                handler.cancel();
            }
        }

        public void onError(Exception ex){
            handler.cancel();
        }
    }
}
