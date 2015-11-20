package com.emby.mobile.iap;

import android.content.Context;
import android.util.Log;

import com.emby.mobile.webviews.IWebView;

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
    private boolean storeReady;

    public final static String GOOGLE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3hoOq1bO7V26Y3789jPWHOW1Fr5+bfHDHl/ICqITpKevnkoXKN009zuX7gkMLG3F2egosr7Xe9NIZ4U4UAA+O12eZzoytBPgpV3GIeDI65Agzhhoo2wDagiy42MXtjBMah/1iZrGSmO5mfuH1g/CZRilwAZUYvjvBU1VlszLaKp5tbnXVKTQyLCd3YPLVVf3LrluVytb7p9UlFmwyseBgUGIqaj/xBAIxxSW5KcSjfJy7woPp1HrWTSW2b4tiEagNIYh0/f9PSci+xo81RT6VNXTwiD0D7ZmbfH5ixmB6ZWyQXKSJsbl3IvPqN+e8mXEEmbxEawX3Kbmzq+zG7kPAQIDAQAB";
    private String appPackage = "com.emby.mobile";

    public IapManager(Context context, IWebView webView, ILogger logger) {
        this.webView = webView;
        this.logger = logger;
        this.context = context;
    }

    @JavascriptInterface
    public void getPurchaseInfos(final String callback) {

        logger.Info("getPurchaseInfos");

        final String unlockSku = InAppProduct.getCurrentUnlockSku(appPackage);
        final String monthlySubSky = InAppProduct.getCurrentMonthlySku(appPackage);

        isPurchasedInternal(unlockSku, new Response<Boolean>() {

            @Override
            public void onResponse(final Boolean result) {
                RespondToWebView(String.format("%s(\"%s\", %s)", callback, unlockSku, result));

                isPurchasedInternal(monthlySubSky, new Response<Boolean>() {

                    @Override
                    public void onResponse(final Boolean result) {
                        RespondToWebView(String.format("%s(\"%s\", %s)", callback, monthlySubSky, result));
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

        logger.Info("Checking purchase status of %s", id);
        iabValidator.checkInAppPurchase(id, new IResultHandler<ResultType>() {
            @Override
            public void onResult(ResultType resultType) {
                logger.Info("*** IsPurchased Result: %s %s", id, resultType);
                response.onResponse(resultType.equals(ResultType.Success));
            }

            @Override
            public void onError(ErrorSeverity errorSeverity, ErrorType errorType, String s) {
                //TODO handle error...
                logger.Info("*** IsPurchased Error %s %s", id, s);
            }
        });

    }

    @JavascriptInterface
    public String getUnlockProductSku() { return InAppProduct.getCurrentUnlockSku(appPackage); }

    @JavascriptInterface
    public String getPremiereMonthlySku() { return InAppProduct.getCurrentMonthlySku(appPackage); }

    public InAppProduct getPremiereMonthly() { return iabValidator.getPremiereMonthly(); }
    public InAppProduct getPremiereWeekly() { return iabValidator.getPremiereWeekly(); }
    public InAppProduct getUnlockProduct() { return iabValidator.getUnlockProduct(); }

    @JavascriptInterface
    public void initStore() {

        logger.Info("initStore called");
        init();
    }

    public boolean isStoreAvailable(){
        return storeReady;
    }

    private void init() {
        if (iabValidator == null || iabValidator.isDisposed()) iabValidator = new IabValidator(context, GOOGLE_KEY);
        iabValidator.validateProductsAsync(new IResultHandler<ResultType>() {
            @Override
            public void onResult(ResultType resultType) {

                boolean wasReady = storeReady;
                storeReady = true;

                if (!wasReady){
                    RespondToWebView("IapManager.onStoreReady();");
                }
            }

            @Override
            public void onError(ErrorSeverity errorSeverity, ErrorType errorType, String s) {
                logger.Error("Error intializing IAP Manager. "+s);
            }
        });
    }
}
