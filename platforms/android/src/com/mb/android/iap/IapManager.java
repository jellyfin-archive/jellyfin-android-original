package com.mb.android.iap;

import android.content.Context;

import com.mb.android.MainActivity;
import com.mb.android.webviews.IWebView;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.logging.ILogger;
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

    public final static String GOOGLE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk4MSP7wxlKaJwF066w7qQ+FvttXc+uSvUI5a+Lq+TT74Y1LTp0qg1+WRqou78WRK5cdfCr2m1N4LqttmYFfsWG/DBon98+ZFtaUbiP+Nx29YCkawE06hMyn0pONw/FnXB90mm0vGl7+fkpdYoUx1pit2DGoQweAZwmilW2jfPdi+YloSbX3SJlTXcgZIoAzIvY+qOinyuWIaRda5YcDfvson2yQC6XQOYqQ4ZOKhQxCSzaaQp3dLMCXlKPpsQNzFpVQsHLt4OntBMPkK3e/RxTE9AyhQYxofEzdKg/MHz1c3vCFIJCkzPy1cstwYMcjktRoLGgPHjxW60Iq9+USjfwIDAQAB";

    public IapManager(Context context, IWebView webView, ILogger logger) {
        this.webView = webView;
        this.logger = logger;
        this.context = context;
    }

    @android.webkit.JavascriptInterface
    public void getPurchaseInfos(final String callback) {

        logger.Info("getPurchaseInfos");

        final String unlockSku = InAppProduct.getCurrentUnlockSku(MainActivity.AppPackageName);
        final String monthlySubSky = InAppProduct.getCurrentMonthlySku(MainActivity.AppPackageName);

        isPurchasedInternal(unlockSku, new Response<Boolean>() {

            @Override
            public void onResponse(final Boolean result) {

                RespondToWebView(String.format("%s(\"%s\", %s, \"%s\")", callback, unlockSku, result, getUnlockPrice()));

                isPurchasedInternal(monthlySubSky, new Response<Boolean>() {

                    @Override
                    public void onResponse(final Boolean result) {

                        RespondToWebView(String.format("%s(\"%s\", %s, \"%s\")", callback, monthlySubSky, result, getMonthlyPrice()));
                    }
                });
            }
        });
    }

    private String getMonthlyPrice(){
        try {
            InAppProduct monthlyProduct = iabValidator.getPremiereMonthly();
            return monthlyProduct == null ? "" : monthlyProduct.getPrice();
        }
        catch ( Exception ex){
            return "";
        }
    }


    private String getUnlockPrice(){
        try {
            InAppProduct unlockProduct = iabValidator.getUnlockProduct();
            return unlockProduct == null ? "" : unlockProduct.getPrice();
        }
        catch ( Exception ex){
            return "";
        }
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

    @android.webkit.JavascriptInterface
    public String getUnlockProductSku() { return InAppProduct.getCurrentUnlockSku(MainActivity.AppPackageName); }

    @android.webkit.JavascriptInterface
    public String getPremiereMonthlySku() { return InAppProduct.getCurrentMonthlySku(MainActivity.AppPackageName); }

    public InAppProduct getPremiereMonthly() { return iabValidator.getPremiereMonthly(); }
    public InAppProduct getPremiereWeekly() { return iabValidator.getPremiereWeekly(); }
    public InAppProduct getUnlockProduct() { return iabValidator.getUnlockProduct(); }

    @android.webkit.JavascriptInterface
    public void initStore() {

        logger.Info("initStore called");
        init();
    }

    public boolean isStoreAvailable(){
        return storeReady;
    }

    private void init() {
        if (iabValidator == null || iabValidator.isDisposed()) iabValidator = new IabValidator(context, GOOGLE_KEY, new IapLogger(logger));
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
