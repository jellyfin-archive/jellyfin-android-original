package com.mb.android.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.mb.android.SyncLoggerFactory;
import com.mb.android.WebViewResponder;

import java.net.URLEncoder;
import java.util.regex.Pattern;

import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.AndroidConnectionManager;
import mediabrowser.apiinteraction.android.AndroidCredentialProvider;
import mediabrowser.apiinteraction.android.AndroidDevice;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.android.sync.MediaSyncAdapter;
import mediabrowser.apiinteraction.android.sync.PeriodicSync;
import mediabrowser.apiinteraction.http.HttpRequest;
import mediabrowser.apiinteraction.http.IAsyncHttpClient;
import mediabrowser.logging.ConsoleLogger;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.net.HttpException;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.ClientCapabilities;

/**
 * Created by Luke on 5/28/2015.
 */
public class ApiClientBridge {

    private Context context;
    private ILogger logger;
    private WebView webView;
    private IAsyncHttpClient httpClient;
    private IJsonSerializer jsonSerializer;

    public ApiClientBridge(Context context, ILogger logger, WebView webView) {
        this.context = context;
        this.logger = logger;
        this.webView = webView;
    }

    @JavascriptInterface
    public void updateCredentials(String credentialsJson) {

        logger.Info("Received instruction to updateCredentials");

        // Save this prior to creating ConnectionManager, to fill in the client-side credentials
        AndroidCredentialProvider.Save(credentialsJson, context);
    }

    @JavascriptInterface
    public void init(String appName, String appVersion, String deviceId, String deviceName, String capabilitiesJson) {

        logger.Info("ApiClientBridge.init");

        ILogger logger = new ConsoleLogger();

        MediaSyncAdapter.LoggerFactory = new SyncLoggerFactory();

        jsonSerializer = new GsonJsonSerializer();

        httpClient = new VolleyHttpClient(logger, context);

        ApiEventListener apiEventListener = new ApiEventListener();

        ClientCapabilities capabilities = jsonSerializer.DeserializeFromString(capabilitiesJson, ClientCapabilities.class);
        capabilities.setSupportsContentUploading(true);

        // All we need to do is instantiate this to prepare data for the sync service
        IConnectionManager connectionManager = new AndroidConnectionManager(context,
                jsonSerializer,
                logger,
                httpClient,
                appName,
                appVersion,
                new AndroidDevice(context, deviceId, deviceName),
                capabilities,
                apiEventListener);

        new PeriodicSync(context).Create();
    }

    @JavascriptInterface
    public void sendRequest(String url, String method, String headers, final String callbackMethod, final String callbackId) {

        HttpRequest request = new HttpRequest();
        request.setUrl(url);
        request.setMethod(method);

        logger.Info("Parsing full header %s", headers);
        String[] headerValues = headers.split(Pattern.quote("|||||"));
        for (int i=0; i < headerValues.length; i++) {

            String headerValue = headerValues[i];

            int index = headerValue.indexOf("=");

            if (index == -1 || headerValue.length() < 2) {

                continue;
            }
            logger.Info("Parsing header %s", headerValue);
            String headerName = headerValue.substring(0, index);
            request.getRequestHeaders().put(headerName, headerValue.substring(index + 1));
        }

        httpClient.Send(request, new Response<String>(){

            @Override
            public void onResponse(String response){
                RespondToWebView(callbackMethod, callbackId, 200, response);
            }

            @Override
            public void onError(Exception ex){

                HttpException httpError = (HttpException)ex;
                RespondToWebView(callbackMethod, callbackId, httpError.getStatusCode(), "");
            }
        });

    }

    private void RespondToWebView(final String callbackMethod, final String callbackId, int status, String response) {

        RespondToWebView(String.format("%s(\"%s\", %s, \"%s\");", callbackMethod, callbackId, status, URLEncoder.encode(response)));
    }

    private void RespondToWebView(final String url) {

        logger.Info("Sending url to webView: %s", url);
        WebViewResponder.send(webView, url);
    }
}
