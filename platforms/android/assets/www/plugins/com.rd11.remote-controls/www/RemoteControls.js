cordova.define("com.rd11.remote-controls.RemoteControls", function(require, exports, module) { //
// RemoteControls.js
// Remote Control Cordova Plugin

// remoteControls created by Seth Hillinger on 2/21/14.
// Copyright 2013 Seth Hillinger. All rights reserved.

// updateMetas created by François LASSERRE on 12/05/13.
// Copyright 2013 François LASSERRE. All rights reserved.
// MIT Licensed
//


//------------------------------------------------------------------------------
// object that we're exporting
//------------------------------------------------------------------------------
var remoteControls = module.exports;

//params = [artist, title, album, cover, duration]
remoteControls.updateMetas = function(success, fail, params) {
    cordova.exec(success, fail, 'RemoteControls', 'updateMetas', params);
};

remoteControls.receiveRemoteEvent = function(event) {
    var ev = document.createEvent('HTMLEvents');
    ev.remoteEvent = event;
    ev.initEvent('remote-event', true, true, arguments);
    document.dispatchEvent(ev);
}
});
