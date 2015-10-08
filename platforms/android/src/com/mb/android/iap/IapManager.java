package com.mb.android.iap;

import android.content.Context;
import android.util.Log;

import com.mb.android.webviews.IWebView;

import org.xwalk.core.JavascriptInterface;

import java.util.List;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;
import tv.emby.iap.ErrorSeverity;
import tv.emby.iap.ErrorType;
import tv.emby.iap.IResultHandler;
import tv.emby.iap.IabValidator;
import tv.emby.iap.InAppProduct;
import tv.emby.iap.ResultType;

/**
 * Created by Luke on 5/27/2015.
 */
public class IapManager {

    private IWebView webView;
    private ILogger logger;
    private Context context;
    private IabValidator iabValidator;
    private boolean initialized;

    public final static String GOOGLE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk4MSP7wxlKaJwF066w7qQ+FvttXc+uSvUI5a+Lq+TT74Y1LTp0qg1+WRqou78WRK5cdfCr2m1N4LqttmYFfsWG/DBon98+ZFtaUbiP+Nx29YCkawE06hMyn0pONw/FnXB90mm0vGl7+fkpdYoUx1pit2DGoQweAZwmilW2jfPdi+YloSbX3SJlTXcgZIoAzIvY+qOinyuWIaRda5YcDfvson2yQC6XQOYqQ4ZOKhQxCSzaaQp3dLMCXlKPpsQNzFpVQsHLt4OntBMPkK3e/RxTE9AyhQYxofEzdKg/MHz1c3vCFIJCkzPy1cstwYMcjktRoLGgPHjxW60Iq9+USjfwIDAQAB";

    public IapManager(Context context, IWebView webView, ILogger logger) {
        this.webView = webView;
        this.logger = logger;
        this.context = context;
        init();
    }

    @JavascriptInterface
    public boolean isStoreAvailable() {

        logger.Info("isStoreAvailable called");

        return initialized;
    }

    @JavascriptInterface
    public void isPurchased(final String id, final String callback) {

        logger.Info("isPurchased: %s", id);

        final String[] idPart = id.split("\\|");

        isPurchasedInternal(idPart[0], new Response<Boolean>() {

            @Override
            public void onResponse(final Boolean result) {
                RespondToWebView(String.format("%s(\"%s\", %s)", callback, idPart[0], result));

                isPurchasedInternal(idPart[1], new Response<Boolean>() {

                    @Override
                    public void onResponse(final Boolean result) {
                        RespondToWebView(String.format("%s(\"%s\", %s)", callback, idPart[1], result));
                    }
                });
            }
        });
    }

    private void RespondToWebView(final String url) {

        logger.Info("Sending url to webView: %s", url);
        webView.sendJavaScript(url);
    }

    private void isPurchasedInternal(final String id, final Response<Boolean> response) {
        iabValidator.checkInAppPurchase(id, new IResultHandler<ResultType>() {
            @Override
            public void onResult(ResultType resultType) {
                iabValidator.dispose();
                logger.Info("*** IsPurchased Result: %s %s", id, resultType);
                response.onResponse(resultType.equals(ResultType.Success));
            }

            @Override
            public void onError(ErrorSeverity errorSeverity, ErrorType errorType, String s) {
                //TODO handle error...
                logger.Info("*** IsPurchased Error %s %s", id, s);
                iabValidator.dispose();
            }
        });

    }

    public InAppProduct getPremiereMonthly() { return iabValidator.getPremiereMonthly(); }
    public InAppProduct getPremiereWeekly() { return iabValidator.getPremiereWeekly(); }
    public InAppProduct getUnlockProduct() { return iabValidator.getUnlockProduct(); }

    private void init() {
        if (iabValidator == null || iabValidator.isDisposed()) iabValidator = new IabValidator(context, GOOGLE_KEY);
        iabValidator.validateProductsAsync(new IResultHandler<ResultType>() {
            @Override
            public void onResult(ResultType resultType) {
                initialized = true;
            }

            @Override
            public void onError(ErrorSeverity errorSeverity, ErrorType errorType, String s) {
                logger.Error("Error intializing IAP Manager. "+s);
                initialized = false;
            }
        });
    }
}
