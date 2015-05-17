/*
 *  WebAppSessionWrapper.java
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

import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.service.sessions.WebAppSession;

public class WebAppSessionWrapper extends JSObjectWrapper {
	WebAppSession session;

	public WebAppSessionWrapper(ConnectSDKCordova plugin, WebAppSession session) {
		super(plugin);
		this.session = session;
	}
	
	@Override
	public JSONObject toJSONObject() throws JSONException {
		JSONObject obj = new JSONObject(); // FIXME serialize session
		obj.put("objectId", objectId);
		
		if (session.launchSession != null) {
			obj.put("launchSession", session.launchSession.toJSONObject());
		}
		
		return obj;
	}
	
	@Override
	public void cleanup() {
		session.setWebAppSessionListener(null);
		session = null;
		
		super.cleanup();
	}
}
