/*
 *  DiscoveryManagerWrapper.java
 *  Connect SDK
 *
 *  Copyright (c) 2014 LG Electronics.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.connectsdk.cordova;

import java.util.ArrayList;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.CapabilityFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManager.PairingLevel;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.service.command.ServiceCommandError;

public class DiscoveryManagerWrapper implements DiscoveryManagerListener {
    ConnectSDKCordova plugin;
    DiscoveryManager discoveryManager;
    CallbackContext callbackContext;

    DiscoveryManagerWrapper(ConnectSDKCordova plugin, DiscoveryManager discoveryManager) {
        this.plugin = plugin;

        this.discoveryManager = discoveryManager;
        discoveryManager.addListener(this);
    }

    public void setCallbackContext(CallbackContext callbackContext) {
        if (this.callbackContext != null && this.callbackContext != callbackContext) {
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            this.callbackContext.sendPluginResult(result);
        }

        this.callbackContext = callbackContext;
    }

    public void start() {
        discoveryManager.start();
        sendEvent("startdiscovery", null);
    }

    public void stop() {
        discoveryManager.stop();
        sendEvent("stopdiscovery", null);
    }

    public void configure(JSONObject config) throws JSONException {
        if (config.has("pairingLevel")) {
            String pairingLevel = config.getString("pairingLevel");

            if ("off".equals(pairingLevel)) {
                discoveryManager.setPairingLevel(PairingLevel.OFF);
            } else if ("on".equals(pairingLevel)) {
                discoveryManager.setPairingLevel(PairingLevel.ON);
            }
        }

        if (config.has("capabilityFilters")) {
            JSONArray filters = config.getJSONArray("capabilityFilters");
            ArrayList<CapabilityFilter> capabilityFilters = new ArrayList<CapabilityFilter>();

            for (int i = 0; i < filters.length(); i++) {
                JSONArray filter = filters.getJSONArray(i);
                CapabilityFilter capabilityFilter = new CapabilityFilter();

                for (int j = 0; j < filter.length(); j++) {
                    capabilityFilter.addCapability(filter.getString(j));
                }

                capabilityFilters.add(capabilityFilter);
            }

            discoveryManager.setCapabilityFilters(capabilityFilters);
        }
    }

    @Override
    public void onDeviceAdded(DiscoveryManager manager, ConnectableDevice device) {
        //Log.d(ConnectSDKCordova.LOG_TAG, "sending devicefound event");
        sendDeviceEvent("devicefound", device);
    }

    @Override
    public void onDeviceUpdated(DiscoveryManager manager, ConnectableDevice device) {
        sendDeviceEvent("deviceupdated", device);
    }

    @Override
    public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {
        sendDeviceEvent("devicelost", device);
        plugin.removeDeviceWrapper(device);
    }

    @Override
    public void onDiscoveryFailed(DiscoveryManager manager, ServiceCommandError error) {
        if (callbackContext != null) {
            plugin.sendErrorEvent(callbackContext, error);
        }
    }

    public JSONObject getDeviceJSON(ConnectableDevice device) {
        ConnectableDeviceWrapper wrapper = plugin.getDeviceWrapper(device);
        return wrapper.toJSONObject();
    }

    public void sendDeviceEvent(String event, ConnectableDevice device) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("device", getDeviceJSON(device));
        } catch (JSONException e) {
        }

        sendEvent(event, obj);
    }

    public void sendEvent(String event, JSONObject obj) {
        if (callbackContext != null) {
            plugin.sendEvent(callbackContext, event, obj);
        }
    }
}
