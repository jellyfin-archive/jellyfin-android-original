/*
 *  JSObjectWrapper.java
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

import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.core.JSONSerializable;

public class JSObjectWrapper implements JSONSerializable {
    ConnectSDKCordova plugin;
    CallbackContext callbackContext;

    static long nextObjectId = 0;
    public String objectId;

    public JSObjectWrapper(ConnectSDKCordova plugin) {
        this.plugin = plugin;
        this.objectId = "object_" + Long.toString(++nextObjectId);
    }

    public void cleanup() {
    }

    public void setCallbackContext(CallbackContext ctx) {
        this.callbackContext = ctx;
    }

    public void sendEvent(String event, Object ... objs) {
        plugin.sendEvent(callbackContext, event, objs);
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("objectId", objectId);
        return obj;
    }
}
