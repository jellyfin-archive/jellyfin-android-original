package com.mb.android.webviews;

import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;

import com.mb.android.MainActivity;

import org.apache.cordova.LOG;
import org.crosswalk.engine.XWalkCordovaResourceClient;
import org.crosswalk.engine.XWalkWebViewEngine;
import org.xwalk.core.XWalkView;

import mediabrowser.apiinteraction.Response;

/**
 * Created by lukep on 5/25/2016.
 */
public class MyXWalkCordovaResourceClient extends XWalkCordovaResourceClient {
    public MyXWalkCordovaResourceClient(XWalkWebViewEngine parentEngine) {
        super(parentEngine);
    }

    @Override
    public void onReceivedSslError(XWalkView view, final ValueCallback<Boolean> callback, SslError error) {
        MainActivity.Current.handleSslError(error, new SslResponse(callback));
    }

    public class SslResponse extends Response<Boolean>{

        private final ValueCallback<Boolean> handler;

        public SslResponse(ValueCallback<Boolean> handler) {
            this.handler = handler;
        }

        public void onResponse(Boolean result){
            if (result){
                handler.onReceiveValue(true);
            } else{
                handler.onReceiveValue(false);
            }
        }

        public void onError(Exception ex){
            handler.onReceiveValue(false);
        }
    }
}
