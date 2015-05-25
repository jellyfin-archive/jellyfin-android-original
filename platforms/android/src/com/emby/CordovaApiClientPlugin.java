/**
 * An ApiClient plugin for Cordova / Phonegap
 */

import java.util.HashMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import profiles.AndroidProfile;
import profiles.AndroidProfileOptions;

public class CordovaApiClientPlugin extends CordovaPlugin {
    private static final String TAG = "CordovaApiClient";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {

        AndroidProfileOptions options = new AndroidProfileOptions();
        options.SupportsAc3 = false;
        options.DefaultH264Level = 41;
        options.SupportsHls = true;

        AndroidProfile profile = new AndroidProfile(options);

        JSONObject response = new JSONObject();

        response.put("profile", profile);
        callbackContext.success(response);

        return true;
    }
}
