package hu.dpal.phonegap.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

public class UniqueDeviceID extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        
        if (action.equals("get")) {
            
            Context context = cordova.getActivity().getApplicationContext();
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            String uuid;
            String androidID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
            String deviceID = tm.getDeviceId();
            String simID = tm.getSimSerialNumber();
            
            if ("9774d56d682e549c".equals(androidID) || androidID == null) {
                androidID = "";
            } 
            
            if (deviceID == null) {
                deviceID = "";
            }
            
            if (simID == null) { 
                simID = "";
            }   

            uuid = androidID + deviceID + simID;
            uuid = String.format("%32s", uuid).replace(' ', '0');
            uuid = uuid.substring(0,32);
    		uuid = uuid.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
            
            callbackContext.success(uuid);
            return true;
        }

        return false;
    }

}