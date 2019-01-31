package com.mb.android.api;

import android.content.Context;
import android.util.Log;

import com.mb.android.logging.AppLogger;

import mediabrowser.apiinteraction.android.AndroidCredentialProvider;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.ClientCapabilities;

public class ApiClientBridge {
    public VolleyHttpClient httpClient;

    public ApiClientBridge(Context context) {
        ILogger logger = AppLogger.getLogger(context);
        httpClient = new VolleyHttpClient(logger, context);
    }
}
