
exports.unhandledException = function (error) {
    console.log("FWChromecast ERROR: " + error);
};

/**
 * Scan the local wifi for chromecast devices. onDiscover get's called, when
 * devices where found.
 *
 * @param receiverAppId, the receiverAppId to filter devices
 */
exports.scanForDevices = function (receiverAppId) {
    var t = this;
    t.receiverAppId = receiverAppId;

    if (typeof (receiverAppId) == 'undefined') {
        return false;
    }

    // this method should only be called once.
    if (!t.startedListening) {
        t.startedListening = true;
        t.devices = {};
        cordova.exec(function (response) {
            response = JSON.parse(response);
            if (response.command == 'deviceDidComeOnline') {
                t.devices[response.data.id] = response.data;
                $(t).trigger('deviceDidComeOnline', response.data);
            } else {
                delete t.devices[response.data.id];
                $(t).trigger('deviceDidGoOffline', response.data);
            }
        }, t.unhandledException, "FWChromecast", "scanForDevices", [receiverAppId]);
    }

    return true;
};

/**
 * Select a device to use.
 *
 * @param device, the device identifier of the device to select.
 */
exports.selectDevice = function (device) {
    var t = this;

    if (typeof (device) == 'undefined') {
        return Promise.reject();
    }

    if (typeof (t.devices[device]) == 'undefined') {
        return Promise.reject();
    }

    return new Promise(function (resolve, reject) {

        cordova.exec(function (response) {
            response = JSON.parse(response);
            if (response.command == 'deviceConnected') {
                t.connected = t.devices[device];
                $(t).trigger('deviceConnected', t.connected);
                resolve(response.data);
            }

            if (response.command == 'failToConnect') {
                $(t).trigger('failToConnect', response.data);
                reject(response.data);
            }

            if (response.command == 'disconnectWithError') {
                $(t).trigger('disconnectWithError', response.data);
                reject(response.data);
            }
            if (response.command == 'applicationLaunched') {
                $(t).trigger('applicationLaunched', response.data);
            }

            if (response.command == 'failToConnectToApp') {
                $(t).trigger('failToConnectToApp', response.data);
            }

            if (response.command == 'receiveStatusForApp') {
                $(t).trigger('failToConnect', response.data);
            }

            if (response.command == 'receiveMessage') {
                $(t).trigger('receiveMessage', response.data);
                $(t).trigger('receiveMessage:' + response.data.channelName, response.data);
            }

        }, t.unhandledException, "FWChromecast", "selectDevice", [device]);
    });
};

/**
 * After selecting a device, we can finally start the application.
 *
 * @param receiverAppId, the receiverAppId of the app to start.
 */
exports.launchApplication = function () {
    var t = this;

    return new Promise(function (resolve, reject) {

        $(t).one("applicationLaunched", function (e, metadata) {
            resolve(metadata);
        });
        $(t).one("ffailToConnectToApp", function (e, error) {
            reject(error);
        });
        cordova.exec(undefined, t.unhandledException, "FWChromecast", "launchApplication", []);
    });
};

/**
 * After selecting a device, we can finally start the application.
 *
 * @param receiverAppId, the receiverAppId of the app to start.
 */
exports.joinApplication = function () {
    var t = this;

    return new Promise(function (resolve, reject) {

        $(t).one("applicationLaunched", function (e, metadata) {
            resolve(metadata);
        });
        $(t).one("ffailToConnectToApp", function (e, error) {
            reject(error);
        });
        cordova.exec(undefined, t.unhandledException, "FWChromecast", "joinApplication", []);
    });
};

/**
 * Disconnect from application.
 */
exports.disconnect = function () {
    var t = this;
    return new Promise(function (resolve, reject) {

        cordova.exec(undefined, t.unhandledException, "FWChromecast", "disconnect", []);
    });
};

/**
 * Start the default media channel.
 */
exports.startMediaChannel = function () {
    var t = this;
    return new Promise(function (resolve, reject) {

        cordova.exec(function (response) {
            response = JSON.parse(response);
            // TODO: Implement all media channel callback methods
            resolve();
        }, t.unhandledException, "FWChromecast", "startMediaChannel", []);
    });
};

/**
 * Load a media item on the default media channel.
 *
 * @param title, title to display
 * @param mediaUrl, absolute url to the media item
 * @param mediaType, mediaType (e.g. "video/mp4")
 * @param subtitle to display
 */
exports.loadMedia = function (title, mediaUrl, mediaType, subtitle) {
    cordova.exec(undefined, this.unhandledException, "FWChromecast", "loadMedia", [title, mediaUrl, mediaType, subtitle]);
    return this;
};

/**
 * Play the current media item on the default media channel.
 */
exports.playMedia = function () {
    cordova.exec(undefined, this.unhandledException, "FWChromecast", "playMedia", []);
    return this;
};

/**
 * Pause the current media item on the default media channel.
 */
exports.pauseMedia = function () {
    cordova.exec(undefined, this.unhandledException, "FWChromecast", "pauseMedia", []);
    return this;
};

/**
 * Stop the current media item on the default media channel.
 */
exports.stopMedia = function () {
    cordova.exec(undefined, this.unhandledException, "FWChromecast", "stopMedia", []);
    return this;
};

/**
 * Mute (or unmute) the current media item on the default media channel.
 *
 * @param mute, to mute or not to mute the media item
 */
exports.muteMedia = function (mute) {
    cordova.exec(undefined, this.unhandledException, "FWChromecast", "muteMedia", [mute]);
    return this;
};

/**
 * Seek the current media item on the default media channel to a time-position.
 *
 * @param seektime, the time to seek to
 */
exports.seekMedia = function (seektime) {
    cordova.exec(undefined, this.unhandledException, "FWChromecast", "seekMedia", [seektime]);
    return this;
};

/**
 * Set the volume for the current media item on the default media channel.
 *
 * @param volume, audo volume between 0 and 1 (e.g. 0.35)
 */
exports.setVolumeForMedia = function (volume) {
    cordova.exec(undefined, this.unhandledException, "FWChromecast", "setVolumeForMedia", [volume]);
    return this;
};
