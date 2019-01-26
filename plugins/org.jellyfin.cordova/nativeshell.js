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

NativeShell.prototype.updateMediaSession = function(successCallback, errorCallback, action, isLocalPlayer, itemId, title, artist, album, duration, position, imageUrl, canSeek, isPaused) {
  var options = {};
  options.action = action;
  options.isLocalPlayer = isLocalPlayer;
  options.itemId = itemId
  options.title = title
  options.artist = artist
  options.album = album
  options.duration = duration
  options.position = position
  options.imageUrl = imageUrl
  options.canSeek = canSeek
  options.isPaused = isPaused
  cordova.exec(successCallback, errorCallback, 'NativeShell', 'updateMediaSession', [options]);
}

NativeShell.prototype.hideMediaSession = function(successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, 'NativeShell', 'hideMediaSession', []);
}

var nativeShell = new NativeShell();
module.exports = nativeShell;
