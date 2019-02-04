package org.jellyfin.mobile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;

import org.apache.cordova.engine.SystemWebViewClient;
import org.apache.cordova.engine.SystemWebViewEngine;

public class CustomWebViewClient extends SystemWebViewClient {
    public Context context;

    public CustomWebViewClient(Context context, SystemWebViewEngine systemWebViewEngine) {
        super(systemWebViewEngine);
        this.context = context;
    }

    @Override
    public void onReceivedSslError(final WebView webView, final SslErrorHandler sslErrorHandler, final SslError sslError) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Untrusted Certificate")
                .setMessage("The certificate presented while connecting does not appear to be valid. Connect anyway?")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        sslErrorHandler.proceed();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        sslErrorHandler.cancel();
                        webView.clearSslPreferences();
                    }
                });
    }
}
