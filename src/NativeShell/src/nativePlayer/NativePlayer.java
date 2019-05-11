package org.jellyfin.mobile.nativePlayer;

import android.app.Activity;
import android.content.Intent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NativePlayer {

    public boolean handleRequest(String methodName, JSONArray args, CallbackContext callbackContext, Activity activity) {
        try {
            Method method = this.getClass().getMethod(methodName, JSONArray.class, CallbackContext.class, Activity.class);

            try {
                return (boolean) method.invoke(this, args, callbackContext, activity);
            } catch (IllegalAccessException e) {
                callbackContext.error("can't access method: " + methodName);
            } catch (InvocationTargetException e) {
                callbackContext.error("failed to call method: " + methodName);
            }
        } catch (NoSuchMethodException e) {
            callbackContext.error("method doesn't exist: " + methodName);
        }

        callbackContext.error("unknown error for method: " + methodName);
        return false;
    }

    public boolean loadPlayer(JSONArray args, CallbackContext callbackContext, Activity activity) {

        Intent playerIntent = new Intent(activity.getApplicationContext(), NativePlayerActivity.class);

        activity.startActivity(playerIntent);

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        callbackContext.sendPluginResult(pluginResult);

        return true;
    }
}
