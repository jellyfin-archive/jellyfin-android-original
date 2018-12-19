package com.mb.android.api;

import android.content.Context;

import com.mb.android.webviews.IWebView;

import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.apiinteraction.IConnectionManager;
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

        MediaSyncAdapter.LoggerFactory = new SyncLoggerFactory(logger);

        localAssetManager = new AndroidAssetManager(context, logger, this.jsonSerializer);

        httpClient = new VolleyHttpClient(logger, context);
        Current = this;
    }

    @android.webkit.JavascriptInterface
    public void updateCredentials(String credentialsJson) {
        logger.Info("Received instruction to updateCredentials");

        // Save this prior to creating ConnectionManager, to fill in the client-side credentials
        AndroidCredentialProvider.Save(credentialsJson, context);
    }

    @android.webkit.JavascriptInterface
    public void init(String appName, String appVersion, String deviceId, String deviceName, String capabilitiesJson) {
        logger.Info("AppVersion: %s", appVersion);
        logger.Info("DeviceId: %s", deviceId);
        logger.Info("DeviceName: %s", deviceName);

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
    public String getLocalMediaSource(String serverId, String itemId) {
        LocalItem item = localAssetManager.getLocalItem(serverId, itemId);

        if (item == null){

            logger.Info("No local media source found for item id: %s", itemId);
            return null;
        }

        if (item.getItem().getMediaSources().size() > 0) {

            MediaSourceInfo mediaSourceInfo = item.getItem().getMediaSources().get(0);

            boolean fileExists = localAssetManager.fileExists(mediaSourceInfo.getPath());

            if (fileExists) {
                return jsonSerializer.SerializeToString(mediaSourceInfo);
            }

        }
        return null;
    }
}
