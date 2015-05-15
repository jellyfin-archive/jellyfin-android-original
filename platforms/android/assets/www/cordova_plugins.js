cordova.define('cordova/plugin_list', function(require, exports, module) {
module.exports = [
    {
        "file": "plugins/cordova-plugin-whitelist/whitelist.js",
        "id": "cordova-plugin-whitelist.whitelist",
        "runs": true
    },
    {
        "file": "plugins/cordova-plugin-device/www/device.js",
        "id": "cordova-plugin-device.device",
        "clobbers": [
            "device"
        ]
    },
    {
        "file": "plugins/cordova-plugin-dialogs/www/notification.js",
        "id": "cordova-plugin-dialogs.notification",
        "merges": [
            "navigator.notification"
        ]
    },
    {
        "file": "plugins/cordova-plugin-dialogs/www/android/notification.js",
        "id": "cordova-plugin-dialogs.notification_android",
        "merges": [
            "navigator.notification"
        ]
    },
    {
        "file": "plugins/cordova-plugin-statusbar/www/statusbar.js",
        "id": "cordova-plugin-statusbar.statusbar",
        "clobbers": [
            "window.StatusBar"
        ]
    },
    {
        "file": "plugins/cordova-plugin-chrome-apps-sockets-udp/sockets.udp.js",
        "id": "cordova-plugin-chrome-apps-sockets-udp.sockets.udp",
        "clobbers": [
            "chrome.sockets.udp"
        ]
    },
    {
        "file": "plugins/com.rd11.remote-controls/www/RemoteControls.js",
        "id": "com.rd11.remote-controls.RemoteControls",
        "clobbers": [
            "window.remoteControls"
        ]
    },
    {
        "file": "plugins/hu.dpal.phonegap.plugins.UniqueDeviceID/www/uniqueid.js",
        "id": "hu.dpal.phonegap.plugins.UniqueDeviceID.UniqueDeviceID",
        "merges": [
            "window.plugins.uniqueDeviceID"
        ]
    },
    {
        "file": "plugins/io.litehelpers.cordova.sqlite/www/SQLitePlugin.js",
        "id": "io.litehelpers.cordova.sqlite.SQLitePlugin",
        "clobbers": [
            "SQLitePlugin"
        ]
    },
    {
        "file": "plugins/cordova-plugin-chrome-apps-common/events.js",
        "id": "cordova-plugin-chrome-apps-common.events",
        "clobbers": [
            "chrome.Event"
        ]
    },
    {
        "file": "plugins/cordova-plugin-chrome-apps-common/errors.js",
        "id": "cordova-plugin-chrome-apps-common.errors"
    },
    {
        "file": "plugins/cordova-plugin-chrome-apps-common/stubs.js",
        "id": "cordova-plugin-chrome-apps-common.stubs"
    },
    {
        "file": "plugins/cordova-plugin-chrome-apps-common/helpers.js",
        "id": "cordova-plugin-chrome-apps-common.helpers"
    }
];
module.exports.metadata = 
// TOP OF METADATA
{
    "cordova-plugin-whitelist": "1.0.1-dev",
    "cordova-plugin-device": "1.0.0",
    "cordova-plugin-console": "1.0.0",
    "cordova-plugin-dialogs": "1.0.0",
    "cordova-plugin-statusbar": "1.0.0",
    "cordova-plugin-chrome-apps-sockets-udp": "1.2.2",
    "com.telerik.plugins.wkwebview": "0.3.7",
    "com.crewmeister.cordova-ios-backgroundcolor": "0.0.1",
    "com.rd11.remote-controls": "1.0.0",
    "hu.dpal.phonegap.plugins.UniqueDeviceID": "1.2.0",
    "io.litehelpers.cordova.sqlite": "0.7.7",
    "cordova-plugin-chrome-apps-common": "1.0.7"
}
// BOTTOM OF METADATA
});