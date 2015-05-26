cordova.define('cordova/plugin_list', function(require, exports, module) {
module.exports = [
    {
        "file": "plugins/com.rd11.remote-controls/www/RemoteControls.js",
        "id": "com.rd11.remote-controls.RemoteControls",
        "clobbers": [
            "window.remoteControls"
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
    },
    {
        "file": "plugins/cordova-plugin-chrome-apps-sockets-udp/sockets.udp.js",
        "id": "cordova-plugin-chrome-apps-sockets-udp.sockets.udp",
        "clobbers": [
            "chrome.sockets.udp"
        ]
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
        "file": "plugins/cordova-plugin-whitelist/whitelist.js",
        "id": "cordova-plugin-whitelist.whitelist",
        "runs": true
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
        "file": "plugins/com.connectsdk.cordovaplugin/www/ConnectSDK.js",
        "id": "com.connectsdk.cordovaplugin.ConnectSDK.js",
        "merges": [
            "navigator.ConnectSDK",
            "ConnectSDK"
        ]
    },
    {
        "file": "plugins/de.appplant.cordova.plugin.local-notification/www/local-notification.js",
        "id": "de.appplant.cordova.plugin.local-notification.LocalNotification",
        "clobbers": [
            "cordova.plugins.notification.local",
            "plugin.notification.local"
        ]
    },
    {
        "file": "plugins/de.appplant.cordova.plugin.local-notification/www/local-notification-core.js",
        "id": "de.appplant.cordova.plugin.local-notification.LocalNotification.Core",
        "clobbers": [
            "cordova.plugins.notification.local.core",
            "plugin.notification.local.core"
        ]
    },
    {
        "file": "plugins/de.appplant.cordova.plugin.local-notification/www/local-notification-util.js",
        "id": "de.appplant.cordova.plugin.local-notification.LocalNotification.Util",
        "merges": [
            "cordova.plugins.notification.local.core",
            "plugin.notification.local.core"
        ]
    },
    {
        "file": "plugins/cordova-plugin-purchase/www/store-android.js",
        "id": "cordova-plugin-purchase.InAppBillingPlugin",
        "clobbers": [
            "store"
        ]
    },
    {
        "file": "plugins/com.hutchind.cordova.plugins.launcher/www/Launcher.js",
        "id": "com.hutchind.cordova.plugins.launcher.Launcher",
        "clobbers": [
            "plugins.launcher"
        ]
    },
    {
        "file": "plugins/com.mesmotronic.plugins.fullscreen/www/AndroidFullScreen.js",
        "id": "com.mesmotronic.plugins.fullscreen.AndroidFullScreen",
        "clobbers": [
            "AndroidFullScreen"
        ]
    }
];
module.exports.metadata = 
// TOP OF METADATA
{
    "com.crewmeister.cordova-ios-backgroundcolor": "0.0.1",
    "com.rd11.remote-controls": "1.0.0",
    "com.telerik.plugins.wkwebview": "0.3.7",
    "cordova-plugin-chrome-apps-common": "1.0.7",
    "cordova-plugin-chrome-apps-iossocketscommon": "1.0.2",
    "cordova-plugin-chrome-apps-sockets-udp": "1.2.2",
    "cordova-plugin-console": "1.0.0",
    "cordova-plugin-device": "1.0.0",
    "cordova-plugin-dialogs": "1.0.0",
    "cordova-plugin-statusbar": "1.0.0",
    "cordova-plugin-whitelist": "1.0.1-dev",
    "hu.dpal.phonegap.plugins.UniqueDeviceID": "1.2.0",
    "io.litehelpers.cordova.sqlite": "0.7.7",
    "com.connectsdk.cordovaplugin": "1.2.0",
    "de.appplant.cordova.plugin.local-notification": "0.8.2dev",
    "cordova-plugin-purchase": "1.0.4",
    "cordova-plugin-okhttp": "2.0.0-dev",
    "com.hutchind.cordova.plugins.launcher": "0.2.2",
    "com.mesmotronic.plugins.fullscreen": "1.0.1",
    "android.support.v4": "21.0.1"
}
// BOTTOM OF METADATA
});