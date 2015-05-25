cordova.define("com.emby.cordovaApiClient.CordovaApiClient", function(require, exports, module) { 
var exec = require('cordova/exec');

var nativeApiClient = {
    getDeviceProfile: function(success, failure) {
        return exec(success, failure, "CordovaApiClientPlugin", "profile");
    }
};

module.exports = nativeApiClient;

window.NativeApiClient = nativeApiClient;
});
