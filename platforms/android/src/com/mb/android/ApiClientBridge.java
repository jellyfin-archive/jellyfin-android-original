package com.mb.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;

import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.android.AndroidConnectionManager;
import mediabrowser.apiinteraction.android.AndroidCredentialProvider;
import mediabrowser.apiinteraction.android.AndroidDevice;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.android.sync.MediaSyncAdapter;
import mediabrowser.apiinteraction.android.sync.PeriodicSync;
import mediabrowser.apiinteraction.http.IAsyncHttpClient;
import mediabrowser.logging.ConsoleLogger;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.ClientCapabilities;

/**
 * Created by Luke on 5/28/2015.
 */
public class ApiClientBridge {

    private Context context;

    public ApiClientBridge(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void updateCredentials(String credentialsJson) {

        // Save this prior to creating ConnectionManager, to fill in the client-side credentials
        AndroidCredentialProvider.Save(credentialsJson, context);
    }

    @JavascriptInterface
    public void init(String appName, String appVersion, String deviceId, String deviceName, String capabilitiesJson) {

        ILogger logger = new ConsoleLogger();

        MediaSyncAdapter.LoggerFactory = new SyncLoggerFactory();

        IJsonSerializer jsonSerializer = new GsonJsonSerializer();

        IAsyncHttpClient volleyHttpClient = new VolleyHttpClient(logger, context);

        ApiEventListener apiEventListener = new ApiEventListener();

        ClientCapabilities capabilities = jsonSerializer.DeserializeFromString(capabilitiesJson, ClientCapabilities.class);
        capabilities.setSupportsContentUploading(true);

        // All we need to do is instantiate this to prepare data for the sync service
        IConnectionManager connectionManager = new AndroidConnectionManager(context,
                jsonSerializer,
                logger,
                volleyHttpClient,
                appName,
                appVersion,
                new AndroidDevice(context, deviceId, deviceName),
                capabilities,
                apiEventListener);

        new PeriodicSync(context).Create();
    }
}
