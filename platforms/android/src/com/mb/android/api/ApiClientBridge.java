package com.mb.android.api;

import android.content.Context;
import android.os.Handler;

import com.mb.android.webviews.IWebView;

import org.xwalk.core.JavascriptInterface;

import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.AndroidConnectionManager;
import mediabrowser.apiinteraction.android.AndroidCredentialProvider;
import mediabrowser.apiinteraction.android.AndroidDevice;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.android.sync.MediaSyncAdapter;
import mediabrowser.apiinteraction.android.sync.PeriodicSync;
import mediabrowser.apiinteraction.android.sync.data.AndroidAssetManager;
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

    @JavascriptInterface
    public void updateCredentials(String credentialsJson) {

        logger.Info("Received instruction to updateCredentials");

        // Save this prior to creating ConnectionManager, to fill in the client-side credentials
        AndroidCredentialProvider.Save(credentialsJson, context);
    }

    @JavascriptInterface
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

    @JavascriptInterface
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

    @JavascriptInterface
    public void getImage(final String id, final String url) {

        Handler mainHandler = new Handler(context.getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                logger.Debug("Received request for %s", url);

                httpClient.getCachedFile(url, new Response<String>() {

                    @Override
                    public void onResponse(String path) {

                        if (path != null) {
                            RespondToWebView(String.format("$(ImageStore).trigger('imagepath%s', ['%s']);", id, path));
                        }
                    }

                });
            }
        };
        mainHandler.post(myRunnable);
    }

    private void RespondToWebView(final String js) {

        //logger.Info("Sending url to webView: %s", js);
        webView.sendJavaScript(js);
    }
}
