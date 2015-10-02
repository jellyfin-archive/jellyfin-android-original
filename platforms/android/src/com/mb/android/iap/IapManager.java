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
    private boolean operationInProgress;

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

        if (!confirmValidator()) {
            logger.Error("*** Attempt to call isPurchased with IAP operation already in progress");
            RespondToWebView(String.format("%s(\"%s\", %s)", callback, id, false));
        }

        logger.Info("isPurchased: %s", id);

        final String[] idPart = id.split("|");

        operationInProgress = true;
        isPurchasedInternal(idPart[0], new Response<Boolean>() {

            @Override
            public void onResponse(final Boolean result) {
                RespondToWebView(String.format("%s(\"%s\", %s)", callback, id, result));

                if (!confirmValidator()) {
                    logger.Error("*** Attempt to call isPurchased with IAP operation already in progress");
                    RespondToWebView(String.format("%s(\"%s\", %s)", callback, id, false));
                }

                isPurchasedInternal(idPart[1], new Response<Boolean>() {

                    @Override
                    public void onResponse(final Boolean result) {
                        RespondToWebView(String.format("%s(\"%s\", %s)", callback, id, result));
                    }
                });
            }
        });
    }

    private void RespondToWebView(final String url) {

        logger.Info("Sending url to webView: %s", url);
        webView.sendJavaScript(url);
    }

    private void isPurchasedInternal(String id, final Response<Boolean> response) {
        iabValidator.checkInAppPurchase(id, new IResultHandler<ResultType>() {
            @Override
            public void onResult(ResultType resultType) {
                iabValidator.dispose();
                operationInProgress = false;
                logger.Info("*** IsPurchased Result: %s", resultType);
                response.onResponse(resultType.equals(ResultType.Success));
            }

            @Override
            public void onError(ErrorSeverity errorSeverity, ErrorType errorType, String s) {
                //TODO handle error...
                logger.Info("*** IsPurchased Error %s", s);
                iabValidator.dispose();
                operationInProgress = false;
            }
        });

    }

    private boolean confirmValidator() {
        if (operationInProgress) return false;
        if (iabValidator == null || iabValidator.isDisposed()) iabValidator = new IabValidator(context, GOOGLE_KEY);
        return true;
    }

    public void getAvailableProducts(final IJsonSerializer jsonSerializer, final Response<List<InAppProduct>> handler) {
        logger.Info("*** Get products test call");
        if (!confirmValidator()) {
            // an iap operation is currently in progress - only can have one at a time respond appropriately
            logger.Error("*** Attempt to call get products with IAP operation already in progress");
        } else {
            iabValidator.getAvailableProductsAsync(new IResultHandler<List<InAppProduct>>() {
                @Override
                public void onResult(List<InAppProduct> inAppProducts) {
                    logger.Info("*** In App product result");
                    logger.Info(jsonSerializer.SerializeToString(inAppProducts));
                    iabValidator.dispose();
                    handler.onResponse(inAppProducts);
                }

                @Override
                public void onError(ErrorSeverity errorSeverity, ErrorType errorType, String s) {
                    logger.Error("*** Error getting products: " + s);
                    iabValidator.dispose();
                    handler.onError(new Exception());
                }
            });


        }

    }
}
