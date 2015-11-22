package com.emby.mobile.api;

import android.content.Context;

import com.emby.mobile.webviews.IWebView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.QueryStringDictionary;
import mediabrowser.apiinteraction.android.AndroidConnectionManager;
import mediabrowser.apiinteraction.android.AndroidCredentialProvider;
import mediabrowser.apiinteraction.android.AndroidDevice;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.android.sync.MediaSyncAdapter;
import mediabrowser.apiinteraction.android.sync.PeriodicSync;
import mediabrowser.apiinteraction.android.sync.data.AndroidAssetManager;
import mediabrowser.apiinteraction.http.HttpRequest;
import mediabrowser.apiinteraction.sync.data.ILocalAssetManager;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.ClientCapabilities;
import mediabrowser.model.sync.LocalItem;

/**
 * Created by Luke on 5/28/2015.
 */
public class ApiClientBridge {

    private Context context;
    private ILogger logger;
    private IWebView webView;
    public VolleyHttpClient httpClient;
    private IJsonSerializer jsonSerializer;

    private ILocalAssetManager localAssetManager;

    public static ApiClientBridge Current;

    public ApiClientBridge(Context context, ILogger logger, IWebView webView, IJsonSerializer jsonSerializer) {
        this.context = context;
        this.logger = logger;
        this.webView = webView;
        this.jsonSerializer = jsonSerializer;

        MediaSyncAdapter.LoggerFactory = new SyncLoggerFactory(logger, context);

        localAssetManager = new AndroidAssetManager(context, logger, this.jsonSerializer);

        httpClient = new VolleyHttpClient(logger, context);
        Current = this;
    }

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void updateCredentials(String credentialsJson) {

        logger.Info("Received instruction to updateCredentials");

        // Save this prior to creating ConnectionManager, to fill in the client-side credentials
        AndroidCredentialProvider.Save(credentialsJson, context);
    }

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void init(String appName, String appVersion, String deviceId, String deviceName, String capabilitiesJson) {

        logger.Info("ApiClientBridge.init");

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

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public String getLocalMediaSource(String serverId, String itemId) {

        LocalItem item = localAssetManager.getLocalItem(serverId, itemId);

        if (item != null && item.getItem().getMediaSources().size() > 0) {

            MediaSourceInfo mediaSourceInfo = item.getItem().getMediaSources().get(0);

            boolean fileExists = localAssetManager.fileExists(mediaSourceInfo.getPath());

            if (fileExists) {
                return jsonSerializer.SerializeToString(mediaSourceInfo);
            }

        }

        return null;
    }

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void sendRequest(String requestJson, String dataType, final String callbackId) {

        HttpRequest request = jsonSerializer.DeserializeFromString(requestJson, HttpRequest.class);

        httpClient.Send(request, new HttpRequestResponse(jsonSerializer, webView, callbackId, logger));

    }

    @android.webkit.JavascriptInterface
    @org.xwalk.core.JavascriptInterface
    public void getDownloadSpeed(final long downloadBytes, final String address) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                getDownloadSpeedInternal(downloadBytes, address);
            }
        });

        thread.start();
    }

    private void getDownloadSpeedInternal(long downloadBytes, String address) {

        HttpURLConnection conn = null;

        try
        {
            logger.Info("Bitrate test url: %s", address);
            URL url = new URL(address);

            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true); // Allow Inputs
            conn.setUseCaches(false); // Don't use a Cached Copy
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Connection", "Keep-Alive");

            final long startTime = System.currentTimeMillis();

            InputStream inputStream = conn.getInputStream();

            try {

                byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
                int n;

                while ( (n = inputStream.read(byteChunk)) > 0 ) {

                }

                double time = System.currentTimeMillis() - startTime;
                double bitrate = downloadBytes;
                bitrate /= time;
                bitrate *= 1000;
                bitrate *= 8;

                long result = Math.round(bitrate);
                logger.Info("Detected download speed of %s", result);
                RespondToWebView(String.format("AndroidAjax.onDownloadSpeedResponse(%s);", result));
            }
            catch (IOException ioException){

                logger.ErrorException("Error performing download speed test", ioException);
                RespondToWebView("AndroidAjax.onDownloadSpeedResponse(0);");
                return;
            }
            finally {
                inputStream.close();
            }
        }
        catch (Exception ex)
        {
            logger.ErrorException("Error performing download speed test", ex);
            RespondToWebView("AndroidAjax.onDownloadSpeedResponse(0);");
        }
    }

    private void RespondToWebView(final String js) {

        //logger.Debug("Sending url to webView: %s", js);
        webView.sendJavaScript(js);
    }
}
