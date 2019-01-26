package com.mb.android.api;

import android.content.Context;
import android.util.Log;

import com.mb.android.logging.AppLogger;

import mediabrowser.apiinteraction.android.AndroidCredentialProvider;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.ClientCapabilities;

/**
 * Created by Luke on 5/28/2015.
 */
public class ApiClientBridge {
    private Context context;
    private IJsonSerializer jsonSerializer;

    public VolleyHttpClient httpClient;

    public ApiClientBridge(Context context, IJsonSerializer jsonSerializer) {
        this.context = context;
        this.jsonSerializer = jsonSerializer;

        ILogger logger = AppLogger.getLogger(context);
        httpClient = new VolleyHttpClient(logger, context);
    }

    @android.webkit.JavascriptInterface
    public void updateCredentials(String credentialsJson) {
        Log.i(this.getClass().toString(), "Received instruction to updateCredentials");

        // save this prior to creating ConnectionManager to fill in the client credentials
        AndroidCredentialProvider.Save(credentialsJson, context);
    }

    @android.webkit.JavascriptInterface
    public void init(String appName, String appVersion, String deviceId, String deviceName, String capabilitiesJson) {
        ClientCapabilities capabilities = jsonSerializer.DeserializeFromString(capabilitiesJson, ClientCapabilities.class);
        capabilities.setSupportsContentUploading(true);
    }
}
