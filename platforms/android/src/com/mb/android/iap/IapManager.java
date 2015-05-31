package com.mb.android.iap;

import android.content.Context;

import com.mb.android.webviews.IWebView;

import org.xwalk.core.JavascriptInterface;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.logging.ILogger;
import tv.emby.googleiap.ErrorSeverity;
import tv.emby.googleiap.ErrorType;
import tv.emby.googleiap.IResultHandler;
import tv.emby.googleiap.IabValidator;
import tv.emby.googleiap.ResultType;

/**
 * Created by Luke on 5/27/2015.
 */
public class IapManager {

    private IWebView webView;
    private ILogger logger;
    private Context context;

    public final static String GOOGLE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk4MSP7wxlKaJwF066w7qQ+FvttXc+uSvUI5a+Lq+TT74Y1LTp0qg1+WRqou78WRK5cdfCr2m1N4LqttmYFfsWG/DBon98+ZFtaUbiP+Nx29YCkawE06hMyn0pONw/FnXB90mm0vGl7+fkpdYoUx1pit2DGoQweAZwmilW2jfPdi+YloSbX3SJlTXcgZIoAzIvY+qOinyuWIaRda5YcDfvson2yQC6XQOYqQ4ZOKhQxCSzaaQp3dLMCXlKPpsQNzFpVQsHLt4OntBMPkK3e/RxTE9AyhQYxofEzdKg/MHz1c3vCFIJCkzPy1cstwYMcjktRoLGgPHjxW60Iq9+USjfwIDAQAB";

    public IapManager(Context context, IWebView webView, ILogger logger) {
        this.webView = webView;
        this.logger = logger;
        this.context = context;
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

    private void isPurchasedInternal(String id, final Response<Boolean> response) {
        final IabValidator iabValidator = new IabValidator(context, GOOGLE_KEY, new IResultHandler() {
            @Override
            public void handleResult(ResultType resultType) {
                response.onResponse(resultType.equals(ResultType.Success));
            }

            @Override
            public void handleError(ErrorSeverity errorSeverity, ErrorType errorType, String s) {
                //TODO handle error...
            }
        });

        iabValidator.checkInAppPurchase(id);

    }

    private void beginPurchaseInternal(String id, EmptyResponse response) {

    }
}
