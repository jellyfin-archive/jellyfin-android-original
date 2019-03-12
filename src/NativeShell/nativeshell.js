function NativeShell() {}

NativeShell.prototype.getDeviceInformation = function(successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, 'NativeShell', 'getDeviceInformation', []);
}

NativeShell.prototype.enableFullscreen = function(successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, 'NativeShell', 'enableFullscreen', []);
}

NativeShell.prototype.disableFullscreen = function(successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, 'NativeShell', 'disableFulsceeen', []);
}

NativeShell.prototype.updateMediaSession = function(successCallback, errorCallback, options) {
  cordova.exec(successCallback, errorCallback, 'NativeShell', 'updateMediaSession', [options]);
}

NativeShell.prototype.hideMediaSession = function(successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, 'NativeShell', 'hideMediaSession', []);
}

NativeShell.prototype.openUrl = function (url, target) {
  cordova.InAppBrowser.open(url, target);
};

var nativeShell = new NativeShell();
module.exports = nativeShell;
