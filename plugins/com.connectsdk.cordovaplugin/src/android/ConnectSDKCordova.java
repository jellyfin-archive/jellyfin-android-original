/*
 *  ConnectSDKCordova.java
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
import java.util.LinkedHashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.SimpleDevicePicker;
import com.connectsdk.device.SimpleDevicePickerListener;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.command.ServiceCommandError;

public class ConnectSDKCordova extends CordovaPlugin {
	static final String LOG_TAG = "ConnectSDKCordova";
	
	DiscoveryManager discoveryManager;
	DiscoveryManagerWrapper discoveryManagerWrapper;
	LinkedHashMap<String, ConnectableDeviceWrapper> deviceWrapperById = new LinkedHashMap<String, ConnectableDeviceWrapper>();
	LinkedHashMap<ConnectableDevice, ConnectableDeviceWrapper> deviceWrapperByDevice = new LinkedHashMap<ConnectableDevice, ConnectableDeviceWrapper>();
	
	HashMap<String, JSObjectWrapper> objectWrappers = new HashMap<String, JSObjectWrapper>();
	
	class NoSuchDeviceException extends Exception {
		private static final long serialVersionUID = 1L;
	}
	
	synchronized ConnectableDeviceWrapper getDeviceWrapper(String deviceId) throws NoSuchDeviceException {
		ConnectableDeviceWrapper wrapper = deviceWrapperById.get(deviceId);
		
		if (wrapper == null) {
			throw new NoSuchDeviceException();
		}
		
		return wrapper;
	}
	
	synchronized ConnectableDeviceWrapper getDeviceWrapper(ConnectableDevice device) {
		ConnectableDeviceWrapper wrapper = deviceWrapperByDevice.get(device);
		
		if (wrapper == null) {
			wrapper = new ConnectableDeviceWrapper(this, device);
			deviceWrapperByDevice.put(device, wrapper);
			deviceWrapperById.put(wrapper.deviceId, wrapper);
		}
		
		return wrapper;
	}
	
	synchronized void removeDeviceWrapper(ConnectableDevice device) {
		ConnectableDeviceWrapper wrapper = deviceWrapperByDevice.get(device);
		
		if (wrapper != null) {
			deviceWrapperByDevice.remove(device);
			deviceWrapperById.remove(wrapper.deviceId);
		}
	}
	
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
	    try {

	    	if ("sendCommand".equals(action)) {
	    		ConnectableDeviceWrapper deviceWrapper = getDeviceWrapper(args.getString(0));
	    		
	    		String commandId = args.getString(1);
	    		String ifaceName = args.getString(2);
	    		String methodName = args.getString(3);
	    		JSONObject methodArgs = args.getJSONObject(4);
	    		boolean subscribe = args.getBoolean(5);
	    		deviceWrapper.sendCommand(commandId, ifaceName, methodName, methodArgs, subscribe, callbackContext);
	    		return true;
	    	} else if ("cancelCommand".equals(action)) {
	    		ConnectableDeviceWrapper deviceWrapper = getDeviceWrapper(args.getString(0));
	    		String commandId = args.getString(1);
	    		
	    		deviceWrapper.cancelCommand(commandId);
	    		callbackContext.success();
	    		
	    		return true;
	    	} else if ("startDiscovery".equals(action)) {
		        startDiscovery(args, callbackContext);
		        return true;
		    } else if ("stopDiscovery".equals(action)) {
		    	stopDiscovery(args, callbackContext);
		    	return true;
		    } else if ("setDiscoveryConfig".equals(action)) {
		    	setDiscoveryConfig(args, callbackContext);
		    	return true;
		    } else if ("pickDevice".equals(action)) {
		    	pickDevice(args, callbackContext);
		    	return true;
		    } else if ("setDeviceListener".equals(action)) {
		    	ConnectableDeviceWrapper deviceWrapper = getDeviceWrapper(args.getString(0));
		    	deviceWrapper.setCallbackContext(callbackContext);
		    	return true;
		    } else if ("connectDevice".equals(action)) {
		    	ConnectableDeviceWrapper deviceWrapper = getDeviceWrapper(args.getString(0));
		    	deviceWrapper.setCallbackContext(callbackContext);
		    	deviceWrapper.connect();
		    	return true;
		    } else if ("disconnectDevice".equals(action)) {
		    	ConnectableDeviceWrapper deviceWrapper = getDeviceWrapper(args.getString(0));
		    	deviceWrapper.disconnect();
		    	return true;
		    } else if ("acquireWrappedObject".equals(action)) {
		    	String objectId = args.getString(0);
		    	JSObjectWrapper wrapper = objectWrappers.get(objectId);
		    	
		    	if (wrapper != null) {
		    		wrapper.setCallbackContext(callbackContext);
		    	}
		    	
		    	return true;
		    } else if ("releaseWrappedObject".equals(action)) {
		    	String objectId = args.getString(0);
		    	JSObjectWrapper wrapper = objectWrappers.get(objectId);
		    	
		    	if (wrapper != null) {
		    		wrapper.setCallbackContext(null);
		    		removeObjectWrapper(wrapper);
		    	}
		    	
		    	return true;
		    }
	    } catch (NoSuchDeviceException e) {
	    	callbackContext.error("no such device");
	    	return true;
		} catch (JSONException e) {
			Log.d(LOG_TAG, "exception while handling " + action, e);
			callbackContext.error(e.toString());
			return true;
		}
	    
	    Log.w(LOG_TAG, "no handler for exec action " + action);
	    return false;
	}
	
	void initDiscoveryManagerWrapper() {
		if (discoveryManagerWrapper == null) {
			DiscoveryManager.init(cordova.getActivity().getApplicationContext());
			discoveryManager = DiscoveryManager.getInstance();
			discoveryManagerWrapper = new DiscoveryManagerWrapper(this, discoveryManager);
		}
	}
	
	void startDiscovery(JSONArray args, final CallbackContext callbackContext) throws JSONException {
		initDiscoveryManagerWrapper();
		
		if (args.length() > 0) {
			JSONObject config = args.optJSONObject(0);
			discoveryManagerWrapper.configure(config);
		}
		
		discoveryManagerWrapper.setCallbackContext(callbackContext);
		discoveryManagerWrapper.start();
	}
	
	void stopDiscovery(JSONArray args, final CallbackContext callbackContext) throws JSONException {
		if (discoveryManagerWrapper != null) {
			discoveryManagerWrapper.stop();
		}
		
		callbackContext.success();
	}
	
	void setDiscoveryConfig(JSONArray args, final CallbackContext callbackContext) throws JSONException {
		initDiscoveryManagerWrapper();
		discoveryManagerWrapper.configure(args.getJSONObject(0));
		
		callbackContext.success();
	}
	
	void pickDevice(JSONArray args, final CallbackContext callbackContext) throws JSONException {
		if (discoveryManager != null) {

			cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					SimpleDevicePicker picker = new SimpleDevicePicker(cordova.getActivity());
					
					picker.setListener(new SimpleDevicePickerListener() {
						@Override
						public void onPrepareDevice(ConnectableDevice device) {
						}
						
						@Override
						public void onPickDevice(ConnectableDevice device) {
							ConnectableDeviceWrapper wrapper = getDeviceWrapper(device);
							
							callbackContext.success(wrapper.toJSONObject());
						}

						@Override
						public void onPickDeviceFailed(boolean canceled) {
						}
					});
					picker.showPicker();
				}
			});
		} else {
			callbackContext.error("discovery not started");
		}
	}
	
	public void sendEvent(CallbackContext callbackContext, String event, Object ... objs) {
		if (callbackContext != null) {
			PluginResult result;
			
			JSONArray arr = new JSONArray();
			arr.put(event);
			
			for (Object obj : objs) {
				arr.put(obj);
			}
			
			result = new PluginResult(PluginResult.Status.OK, arr);
			result.setKeepCallback(true);
			callbackContext.sendPluginResult(result);
		}
	}
	
	public void sendErrorEvent(CallbackContext callbackContext, ServiceCommandError error) {
		if (callbackContext != null) {
			JSONObject errorObj = new JSONObject();
			
			try {
				errorObj.put("message", error.getMessage());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			sendEvent(callbackContext, "error", errorObj);
		}
	}
	
	public void addObjectWrapper(JSObjectWrapper wrapper) {
		objectWrappers.put(wrapper.objectId, wrapper);
	}
	
	public JSObjectWrapper getObjectWrapper(String objectId) {
		return objectWrappers.get(objectId);
	}
	
	public void removeObjectWrapper(JSObjectWrapper wrapper) {
		objectWrappers.remove(wrapper.objectId);
		wrapper.cleanup();
	}
}
