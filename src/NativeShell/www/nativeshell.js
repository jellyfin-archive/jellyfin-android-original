function invokeMethod(successCallback, errorCallback, method, options) {
    successCallback = successCallback || function () {};
    errorCallback = errorCallback || function () {};
    options = options || [];

    cordova.exec(successCallback, errorCallback, 'NativeShell', method, options);
}

function NativeShell() {
    this.FileSystem = require('org.jellyfin.mobile.FileSystem');
    this.AppHost = require('org.jellyfin.mobile.AppHost');
}

NativeShell.prototype.getDeviceInformation = function(successCallback, errorCallback) {
    invokeMethod(successCallback, errorCallback, 'getDeviceInformation', []);
};

NativeShell.prototype.enableFullscreen = function(successCallback, errorCallback) {
    invokeMethod(successCallback, errorCallback, 'enableFullscreen', []);
};

NativeShell.prototype.disableFullscreen = function(successCallback, errorCallback) {
    invokeMethod(successCallback, errorCallback, 'disableFullscreen', []);
};

NativeShell.prototype.openUrl = function(successCallback, errorCallback, options) {
    invokeMethod(successCallback, errorCallback, 'openIntent', [options]);
};

NativeShell.prototype.updateMediaSession = function(options, successCallback, errorCallback) {
    invokeMethod(successCallback, errorCallback, 'updateMediaSession', [options]);
};

NativeShell.prototype.hideMediaSession = function(successCallback, errorCallback) {
    invokeMethod(successCallback, errorCallback, 'hideMediaSession', []);
};

NativeShell.prototype.downloadFile = function(url) {
    // TODO implement or remove
};

module.exports = new NativeShell();
