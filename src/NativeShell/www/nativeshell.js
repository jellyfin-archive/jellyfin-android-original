function invokeMethod(method, options, successCallback, errorCallback) {
    successCallback = successCallback || function () {};
    errorCallback = errorCallback || function () {};
    options = options || [];

    cordova.exec(successCallback, errorCallback, 'NativeShell', method, options);
}

function NativeShell() {}

NativeShell.prototype.invokeMethod = invokeMethod;

NativeShell.prototype.getDeviceInformation = function(successCallback, errorCallback) {
    invokeMethod('getDeviceInformation', [], successCallback, errorCallback);
};

NativeShell.prototype.enableFullscreen = function(successCallback, errorCallback) {
    invokeMethod('enableFullscreen', [], successCallback, errorCallback);
};

NativeShell.prototype.disableFullscreen = function(successCallback, errorCallback) {
    invokeMethod('disableFullscreen', [], successCallback, errorCallback);
};

NativeShell.prototype.openUrl = function(url, target, successCallback, errorCallback) {
    invokeMethod('openIntent', [url], successCallback, errorCallback);
};

NativeShell.prototype.updateMediaSession = function(options, successCallback, errorCallback) {
    invokeMethod('updateMediaSession', [options], successCallback, errorCallback);
};

NativeShell.prototype.hideMediaSession = function(successCallback, errorCallback) {
    invokeMethod('hideMediaSession', [], successCallback, errorCallback);
};

NativeShell.prototype.downloadFile = function(options, successCallback, errorCallback) {
    invokeMethod('downloadFile', [options], successCallback, errorCallback);
};

NativeShell.prototype.getPlugins = function() {
    return [
        //'cordova/exoplayer',
        //'cordova/externalplayer'
    ];
};

module.exports = new NativeShell();
