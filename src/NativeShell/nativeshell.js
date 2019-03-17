function invokeMethod(successCallback, errorCallback, method, options) {
    successCallback = successCallback || function () {};
    errorCallback = errorCallback || function () {};
    options = options || [];

    cordova.exec(successCallback, errorCallback, 'NativeShell', method, options);
}

function NativeShell() {

    this.FileSystem = {
        fileExists: function (path) {
            return Promise.reject();
        },
        directoryExists: function (path) {
            return Promise.reject();
        }
    };
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

NativeShell.prototype.updateMediaSession = function(options, successCallback, errorCallback) {
    invokeMethod(successCallback, errorCallback, 'updateMediaSession', [options]);
};

NativeShell.prototype.hideMediaSession = function(successCallback, errorCallback) {
    invokeMethod(successCallback, errorCallback, 'hideMediaSession', []);
};

NativeShell.prototype.openUrl = function (url, target) {
    cordova.InAppBrowser.open(url, target || '_system');
};

NativeShell.prototype.sync = function(successCallback, errorCallback) {
    // TODO implement or remove
};

NativeShell.prototype.downloadFile = function(url) {
    // TODO implement or remove
};

module.exports = new NativeShell();
