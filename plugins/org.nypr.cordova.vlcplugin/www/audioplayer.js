var exec = require("cordova/exec");

/**
 * This is a global variable called exposed by cordova
 */    
var AudioPlayer = function(){};

AudioPlayer.prototype.configure = function(success, error) {
  exec(success, error, "AudioPlayerPlugin", "init",[]);
};

AudioPlayer.prototype.getaudiostate = function(success, error) {
  exec(success, error, "AudioPlayerPlugin", "getaudiostate",[]);
};

AudioPlayer.prototype.playstream = function(success, error, stream, info, extra) {
  exec(success, error, "AudioPlayerPlugin", "playstream",[stream, info, extra]);
};

AudioPlayer.prototype.playfile = function(success, error, url, info, position, extra) {
  exec(success, error, "AudioPlayerPlugin", "playfile", [url, info, position, extra]);
};

AudioPlayer.prototype.pause = function(success, error) {
  exec(success, error, "AudioPlayerPlugin", "pause", []);
};

AudioPlayer.prototype.stop = function(success, error) {
  exec(success, error, "AudioPlayerPlugin", "stop", []);
};

AudioPlayer.prototype.setaudioinfo = function(success, error, json) {
  exec(success, error, "AudioPlayerPlugin", "setaudioinfo", json);
};

AudioPlayer.prototype.seek = function(success, error, interval) {
  exec(success, error, "AudioPlayerPlugin", "seek", [interval]);
};

AudioPlayer.prototype.seekto = function(success, error, position) {
  exec(success, error, "AudioPlayerPlugin", "seekto", [position]);
};

module.exports = new AudioPlayer();
