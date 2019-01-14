function Plugin() {}

Plugin.prototype.show = function(message, duration, successCallback, errorCallback) {
  var options = {};
  options.message = message;
  options.duration = duration;
  cordova.exec(successCallback, errorCallback, 'Plugin', 'show', [options]);
}

var plugin = new Plugin();
module.exports = plugin;
