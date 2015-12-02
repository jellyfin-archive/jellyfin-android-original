cordova.define("com.connectsdk.cordovaplugin.ConnectSDK.js", function(require, exports, module) { /*
 *  ConnectSDK.js
 *  Connect SDK
 *
 *  Copyright (c) 2015 LG Electronics.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

"use strict";

var PLUGIN_ID = "ConnectSDK";

/** @mixin */
var SimpleEventEmitter = {
    /**
     * Add event listener.
     * @param {string} event - name of event
     * @param {function} callback - function to call when event is fired
     * @param {object} [context] - object to bind to "this" value when calling function
     * @returns {object} reference to the same object to allow chaining
     */
    addListener: function (event, callback, context) {
        if (!event) { throw new Error("missing parameter: event"); }
        if (!callback) { throw new Error("missing parameter: callback"); }

        this._listeners = this._listeners || {};
        if (!this._listeners) this._listeners = {};
        if (!this._listeners[event]) this._listeners[event] = [];
        this._listeners[event].push({callback: callback, context: context});

        this.emit("_addListener", event);

        return this;
    },

    /**
     * Remove event listener with the specified callback and context.
     * If callback is null or undefined, all callbacks for this event will be removed.
     * @param {string} event - name of event
     * @param {function} [callback] - function originally passed to addListener
     * @param {object} [context] - context object originally passed to addListener
     * @returns {object} reference to the same object to allow chaining
     */
    removeListener: function (event, callback, context) {
        if (this._listeners && this._listeners[event]) {
            this._listeners[event] = this._listeners[event].filter(function (l) {
                return (callback && callback !== l.callback) && (context && context !== l.context);
            });
        }

        this.emit("_removeListener", event);

        return this;
    },

    hasListeners: function (event) {
        if (event) {
            return (this._listeners && this._listeners[event] && this._listeners[event].length > 0);
        } else {
            for (event in this._listeners) {
                if (event[0] !== "_" && this._listeners.hasOwnProperty(event) && this._listeners[event].length > 0) {
                    return true;
                }
            }
            return false;
        }
    },

    emit: function (event) {
        var listeners = this._listeners && this._listeners[event];
        var args = Array.prototype.slice.call(arguments, 1);

        if (listeners) {
            listeners.forEach(function (l) {
                l.callback.apply(l.context || null, args);
            });
        }

        if (this["on" + event]) {
            this["on" + event].apply(null, args);
        }
    },

    /**
     * Alias for addListener.
     * @param {string} event - name of event
     * @param {function} callback - function to call when event is fired
     * @param {object} [context] - object to bind to "this" value when calling function
     * @returns {object} reference to the same object to allow chaining
     */
    on: function (event, callback, context) {
        return this.addListener(event, callback, context);
    },

    /**
     * Alias for removeListener.
     * @param {string} event - event name
     * @param {function} [callback] - function originally passed to on
     * @param {object} [context] - context object originally passed to on
     * @returns {object} reference to the same object to allow chaining
     */
    off: function (event, callback, context) {
        return this.removeListener(event, callback, context);
    }
};

/** @mixin */
var SuccessCallbacks = {
    /**
     * Register a callback for the "success" event.
     * The success callback may be called with zero or more arguments
     * depending on the type of response.
     *
     * Example:
     * ```js
     * obj.success(function (result) {
     *     this.report("I got a result: " + result);
     * }, this);
     * ```
     *
     * @param {function} callback - function to call when event is fired
     * @param {*} [context] - object to bind to "this" value when calling function
     * @returns {object} reference to the same object to allow chaining
     */
    success: function (callback, context) {
        return this.on("success", callback, context);
    },

    /**
     * Register a callback for the "error" event.
     * The error callback will be called with a error object
     * as the only argument.
     *
     * Example:
     * ```js
     * obj.error(function (err) {
     *     this.reportError("I got an error: " + err);
     * }, this);
     * ```
     *
     * @param {function} callback - function to call when event is fired
     * @param {*} [context] - object to bind to "this" value when calling function
     * @returns {object} reference to the same object to allow chaining
     */
    error: function (callback, context) {
        return this.on("error", callback, context);
    },

    /**
     * Register a callback for the "complete" event.
     * The complete callback will be called with
     *
     * Example:
     * ```js
     * obj.complete(function (err, result) {
     *     if (err) {
     *         this.report("I got an error: " + err);
     *     } else {
     *         console.log("I got a result: " + result);
     *     }
     * }, this);
     * ```
     *
     * @param {function} callback - function to call when event is fired
     * @param {*} [context] - object to bind to "this" value when calling function
     * @returns {object} reference to the same object to allow chaining
     */
    complete: function (callback, context) {
        return this.on("complete", callback, context);
    }
};

// very simple class maker
var createClass = function (desc) {
    var constructor;

    if (desc.constructor) {
        constructor = desc.constructor;
        delete desc.constructor;
    } else {
        constructor = function () {};
        throw new Error("no constructor");
    }

    var prototype = constructor.prototype;

    if (desc.inherits) {
        var inProto = desc.inherits.prototype;
        for (var inProp in inProto) {
            if (inProto.hasOwnProperty(inProp)) {
                prototype[inProp] = inProto[inProp];
            }
        }
    }

    if (desc.mixins) {
        desc.mixins.forEach(function (mixin) {
            for (var mixinProp in mixin) {
                if (mixin.hasOwnProperty(mixinProp)) {
                    prototype[mixinProp] = mixin[mixinProp];
                }
            }
        });
        delete desc.mixins;
    }

    if (desc.statics) {
        for (var staticProp in desc.statics) {
            if (desc.statics.hasOwnProperty(staticProp)) {
                constructor[staticProp] = desc.statics[staticProp];
            }
        }
        delete desc.statics;
    }

    for (var p in desc) {
        if (desc.hasOwnProperty(p)) {
            prototype[p] = desc[p];
        }
    }

    return constructor;
};

/**
 * Create a CapabilityFilter
 * @param {string[]} capabilities - array of capabilities
 *
 * @class CapabilityFilter
 * @classdesc
 * CapabilityFilter consists of a list of capabilities which
 * must all be present in order for the filter to match.
 *
 * For example, `new ConnectSDK.CapabilityFilter([ConnectSDK.Capabilities.MediaPlayer.Play.Video, ConnectSDK.Capabilities.MediaControl.Pause])`
 * describes a device that supports showing a video and pausing it.
 */
var CapabilityFilter = createClass(
/** @lends CapabilityFilter.prototype */
{
    constructor: function (capabilities) {
        if (!capabilities) {
            throw new Error("missing argument to CapabilityFilter constructor");
        }

        if (typeof capabilities === "string") {
            capabilities = [capabilities];
        }

        this._capabilities = capabilities;
    },

    /**
     * @returns {string[]} list of capabilities in filter
     */
    getCapabilities: function () {
        return this._capabilities.slice(0);
    }
});

/**
 * @class
 * @classdesc
 * DevicePicker represents a picker UI widget created by calling
 * `DiscoveryManager.pickDevice()`.
 *
 * Example:
 * ```js
 * var devicePicker = ConnectSDK.discoveryManager.pickDevice()
 * devicePicker.success(function (device) {
 *     console.log("picked device " + device.getFriendlyName());
 * });
 * ```
 *
 * @mixes SimpleEventEmitter
 * @mixes SuccessCallbacks
 */
var DevicePicker = createClass(
/** @lends DevicePicker.prototype */
{
    mixins: [SimpleEventEmitter, SuccessCallbacks],

    constructor: function () {
    },

    /**
     * Close the device picker.
     */
    close: function () {
        cordova.exec(null, null, PLUGIN_ID, "closeDevicePicker", []);
    }
});

/**
 * @constant
 * @property {string} ON - access to capabilities that require pairing
 * @property {string} OFF - access to capabilities that don't require pairing
 */
var PairingLevel = {
    ON: "on",
    OFF: "off"
};

/**
 * @constant
 * @property {string} NONE - Only connect if no pairing is required
 * @property {string} FIRST_SCREEN - Prompt the user on the TV to accept paring
 * @property {string} PIN - Display a PIN on the TV, require user to enter it on the device
 * @property {string} MIXED - Prompt the user on the TV to accept pairing. Also display a pin on the TV that the user can enter on the device.
 * @property {string} AIRPLAY_MIRRORING - Require AirPlay mirroring to be enabled for connection (iOS only)
 */
var PairingType = {
    NONE: "NONE",
    FIRST_SCREEN: "FIRST_SCREEN",
    PIN: "PIN",
    MIXED: "MIXED",
    AIRPLAY_MIRRORING: "AIRPLAY_MIRRORING"
};

/**
 * @constant
 * @property {string} WEBAPP - display media using a web app mirrored to the TV (iOS only)
 * @property {string} MEDIA - display media using AirPlay media playback APIs
 */
var AirPlayServiceMode = {
    WEBAPP: "webapp",
    MEDIA: "media"
};

/**
 * @constant
 * @property {string} Chromecast - Chromecast
 * @property {string} DIAL - DIAL
 * @property {string} DLNA - DLNA
 * @property {string} NetcastTV - LG 2012/2013 Smart TV with Netcast
 * @property {string} Roku - Roku
 * @property {string} WebOSTV - LG 2014 Smart TV with webOS
 * @property {string} FireTV - Amazon FireTV
 * @property {string} AirPlay - Apple AirPlay
 */
var Services = {
    Chromecast: "Chromecast",
    DIAL: "DIAL",
    DLNA: "DLNA",
    NetcastTV: "NetcastTV",
    Roku: "Roku",
    WebOSTV: "webOS TV",
    FireTV: "FireTV",
    AirPlay: "AirPlay"
};

/**
 * @constant
 * @property {number} NUM_0
 * @property {number} NUM_1
 * @property {number} NUM_2
 * @property {number} NUM_3
 * @property {number} NUM_4
 * @property {number} NUM_5
 * @property {number} NUM_6
 * @property {number} NUM_7
 * @property {number} NUM_8
 * @property {number} NUM_9
 * @property {number} DASH
 * @property {number} ENTER
*/
var KeyCodes = {
    NUM_0: 0,
    NUM_1: 1,
    NUM_2: 2,
    NUM_3: 3,
    NUM_4: 4,
    NUM_5: 5,
    NUM_6: 6,
    NUM_7: 7,
    NUM_8: 8,
    NUM_9: 9,
    DASH: 10,
    ENTER: 11
};

/**
 * @class DiscoveryManager
 * @classdesc ConnectSDK.discoveryManager is the main entry point into ConnectSDK.
 * It allows finding devices on the local network and displaying a picker to
 * select and connect to a device. DiscoveryManager should always be accessed
 * through its singleton instance, ConnectSDK.discoveryManager.
 *
 * DiscoveryManager emits the following events while active:
 *
 * * startdiscovery
 * * stopdiscovery
 * * devicelistchanged
 * * devicefound (device)
 * * devicelost (device)
 * * deviceupdated (device)
 *
 * @mixes SimpleEventEmitter
 */
var DiscoveryManager = createClass(
/** @lends DiscoveryManager.prototype */
{
    mixins: [SimpleEventEmitter],

    constructor: function () {
        this._config = {};
        this._devices = {};
    },

    _getDeviceByDesc: function (desc) {
        var device = this._devices[desc.deviceId];
        if (!device) {
            device = new ConnectableDevice(desc);
            this._devices[desc.deviceId] = device;
        }
        return device;
    },

    _handleDiscoveryUpdate: function (args) {
        var event = args[0];
        var update = args[1];

        if (event) {
            if (event === "startdiscovery") {
                this._started = true;
            } else if (event === "stopdiscovery") {
                this._started = false;
            }

            if (event === "devicefound" || event === "devicelost" || event === "deviceupdated") {
                var deviceId = update.device.deviceId;
                var device = this._getDeviceByDesc(update.device);

                if (event === "devicelost") {
                    delete this._devices[deviceId];
                } else {
                    this._devices[deviceId] = device;

                    if (event === "deviceupdated") {
                        device._handleDiscoveryUpdate(update.device);
                    }
                }

                this.emit(event, device);
                this.emit("devicelistchanged", this.getDeviceList());
            } else {
                this.emit.apply(this, args);
            }
        }
    },

    _handleDiscoveryError: function (error) {
        //console.error("got discovery error " + error);
        this.emit("error", error);
    },

    _setPairingLevel: function (pairingLevel, updateNow) {
        if (!pairingLevel || (Object.prototype.toString.call(pairingLevel) !== "[object String]")) {
            throw new TypeError("expected pairingLevel to be a string");
        }

        this._config.pairingLevel = pairingLevel;

        if (updateNow) {
            cordova.exec(null, null, PLUGIN_ID, "setDiscoveryConfig",
                         [{pairingLevel: this._config.pairingLevel}]);
        }
    },

    _setAirPlayServiceMode: function (mode, updateNow) {
        this._config.airPlayServiceMode = mode;

        if (updateNow) {
            cordova.exec(null, null, PLUGIN_ID, "setDiscoveryConfig",
                         [{airPlayServiceMode: this._config.airPlayServiceMode}]);
        }
    },

    _setCapabilityFilters: function (filters, updateNow) {
        filters = filters || [];

        if (Object.prototype.toString.call(filters) !== "[object Array]") {
            throw new TypeError("capabilityFilters should be an array");
        }

        filters = filters.map(function (filter) {
            if (filter instanceof CapabilityFilter) {
                return filter.getCapabilities();
            } else if (Object.prototype.toString.call(filter) === "[object Array]") {
                return filter;
            } else {
                throw new TypeError("filter objects must be CapabilityFilter instances or arrays of strings");
            }
        });

        this._config.capabilityFilters = filters;

        if (updateNow && this._started) {
            cordova.exec(null, null, PLUGIN_ID, "setDiscoveryConfig",
                         [{capabilityFilters: this._config.capabilityFilters}]);
        }
    },

    /**
     * Start searching for devices. DiscoveryManager will start emitting events as the device
     * list changes, and populates the device list used by pickDevice().
     *
     * @param {Object} [config] - Dictionary of settings to configure before starting discovery.
     *     Supported keys are "pairingLevel" and "capabilityFilters".
     *     See setPairingLevel and setCapabilityFilter for more details.
     */
    startDiscovery: function (config) {
        if (config) {
            if (config.pairingLevel) {
                this._setPairingLevel(config.pairingLevel, false);
            }

            if (config.airPlayServiceMode) {
                this._setAirPlayServiceMode(config.airPlayServiceMode, false);
            }

            if (config.capabilityFilters) {
                this._setCapabilityFilters(config.capabilityFilters, false);
            }
        }

        cordova.exec(this._handleDiscoveryUpdate.bind(this), this._handleDiscoveryError.bind(this),
            PLUGIN_ID, "startDiscovery", [this._config]);
    },

    /**
     * Stop searching for devices.
     */
    stopDiscovery: function () {
        cordova.exec(null, this._handleDiscoveryError.bind(this), PLUGIN_ID, "stopDiscovery", [this.config]);
    },

    /**
     * Set pairing level. If set to ConnectSDK.PairingLevel.OFF, the SDK will
     * request device capabilities that do not require entering a pairing code/confirmation.
     *
     * @param {string} pairingLevel - Valid values are the constants ConnectSDK.PairingLevel.ON and ConnectSDk.PairingLevel.OFF
     */
    setPairingLevel: function (pairingLevel) {
        this._setPairingLevel(pairingLevel, true);
    },

    /**
     * Set mode for AirPlay support. If set to ConnectSDK.AirPlayServiceMode.WebApp, a web app will
     * will be mirrored to the TV. If set to ConnectSDK.AirPlayServiceMode.Media, only media
     * APIs will be available. On Android, media mode is the only option.
     *
     * NOTE: This setting must be configured before calling startDiscovery(), or passed in the options
     * parameter to startDiscovery(). The mode should not be changed once configured.
     */
    setAirPlayServiceMode: function (mode) {
        this._setAirPlayServiceMode(mode, true);
    },

    /**
     * Set capability filters. DiscoveryManager will only show devices that match at least one of the
     * CapabilityFilter instances.
     *
     * Example:
     * ```js
     * // Show devices that support playing videos and pausing OR support launching YouTube with a video id
     * ConnectSDK.discoveryManager.setCapabilityFilters([
     *     new ConnectSDK.CapabilityFilter([ConnectSDK.Capabilities.MediaPlayer.Play.Video, ConnectSDK.Capabilities.MediaControl.Pause])
     *     new ConnectSDK.CapabilityFilter([ConnectSDK.Capabilities.Launcher.YouTube.Params])
     * ])
     * ```
     *
     * @param {CapabilityFilter[]} filters - array of CapabilityFilter objects
     */
    setCapabilityFilters: function (filters) {
        this._setCapabilityFilters(filters, true);
    },

    /**
     * Show device picker popup. To get notified when the user has selected a device, add a success/error
     * listener to the DevicePicker returned when calling this method.
     *
     * @param {Object} [options] - All keys are optional
     *
     *     - pairingType (string): PairingType to use
     * @returns {DevicePicker}
     */
    pickDevice: function (options, successCallback, errorCallback) {
        var self = this;
        var picker = new DevicePicker();

        if (successCallback) {
            picker.on("success", successCallback);
        }

        if (errorCallback) {
            picker.on("error", errorCallback);
        }

        var success = function (deviceDesc) {
            var device = self._getDeviceByDesc(deviceDesc);
            device._handleDiscoveryUpdate(deviceDesc);
            picker.emit("success", device);
            picker.emit("complete", undefined, device);
        };

        var failure = function (error) {
            picker.emit("error", error);
            picker.emit("complete", error);
        };

        cordova.exec(success, failure, PLUGIN_ID, "pickDevice", [options]);

        return picker;
    },

    /**
     * Get a list of discovered devices available on the network.
     *
     * @returns {ConnectableDevice[]}
     */
    getDeviceList: function () {
        var list = [];
        for (var id in this._devices) {
            list.push(this._devices[id]);
        }

        return list;
    }
});

/**
 * @class ConnectableDevice
 * @classdesc ConnectableDevice represents a device on the network. It provides several
 * _capability interfaces_ which allow the developer to get information from and control
 * the device.
 *
 * These interfaces are accessed using getter methods like device.getLauncher(). Not all of
 * the capabilities or methods are available on every device; you should check if the
 * functionality is supported using device.supports(capabilityName).
 *
 * If the device was selected from the built-in picker, it will already be connected;
 * if the device was obtained from elsewhere then you must call device.connect()
 * and wait for the "ready" event before trying to use the device.
 *
 * Example:
 * ```js
 * device.on("ready", function () {
 *     // ready to send commands now
 *     device.getLauncher().launchYouTube(videoId);
 * });
 *
 * device.connect();
 * ```
 *
 * ConnectableDevice emits the following high-level events:
 *
 * * ready - device is ready to use
 * * disconnect - device is no longer connected
 * * capabilitieschanged - some capabilities may be available or unavailable now
 *
 * Internally, ConnectableDevice uses one or more _services_ to control a device on the network.
 * Services speak a specific protocol like DIAL or DLNA or other vendor-specific protocols.
 * Services are not directly accessible from the Connect SDK Cordova plugin at this time.
 *
 * There are several events related to the process of connecting to individual services:
 *
 * * serviceconnectionrequired - pending connection
 * * serviceconnectionerror - error connecting to a service
 * * servicepairingrequired - pairing is required for a service
 * * servicepairingsuccess - pairing successful for a service
 * * servicepairingerror - error pairing with a service
 *
 * @mixes SimpleEventEmitter
 */
var ConnectableDevice = createClass(
/** @lends ConnectableDevice.prototype */
{
    mixins: [SimpleEventEmitter],

    statics: {
        _interfaceClasses: {},
        _serviceWrappers: {},

        _registerInterface: function (name, ifaceClass) {
            var getterName = "get" + name[0].toUpperCase() + name.substr(1);

            this._interfaceClasses[name] = ifaceClass;

            this.prototype[getterName] = function () {
                return this._interfaces[name];
            };
        },

        _registerServiceWrapper: function (serviceName, wrapperFunc) {
            if (!serviceName) {
                throw new TypeError("invalid name: " + serviceName);
            }

            this._serviceWrappers[serviceName] = wrapperFunc;
        }
    },

    constructor: function (desc) {
        this._deviceId = desc.deviceId;
        this._nextCommandId = 1;
        this._interfaces = {};
        this._desc = desc;
        this._ready = desc.ready || false;

        for (var name in ConnectableDevice._interfaceClasses) {
            var IfaceClass = ConnectableDevice._interfaceClasses[name];
            this._interfaces[name] = new IfaceClass(this);
        }

        this._capabilities = {};
        this._subscribedToEvents = false;

        this.on("_addListener", this._handleAddListener, this);
        this._cacheServices();
        this._cacheCapabilities();
    },

    _handleAddListener: function () {
        if (this.hasListeners() && !this._subscribedToEvents) {
            // Subscribe to events on the native device object
            this._subscribedToEvents = true;
            cordova.exec(this._handleUpdate.bind(this), this._handleError.bind(this), PLUGIN_ID, "setDeviceListener", [this._deviceId]);
        }
    },

    _cacheServices: function () {
        var services = this._desc.services;
        delete this._desc.services;

        if (services) {
            this._services = {};

            for (var i = 0; i < services.length; i += 1) {
                var service = services[i];

                if (service.name) {
                    this._services[service.name] = service;
                }
            }
        }
    },

    _cacheCapabilities: function () {
        var caps = this._desc.capabilities;
        delete this._desc.capabilities;

        if (caps) {
            var capsHash = {};

            for (var i = 0; i < caps.length; i += 1) {
                capsHash[caps[i]] = true;
            }

            this._capabilities = capsHash;
        }
    },

    _handleDiscoveryUpdate: function (desc) {
        this._desc = desc;
        this._cacheServices();
        this._cacheCapabilities();

        if (desc.ready !== undefined) {
            this._ready = desc.ready;
        }
    },

    _handleUpdate: function (args) {
        var event = args[0];
        var update = args[1];

         if (event === "capabilitieschanged") {
            var i, cap;
            var added = update.added || [];
            var removed = update.removed || [];

            for (i = 0; i < added.length; i += 1) {
                cap = added[i];
                this._capabilities[cap] = true;
            }

            for (i = 0; i < removed.length; i += 1) {
                cap = removed[i];
                delete this._capabilities[cap];
            }

            args = [event]; // don't pass changes
        } else if (event === "disconnect") {
            this._capabilities = {};
            this._ready = false;
        } else if (event === "ready") {
            this._ready = true;
        }

        this.emit.apply(this, args);
    },

    _handleError: function (error) {
        this.emit("error", error);
    },

    /**
     * Connect to the device.
     */
    connect: function () {
        this._subscribedToEvents = true;
        cordova.exec(this._handleUpdate.bind(this), this._handleError.bind(this), PLUGIN_ID, "connectDevice", [this._deviceId]);
    },

    /**
     * Disconnect from the device.
     */
    disconnect: function () {
        cordova.exec(null, this._handleError.bind(this), PLUGIN_ID, "disconnectDevice", [this._deviceId]);
    },

    /**
     * Set a desirable pairing type to the device.
     * @param pairingType (string): PairingType to use
     */
    setPairingType: function (pairingType) {
        cordova.exec(this._handleUpdate.bind(this), this._handleError.bind(this), PLUGIN_ID, "setPairingType", [this._deviceId, pairingType]);
    },

    /**
     * Returns true if device is ready to use.
     */
    isReady: function () {
        return this._ready;
    },

    /**
     * Get the human-readable name of the device.
     * @returns {string}
     */
    getFriendlyName: function () {
        return this._desc.friendlyName;
    },

    /**
     * Get the last known IP address of the device.
     * @returns {string}
     */
    getIPAddress: function () {
        return this._desc.ipAddress || this._desc.lastKnownIPAddress;
    },

    /**
     * Get the device model name.
     * @returns {string}
     */
    getModelName: function () {
        return this._desc.modelName;
    },

    /**
     * Get the device model number.
     * @returns {string}
     */
    getModelNumber: function () {
        return this._desc.modelNumber;
    },

    /**
     * Get a list of capabilities supported by this device.
     * @returns {string[]} array of capabilities supported by this device
     */
    getCapabilities: function () {
        return Object.keys(this._capabilities);
    },

    /**
     * @param {string} name of capability. You should use the ConnectSDK.Capabilities constant to reference strings.
     * @returns {boolean} true if device supports the given capability
     */
    hasCapability: function (cap) {
        return !!this._capabilities[cap.toString()];
    },

    /**
     * Flexible version of hasCapability which returns true
     * if all of the capabilities specified are supported.
     *
     * * supports(ConnectSDK.Capabilities.MediaControl.Any)
     * * supports(ConnectSDK.Capabilities.VolumeControl.Set, ConnectSDK.Capabilities.Launcher.Any)
     * * supports([ConnectSDK.Capabilities.TVControl.Any, ConnectSDK.Capabilities.Launcher.Any])
     * @param [...] - array of capability names. You should use the ConnectSDK.Capabilities constant to reference strings.
     * @returns {boolean} true if all specified capabilities are supported
     */
    supports: function (arg) {
        var caps = [];
        if (arguments.length === 1) {
            if (Object.prototype.toString.call(arg) === "[object Array]") {
                caps = arg;
            } else {
                caps = [arg];
            }
        } else if (arguments.length > 0) {
            caps = arguments;
        }

        for (var i = 0; i < caps.length; i += 1) {
            if (!this.hasCapability(caps[i])) {
                return false;
            }
        }

        return true;
    },

    /**
     * Like supports() but returns true if any specified capability
     * is supported.
     * @param [...] - array of capability names. You should use the ConnectSDK.Capabilities constant to reference strings.
     * @returns {boolean} true if any specified capability is supported
     */
    supportsAny: function (arg) {
        var caps = [];
        if (arguments.length === 1) {
            if (Object.prototype.toString.call(arg) === "[object Array]") {
                caps = arg;
            } else {
                caps = [arg];
            }
        } else if (arguments.length > 0) {
            caps = arguments;
        }

        for (var i = 0; i < caps.length; i += 1) {
            if (this.hasCapability(caps[i])) {
                return true;
            }
        }

        return false;
    },

    /**
     * Returns true if the device supports the specified service.
     * See ConnectSDK.Services for a list of constants.
     *
     * @param {string} serviceName
     * @returns {boolean} true if service is supported
     */
    hasService: function (serviceName) {
        if (!serviceName) {
            throw new Error("hasService: service name argument is null or undefined");
        }

        return serviceName in this._services;
    },

    /**
     * Returns a wrapper for a service which gives access to low-level
     * functionality. Only a limited subset of the services supported
     * by the native SDK are available through this plugin.
     *
     * @param {string} serviceName
     * @returns {object} service object or null if not supported
     */
    getService: function (serviceName) {
        if (this.hasService(serviceName) && serviceName in ConnectableDevice._serviceWrappers) {
            return ConnectableDevice._serviceWrappers[serviceName](this);
        } else {
            return null;
        }
    },

    /**
     * Returns an internal id assigned by the SDK to this device.
     * For devices that have been connected to or paired, this
     * id will be persisted to disk in the device store to allow
     * the app to identify the device later (such as reconnecting
     * to the last connected device when starting the app).
     */
    getId: function () {
        return this._deviceId;
    },

    _createCommandId: function () {
        return this._deviceId + "_" + this._nextCommandId++;
    },

    _sendCommand: function (ifaceName, methodName, args, subscribe, responseWrapper) {
        var command = this._createCommand(subscribe, responseWrapper);
        command._send(ifaceName, methodName, args);

        return command;
    },

    _createCommand: function (subscribe, responseWrapper) {
        var options = {responseWrapper: responseWrapper};

        var commandId = this._createCommandId();
        var command = subscribe ? new Subscription(this, commandId, options) : new Command(this, commandId, options);

        return command;
    }
});

/**
 * @class Command
 * @classdesc
 * Command objects are returned when calling capability methods.
 * Command objects allow listening for success/cancel events from the
 * request.
 *
 * Example:
 * ```{.js}
 * var command = device.getLauncher().launchBrowser(url);
 *
 * command.success(function (launchSession) {
 *     console.log("command was successful");
 * }).error(function (err) {
 *     console.error("command failed");
 * });
 * ```
 * @mixes SimpleEventEmitter
 * @mixes SuccessCallbacks
 */
var Command = createClass(
/** @lends Command.prototype */
{
    mixins: [SimpleEventEmitter, SuccessCallbacks],

    _subscribe: false,

    constructor: function (device, commandId, options) {
        this._device = device;
        this._commandId = commandId;
        this._responseWrapper = options && options.responseWrapper;
    },

    _send: function (ifaceName, methodName, args) {
        var self = this;

        var success = function (update) {
            var event = update[0];

            if (event === "success") {
                var data = update.slice(1);

                if (self._responseWrapper) {
                    // call responseWrapper with [device, arg1, ...]
                    data = self._responseWrapper.apply(null, [self._device].concat(data));
                }

                self.emit.apply(self, ["success"].concat(data));
                self.emit.apply(self, ["complete", undefined].concat(data));
            } else {
                self.emit.apply(self, update);
            }
        };

        var failure = function (error) {
            self.emit("error", error);
            self.emit("complete", error);
        };

        cordova.exec(success, failure, PLUGIN_ID, "sendCommand",
            [self._device._deviceId, self._commandId, ifaceName, methodName, args, self._subscribe]
        );
    }
});

/**
 * @class Subscription
 * @extends Command
 * @classdesc
 * Subscription objects are returned when calling capability subscription
 * methods.
 *
 * Subscription objects allow listening for success/error events from the
 * request. Success events may be emitted multiple times when updates to the
 * subscription are received.
 *
 * Example:
 * ```{.js}
 * var subscription = device.getVolumeControl().subscribeVolume();
 * var updateCount = 0;
 *
 * subscription.success(function (volume) {
 *     // this may be called multiple times
 *     console.log("got volume update: " + volume);
 *
 *     updateCount++;
 *     if (updateCount > 5) {
 *         // unsubscribe after 5 updates
 *         subscription.unsubscribe();
 *     }
 * }).error(function (err) {
 *     console.error("subscription failed");
 * });
 * ```
 * @mixes SimpleEventEmitter
 * @mixes SuccessCallbacks
 */
var Subscription = createClass(
/** @lends Subscription.prototype */
{
    inherits: Command,

    _subscribe: true,

    constructor: function () {
        Command.apply(this, arguments);
    },

    /**
     * Unsubscribes from this subscription. Notifies the device that updates are no longer needed,
     * and stops emitting events from this Subscription object.
     */
    unsubscribe: function () {
        cordova.exec(null, null, PLUGIN_ID, "cancelCommand", [this._device._deviceId, this._commandId]);
    }
});

/**
 * @mixin WrappedObject
 */
var WrappedObject = {
    _scheduleCleanup: function () {
        var self = this;
        setTimeout(function () {
            if (!self._acquireRequested) {
                self.release();
            }
        }, 0);
    },

    _checkAcquired: function () {
        if (!this._objectId) {
            if (this._acquired) {
                throw new Error(this._typeName + " instance was released and is no longer valid to use");
            } else {
                throw new Error(this._typeName + " instance not valid; you must call .acquire() immediately upon obtaining the object (such as in a success callback) and call .release() when done using it");
            }
        }
    },

    /**
     * Indicate that you would like to keep an active reference to this object. Wrapped objects that are not acquired may be freed after the success callback returns.
     * @returns {object} reference to object
     */
    acquire: function () {
        if (this._objectId === undefined) {
            return this;
        }

        this._checkAcquired();

        if (!this._acquired && !this._acquireRequested) {
            this._acquireRequested = true;

            // FIXME report error
            cordova.exec(this._handleEvent.bind(this), null, PLUGIN_ID, "acquireWrappedObject", [this._objectId]);
        }

        return this;
    },

    /**
     * Release the reference to this object. After calling .release(), this object may no longer be used.
     * You should always release objects when you no longer need them, to avoid memory leaks.
     */
    release: function () {
        if (this._objectId) {
            cordova.exec(null, null, PLUGIN_ID, "releaseWrappedObject", [this._objectId]);
            this._objectId = null;
        }

        return this;
    },

    _handleEvent: function (args) {
        this.emit.apply(this, args);
    }
};

/**
 * @class LaunchSession
 * @mixes WrappedObject
 * @classdesc A LaunchSession represents the result of an app launch. Its primary purpose is to be able to close an app that was previously launched, using the launchSession.close() method.
 */
var LaunchSession = createClass(
/** @lends LaunchSession.prototype */
{
    mixins: [SimpleEventEmitter, WrappedObject],

    _typeName: "LaunchSession",

    constructor: function (device, data) {
        this._device = device;
        this._data = data;
        this._objectId = data.objectId;
    },

    getAppId: function () {
        return this._data.appId;
    },

    /** Close the app/media associated with this launch session. */
    close: function () {
        return this._device._sendCommand("CORDOVAPLUGIN", "closeLaunchSession", {"launchSession": this._data}, false);
    },

    toJSON: function () {
        return this._data;
    }
});

/**
 * @class MediaControlWrapper
 */
var MediaControlWrapper = createClass(
/** @lends MediaControl.prototype */
{
    mixins: [SimpleEventEmitter, WrappedObject],

    constructor: function (device, data) {
        this._device = device;
        this._data = data;
        this._objectId = data.objectId;
    },

    _sendCommand: function (command, params) {
        params = params || {};
        params.objectId = this._objectId;
        return this._device._sendCommand("mediaControl", command, params);
    },

    play: function () {
        return this._sendCommand("play");
    },

    pause: function () {
        return this._sendCommand("pause");
    },

    stop: function () {
        return this._sendCommand("stop");
    },

    rewind: function () {
        return this._sendCommand("rewind");
    },

    fastForward: function () {
        return this._sendCommand("fastForward");
    },

    seek: function (position) {
        return this._sendCommand("seek", {position: position});
    },

    getDuration: function () {
        return this._sendCommand("getDuration");
    },

    getPosition: function () {
        return this._sendCommand("getPosition");
    },

    subscribePlayState: function () {
        return this._sendCommand("subscribePlayState");
    }
});

/**
 * @class PlaylistControlWrapper
 */
var PlaylistControlWrapper = createClass(
/** @lends PlaylistControl.prototype */
{
    mixins: [SimpleEventEmitter, WrappedObject],

    constructor: function (device, data) {
        this._device = device;
        this._data = data;
        this._objectId = data.objectId;
    },

    _sendCommand: function (command, params) {
        params = params || {};
        params.objectId = this._objectId;
        return this._device._sendCommand("playlistControl", command, params);
    },

    next: function() {
        return this._sendCommand("next");
    },

    previous: function() {
        return this._sendCommand("previous");
    },

    jumpToTrack: function (index) {
        return this._sendCommand("jumpToTrack", {"index": index});
    }
});

/**
 * @class WebAppSession
 * @mixes SimpleEventEmitter
 * @mixes WrappedObject
 * @classdesc A WebAppSession represents a web-based app running on a TV.
 * You can communicate with a web app by first calling connect() to establish a communication channel,
 * and then listening for "message" events as well as sending your own messages using sendText and
 * sendJSON.
 *
 * Example:
 * ```js
 * device.getWebAppLauncher().launchWebApp(webAppId).success(function (session) {
 *     this.session = session.acquire(); // hold on to a reference
 *
 *     session.connect().success(function () {
 *         session.sendText("Hello world");
 *     });
 *
 *     session.on('message', function (message) {
 *         // message could be either a string or an object
 *         if (typeof message === 'string') {
 *             console.log("received string message: " + message);
 *         } else {
 *             console.log("received object message: " + JSON.stringify(message);
 *         }
 *     }, this);
 *
 *     session.on('disconnect', function () {
 *         console.log("session disconnected");
 *         this.session = null;
 *     }, this);
 *
 * }, this);
 * ```
 */
var WebAppSession = createClass(
/** @lends WebAppSession.prototype */
{
    mixins: [SimpleEventEmitter, WrappedObject],

    _typeName: "WebAppSession",

    constructor: function (device, data) {
        this._device = device;
        this._data = data;
        this._objectId = data.objectId;

        this._scheduleCleanup();
    },

    /**
     * Open a message channel to the app.
     * @returns {Command}
     */
    connect: function () {
        this.acquire();
        return this._device._sendCommand("webAppSession", "connect", {objectId: this._objectId});
    },

    /**
     * Close channel to app.
     * @returns {Command}
     */
    disconnect: function () {
        this.acquire();
        return this._device._sendCommand("webAppSession", "disconnect", {objectId: this._objectId});
    },

    /**
     * Set web app session listener to app
     * @returns {Command}
     */
     setWebAppSessionListener: function() {
        this.acquire();
        return this._device._sendCommand("webAppSession", "setWebAppSessionListener", {objectId: this._objectId});
     },


    /**
     * Send a text string to the app. Must be connected first.
     * @param {string} text - Text to send to the app
     * @returns {Command}
     */
    sendText: function (text) {
        this.acquire();
        return this._device._sendCommand("webAppSession", "sendText", {objectId: this._objectId, text: text});
    },

    /**
     * Send a plain JavaScript object to the app. Must be connected first.
     * If the receiving app does not support non-string messages, the object will be serialized into a string in JSON format.
     *
     * @param {object} object - Plain JavaScript object to send to the app
     * @returns {Command}
     */
    sendJSON: function (obj) {
        this.acquire();
        return this._device._sendCommand("webAppSession", "sendJSON", {objectId: this._objectId, jsonObject: obj});
    },

    /**
     * Close the web app.
     * @returns {Command}
     */
    close: function () {
        return this._device._sendCommand("CORDOVAPLUGIN", "closeLaunchSession", {"launchSession": this._data.launchSession}, false);
    },

    toJSON: function () {
        return this._data;
    }
});

function wrapLaunchSession(device, launchSessionData) {
    return [new LaunchSession(device, launchSessionData)];
}

function wrapMediaLaunchSession(device, launchSessionData, mediaControlData, playlistControlData) {
    return [new LaunchSession(device, launchSessionData),
        mediaControlData && new MediaControlWrapper(device, mediaControlData),
        playlistControlData && new PlaylistControlWrapper(device, playlistControlData)];
}

function wrapWebAppSession(device, sessionData) {
    return [new WebAppSession(device, sessionData)];
}

function createDeviceMethod(ifaceName, name, method) {
    var f = function () {
        var params = {};
        var args = method.args || [];

        for (var i = 0; i < args.length; i++) {
            var arg = args[i];
            var optional = false;

            if (arg[arg.length - 1] === '?') {
                arg = arg.substr(0, arg.length - 1);
                optional = true;
            }

            if (!optional && (i > arguments.length)) {
                if (console) { console.warn("missing parameter to " + name + ": " + arg); }
            }

            params[arg] = arguments[i];
        }

        return this._device._sendCommand(ifaceName, name, params, method.subscribe, method.responseWrapper);
    };
    return f;
}

function registerDeviceInterface (ifaceName, methods) {
    var desc = {
        constructor: function (device) {
            this._device = device;
        }
    };

    for (var name in methods) {
        var method = methods[name];

        if (typeof method === 'function') {
            desc[name] = method;
        } else if (typeof method === 'object') {
            desc[name] = createDeviceMethod(ifaceName, name, method);
        }
    }

    var ifaceClass = createClass(desc);

    if (!exports.interfaces) { exports.interfaces = {}; }
    exports.interfaces[ifaceName] = ifaceClass;

    // Add getter method to ConnectableDevice
    ConnectableDevice._registerInterface(ifaceName, ifaceClass);
}

/**
 * @method ConnectableDevice.getLauncher
 * @returns {Launcher}
 */

/**
 * @callback launchCallback
 * @param {LaunchSession} launchSession
 */

/**
 * @callback appListCallback
 * @param {AppInfo[]} appList - Each AppInfo object contains:
 *
 * - id (string): platform-specific appId
 * - name (string): human-readable name of app
 */

/** @class Launcher */
registerDeviceInterface("launcher",
/** @lends Launcher.prototype */
{
    /**
     * @method
     * @param {string} appId
     * @success {launchCallback}
     */
    launchApp: {
        args: ["appId", "params?"],
        responseWrapper: wrapLaunchSession
    },

    /**
     * @method
     * @param {string} appId
     */
    closeApp: {
        args: ["appId"],
        responseWrapper: wrapLaunchSession
    },

    /**
     * @method
     * @param {string} appId
     * @success {launchCallback}
     */
    launchAppStore: {
        args: ["appId"],
        responseWrapper: wrapLaunchSession
    },

    /**
     * @method
     * @param {string} url
     * @success {launchCallback}
     */
    launchBrowser: {
        args: ["url?"],
        responseWrapper: wrapLaunchSession
    },

    /**
     * @method
     * @param {string} contentId
     * @success {launchCallback}
     */
    launchHulu: {
        args: ["contentId?"],
        responseWrapper: wrapLaunchSession
    },

    /**
     * @method
     * @param {string} contentId
     * @success {launchCallback}
     */
    launchNetflix: {
        args: ["contentId?"],
        responseWrapper: wrapLaunchSession
    },

    /**
     * @method
     * @param {string} contentId
     * @success {launchCallback}
     */
    launchYouTube: {
        args: ["contentId?"],
        responseWrapper: wrapLaunchSession
    },

    /**
     * @method
     * @success {appListCallback}
     */
    getAppList: {
    }
});

/**
 * @method ConnectableDevice.getMediaPlayer
 * @returns {MediaPlayer}
 */

/**
 * @callback mediaLaunchCallback
 * @param {LaunchSession} launchSession
 * @param {MediaControl} mediaControl
 */

/** @class MediaPlayer */
registerDeviceInterface("mediaPlayer",
/** @lends MediaPlayer.prototype */
{
    /**
     * @method
     * @param {string} url
     * @param {string} mimeType
     * @param {object} [options] All properties are optional:
     *
     *   - title (string): Title text to display
     *   - description (string): Description text to display
     *   - iconUrl (string): URL of icon to show next to the title
     * @success {mediaLaunchCallback}
     */
    displayImage: {
        args: ["url", "mimeType", "options?"],
        responseWrapper: wrapMediaLaunchSession,
        subscribe: true
    },

    /**
     * @method
     * @param {string} url
     * @param {string} mimeType
     * @param {object} [options] All properties are optional:
     *
     *   - title (string): Title text to display
     *   - description (string): Description paragraph to display
     *   - iconUrl (string): URL of icon to show next to the title
     *   - shouldLoop (boolean): Whether to automatically loop playback
     *   - subtitles {object} subtitle track with options (properties are
     *      optional unless specified otherwise):
     *      - url (string) [required]: must be a valid URL
     *      - mimeType (string)
     *      - language (string)
     *      - label (string)
     * @success {mediaLaunchCallback}
     */
    playMedia: {
        args: ["url", "mimeType", "options?"],
        responseWrapper: wrapMediaLaunchSession,
        subscribe: true
    },

    // deprecated
    closeMedia: {
        args: ["appInfo"]
    }
});

/**
 * @method ConnectableDevice.getExternalInputControl
 * @returns {ExternalInputControl}
 */

/**
 * @typedef {object} ExternalInputInfo
 * @property {string} id
 * @property {string} name
 */

/**
 * @callback externalInputListCallback
 * @param {ExternalInputInfo[]} externalInputList
 */

/**
 * @class ExternalInputControl
 * @classdesc
 * ExternalInputInfo objects are plain JavaScript objects with the following properties:
 *
 * - id (string): A platform-specific id representing an input device
 * - name (string): A human-readable name for the input device
 */
registerDeviceInterface("externalInputControl",
/** @lends ExternalInputControl.prototype */
{
    /**
     * @method
     * @success {externalInputListCallback}
     */
    getExternalInputList: {},

    /**
     * @method
     * @param {object} externalInputInfo
     */
    setExternalInput: {
        args: ["externalInputInfo"]
    },

    /**
     * @method
     */
    showExternalInputPicker: {
        responseWrapper: wrapLaunchSession
    }
});

/**
 * @method ConnectableDevice.getMediaControl
 * @returns {MediaControl}
 */

/**
 * @callback getDurationCallback
 * @param {number} duration - duration in seconds
 */

/**
 * @callback getPositionCallback
 * @param {number} position - position in seconds
 */

/**
 * @callback playStateCallback
 * @param {string} playState - One of:
 * - "unknown"
 * - "idle"
 * - "playing"
 * - "paused"
 * - "buffering"
 * - "finished"
 */

/** @class MediaControl */
registerDeviceInterface("mediaControl",
/** @lends MediaControl.prototype */
{
    /** @method */
    play: {},

    /** @method */
    pause: {},

    /** @method */
    stop: {},

    /** @method */
    rewind: {},

    /** @method */
    fastForward: {},

    /**
     * @method
     * @param {number} position - Media seek position in seconds
     */
    seek: {
        args: ["position"]
    },

    /**
     * @method
     * @success {getDurationCallback}
     */
    getDuration: {},

    /**
     * @method
     * @success {getPositionCallback}
     */
    getPosition: {},

    /**
     * @method
     * @success {playStateCallback}
     */
    subscribePlayState: {}
});

/** @class PlaylistControl */
registerDeviceInterface("playlistControl",
/** @lends PlaylistControl.prototype */
{
    /** @method */
    next: {},

    /** @method */
    previous: {},

    /**
     * @method
     * @param {number} index - Playlist track index
     */
    jumpToTrack: {
        args: ["index"]
    }
});

/**
 * @method ConnectableDevice.getKeyControl
 * @returns {KeyControl}
 */

/** @class KeyControl */
registerDeviceInterface("keyControl",
/** @lends KeyControl.prototype */
{
    /** @method */
    up: {},

    /** @method */
    down: {},

    /** @method */
    left: {},

    /** @method */
    right: {},

    /** @method */
    ok: {},

    /** @method */
    back: {},

    /** @method */
    home: {},

    /**
     * @method
     * @param {number} keyCode - Refer to the native Connect SDK device services for a list of keycodes
     */
    sendKeyCode: {
        args: ["keyCode"]
    }
});

/**
 * @method ConnectableDevice.getMouseControl
 * @returns {MouseControl}
 */

/** @class MouseControl */
registerDeviceInterface("mouseControl",
/** @lends MouseControl.prototype */
{
    /** @method */
    connectMouse: {},

    /** @method */
    disconnectMouse: {},

    /**
     * @method
     * @param {number} dx
     * @param {number} dy
     */
    move: {
        args: ["dx", "dy"]
    },

    /**
     * @method
     * @param {number} dx
     * @param {number} dy
     */
    scroll: {
        args: ["dx", "dy"]
    },

    /**
     * @method
     */
    click: {}
});

/**
 * @method ConnectableDevice.getTextInputControl
 * @returns {TextInputControl}
 */

/**
 * @typedef TextInputStatus
 * @property {boolean} isVisible
 * @property {object} rawData
 */

/**
 * @callback textInputStatusCallback
 * @param {TextInputStatus} textInputStatus
 */

/** @class TextInputControl */
registerDeviceInterface("textInputControl",
/** @lends TextInputControl.prototype */
{
    /**
     * @method
     * @param {string} input
     */
    sendText: {
        args: ["input"]
    },

    /** @method */
    sendEnter: {},

    /** @method */
    sendDelete: {},

    /**
     * @method
     * @success {textInputStatusCallback}
     */
    subscribeTextInputStatus: {}
});

/**
 * @method ConnectableDevice.getPowerControl
 * @returns {PowerControl}
 */

/** @class PowerControl */
registerDeviceInterface("powerControl",
/** @lends PowerControl.prototype */
{
    /** @method */
    powerOff: {}
});

/**
 * @method ConnectableDevice.getToastControl
 * @returns {ToastControl}
 */

/** @class ToastControl */
registerDeviceInterface("toastControl",
/** @lends ToastControl.prototype */
{
    /**
     * @method
     * @param {string} message
     * @param {object} [options]
     *   - iconData (string): base64-encoded image
     *   - iconExtension (string): file extension of icon (.png or .jpg)
     */
    showToast: {
        args: ["message", "options?"]
    },

    /**
     * @method
     * @param {string} message
     * @param {object} options
     *   - iconData (string): base64-encoded image
     *   - iconExtension (string): file extension of icon (.png or .jpg)
     *   - appId (string): app to launch when clicked OR
     *   - url (string): url to launch in browser when clicked
     */
    showClickableToast: {
        args: ["message", "options?"]
    }
});

/**
 * @method ConnectableDevice.getTVControl
 * @returns {TVControl}
 */

/**
 * @callback getChannelCallback
 * @param {ChannelInfo} channelInfo
 */

/**
 * @callback getChannelListCallback
 * @param {ChannelInfo[]} channelInfoList
 */

/**
 * @typedef {object} ChannelInfo
 * @property {string} id
 * @property {string} name
 * @property {string} number
 * @property {number} majorNumber
 * @property {number} minorNumber
 */

/**
 * @class TVControl
 * @classdesc ChannelInfo objects are plain JavaScript objects with the following properties:
 *
 * - id (string): A platform-specific id used to identify the channel
 * - name (string): A human-readable name of the channel, if available
 * - number (string): Channel number such as "54-1"
 * - majorNumber (number): Major channel number
 * - minorNumber (minorNumber: Minor channel number (subchannel number)
 */
registerDeviceInterface("TVControl",
/** @lends TVControl.prototype */
{
    /** @method */
    channelUp: {},

    /** @method */
    channelDown: {},

    /**
     * @method
     * @param {object} channelInfo
     */
    setChannel: {
        args: ["channelInfo"]
    },

    /**
     * @method
     * @success {getChannelListCallback}
     */
    getChannelList: {},

    /**
     * @method
     * @success {getChannelCallback}
     */
    getCurrentChannel: {},

    /**
     * @method
     * @success {getChannelCallback}
     */
    subscribeCurrentChannel: {
        subscribe: true
    }
});

/**
 * @method ConnectableDevice.getVolumeControl
 * @returns {VolumeControl}
 */

/**
 * @callback getVolumeCallback
 * @param {number} volume
 */

/**
 * @callback getMuteCallback
 * @param {boolean} mute
 */

/** @class VolumeControl **/
registerDeviceInterface("volumeControl",
/** @lends VolumeControl.prototype */
{
    /**
     * @method
     * @success {getVolumeCallback}
     */
    getVolume: {},

    /**
     * @method
     * @param {float} volume
     */
    setVolume: {
        args: ["volume"]
    },

    /** @method */
    volumeUp: {},

    /** @method */
    volumeDown: {},

    /**
     * @method
     * @success {getMuteCallback}
     */
    getMute: {},

    /**
     * @method
     * @param {boolean} mute
     */
    setMute: {
        args: ["mute"]
    },

    /**
     * @method
     * @success {getMuteCallback}
     */
    subscribeMute: {
        subscribe: true
    },

    /**
     * @method
     * @success {getVolumeCallback}
     */
    subscribeVolume: {
        subscribe: true
    }
});

/**
 * @method ConnectableDevice.getWebAppLauncher
 * @returns {WebAppLauncher}
 */

/**
 * @callback webAppLaunchCallback
 * @param {WebAppSession} webAppSession
 */

/** @class WebAppLauncher */
registerDeviceInterface("webAppLauncher",
/** @lends WebAppLauncher.prototype */
{
    /**
     * See WebAppSession for a detailed example.
     *
     * @method
     * @param {string} webAppId
     * @param {object} params
     * @success {webAppLaunchCallback}
     */
    launchWebApp: {
        args: ["webAppId", "params?"],
        responseWrapper: wrapWebAppSession
    },

    /**
     * @method
     * @param {string} webAppId
     * @param {object} params
     * @success {webAppLaunchCallback}
     */
    joinWebApp: {
        args: ["webAppId", "params?"],
        responseWrapper: wrapWebAppSession
    },

    /**
     * @method
     * @param {string} webAppId
     */
    closeWebApp: {
        args: ["webAppId"]
    },

    /**
     * @method
     * @param {string} webAppId
     */
    pinWebApp: {
        args: ["webAppId"]
    },

    /**
     * @method
     * @param {string} webAppId
     */
    unPinWebApp: {
        args: ["webAppId"]
    },

    /**
     * @method
     * @param {string} webAppId
     * @success {isWebAppPinnedCallback}
     */
    isWebAppPinned: {
        args: ["webAppId"]
    },

    /**
     * @method
     * @param {string} webAppId
     * @success {subscribeIsWebAppPinnedCallback}
     */
    subscribeIsWebAppPinned: {
        args: ["webAppId"],
        subscribe: true
    }
});

var ServiceWrapper = createClass({
    _interfaceName: "",

    constructor: function (device) {
    },

    _sendServiceCommand: function (methodName, args, subscribe) {
        return this._device._sendCommand(this._interfaceName, methodName, args, subscribe);
    }
});

var WebOSTVServiceWrapper = createClass({
    inherits: ServiceWrapper,
    _interfaceName: "webOSTVService",

    constructor: function (device) {
        ServiceWrapper.call(this, device);
        this._device = device;
    },

    // connect and wait for app
    connectToApp: function (appId) {
        return this._sendServiceCommand("joinApp", {appId: appId}, false, wrapWebAppSession);
    },

    // connect to app if running, or return an error if not running
    joinApp: function (appId) {
        return this._sendServiceCommand("joinApp", {appId: appId}, false, wrapWebAppSession);
    }
});

ConnectableDevice._registerServiceWrapper(Services.WebOSTV, function (device) {
    return new WebOSTVServiceWrapper(device);
});

exports.Command = Command;
exports.Subscription = Subscription;
exports.LaunchSession = LaunchSession;
exports.DevicePicker = DevicePicker;
exports.ConnectableDevice = ConnectableDevice;
exports.CapabilityFilter = CapabilityFilter;
exports.PairingLevel = PairingLevel;
exports.PairingType = PairingType;
exports.AirPlayServiceMode = AirPlayServiceMode;
exports.Services = Services;
exports.KeyCodes = KeyCodes;

if (typeof __CSDKCapabilities !== "undefined") {
    exports.Capabilites = __CSDKCapabilities;
}

// Singleton instance
exports.discoveryManager = new DiscoveryManager();
});
