/*
 *  ConnectableDeviceWrapper.java
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

import java.util.HashMap;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.ConnectableDeviceListener;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.DeviceService.PairingType;
import com.connectsdk.service.command.ServiceCommandError;

class ConnectableDeviceWrapper implements ConnectableDeviceListener {
    ConnectSDKCordova plugin;
    String deviceId;
    ConnectableDevice device;
    CallbackContext callbackContext;
    HashMap<String, JSCommand> commands = new HashMap<String, JSCommand>();
    JSCommandDispatcher dispatcher;
    boolean active = false;

    public ConnectableDeviceWrapper(ConnectSDKCordova plugin, ConnectableDevice device) {
        this.plugin = plugin;
        this.device = device;
        this.deviceId = device.getId();
        this.dispatcher = new JSCommandDispatcher(this);
    }

    public void setCallbackContext(CallbackContext callbackContext) {
        if (this.callbackContext != null && this.callbackContext != callbackContext) {
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            this.callbackContext.sendPluginResult(result);
        }

        this.callbackContext = callbackContext;

        setActive(callbackContext != null);
    }

    // Active means that the wrapper is actively listening for events
    public void setActive(boolean activate) {
        if (!active && activate) {
            this.device.addListener(this);
            active = true;
        } else if (active && !activate) {
            this.device.removeListener(this);
            active = false;
        }
    }

    public JSONObject toJSONObject() {
        JSONObject obj = device.toJSONObject();

        try {
            obj.put("deviceId", deviceId);
            obj.put("capabilities", new JSONArray(device.getCapabilities()));
            obj.put("ready", device.isConnected()); // FIXME need actual ready state

            JSONArray services = new JSONArray();

            for (DeviceService service : device.getServices()) {
                String serviceName = service.getServiceName();

                if (serviceName == null) {
                    Log.e("ConnectSDK", "service is missing id: " + service.getClass().getName());
                    continue;
                }

                JSONObject serviceObj = new JSONObject();
                serviceObj.put("name", serviceName);
                services.put(serviceObj);
            }

            obj.put("services", services);
        } catch (JSONException e) {
        }

        return obj;
    }

    public void connect() {
        this.device.connect();
    }

    public void disconnect() {
        this.device.disconnect();
    }

    public void sendCommand(String commandId, String ifaceName, String methodName, JSONObject args, boolean subscribe, CallbackContext callbackContext) {
        JSCommand command = new JSCommand(this, commandId, subscribe, callbackContext);
        commands.put(commandId, command);

        Log.d("ConnectSDK", "dispatching command " + ifaceName + "." + methodName);
        this.dispatcher.dispatchCommand(ifaceName, methodName, command, args, subscribe);
    }

    public void cancelCommand(String commandId) {
        JSCommand command = commands.remove(commandId);

        if (command != null) {
            command.cancel();
        }
    }

    void sendEvent(String event, Object ...objs) {
        if (callbackContext != null) {
            plugin.sendEvent(callbackContext, event, objs);
        }
    }

    @Override
    public void onConnectionFailed(ConnectableDevice device, ServiceCommandError error) {
        plugin.sendErrorEvent(callbackContext, error);
    }

    @Override
    public void onDeviceReady(ConnectableDevice device) {
        sendEvent("ready");
    }

    @Override
    public void onDeviceDisconnected(ConnectableDevice device) {
        sendEvent("disconnect");
    }

    @Override
    public void onCapabilityUpdated(ConnectableDevice device, List<String> added, List<String> removed) {
        JSONObject obj = new JSONObject();

        try {
            obj.put("added", removed != null ? new JSONArray(added) : new JSONArray());
            obj.put("removed", removed != null ? new JSONArray(removed) : new JSONArray());
            obj.put("reset", false);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendEvent("capabilitieschanged", obj);
    }

    @Override
    public void onPairingRequired(ConnectableDevice device,
            DeviceService service, PairingType pairingType) {
        sendEvent("servicepairingrequired");
    }

    public void setPairingType(PairingType pairingType) {
        device.setPairingType(pairingType);
    }
}
