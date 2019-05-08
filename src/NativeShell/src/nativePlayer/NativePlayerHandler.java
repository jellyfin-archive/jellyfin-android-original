package org.jellyfin.mobile.nativePlayer;

import com.google.gson.JsonArray;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NativePlayerHandler {
    private NativePlayer nativePlayer;

    public boolean handleRequest(String request, JSONArray args, CallbackContext callbackContext) {
        String methodName = request.split("|")[1];

        try {
            Method method = this.getClass().getMethod(methodName, JsonArray.class);

            try {
                return (boolean) method.invoke(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            callbackContext.error("method doesn't exist: " + methodName);
        }

        return false;
    }
}
