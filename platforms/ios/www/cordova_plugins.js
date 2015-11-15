cordova.define('cordova/plugin_list', function(require, exports, module) {
module.exports = [
    {
        "file": "plugins/cordova-plugin-chrome-apps-sockets-udp/sockets.udp.js",
        "id": "cordova-plugin-chrome-apps-sockets-udp.sockets.udp",
        "clobbers": [
            "chrome.sockets.udp"
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
    },
    {
        "file": "plugins/cordova-plugin-file/www/DirectoryEntry.js",
        "id": "cordova-plugin-file.DirectoryEntry",
        "clobbers": [
            "window.DirectoryEntry"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/DirectoryReader.js",
        "id": "cordova-plugin-file.DirectoryReader",
        "clobbers": [
            "window.DirectoryReader"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/Entry.js",
        "id": "cordova-plugin-file.Entry",
        "clobbers": [
            "window.Entry"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/File.js",
        "id": "cordova-plugin-file.File",
        "clobbers": [
            "window.File"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/FileEntry.js",
        "id": "cordova-plugin-file.FileEntry",
        "clobbers": [
            "window.FileEntry"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/FileError.js",
        "id": "cordova-plugin-file.FileError",
        "clobbers": [
            "window.FileError"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/FileReader.js",
        "id": "cordova-plugin-file.FileReader",
        "clobbers": [
            "window.FileReader"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/FileSystem.js",
        "id": "cordova-plugin-file.FileSystem",
        "clobbers": [
            "window.FileSystem"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/FileUploadOptions.js",
        "id": "cordova-plugin-file.FileUploadOptions",
        "clobbers": [
            "window.FileUploadOptions"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/FileUploadResult.js",
        "id": "cordova-plugin-file.FileUploadResult",
        "clobbers": [
            "window.FileUploadResult"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/FileWriter.js",
        "id": "cordova-plugin-file.FileWriter",
        "clobbers": [
            "window.FileWriter"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/Flags.js",
        "id": "cordova-plugin-file.Flags",
        "clobbers": [
            "window.Flags"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/LocalFileSystem.js",
        "id": "cordova-plugin-file.LocalFileSystem",
        "clobbers": [
            "window.LocalFileSystem"
        ],
        "merges": [
            "window"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/Metadata.js",
        "id": "cordova-plugin-file.Metadata",
        "clobbers": [
            "window.Metadata"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/ProgressEvent.js",
        "id": "cordova-plugin-file.ProgressEvent",
        "clobbers": [
            "window.ProgressEvent"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/fileSystems.js",
        "id": "cordova-plugin-file.fileSystems"
    },
    {
        "file": "plugins/cordova-plugin-file/www/requestFileSystem.js",
        "id": "cordova-plugin-file.requestFileSystem",
        "clobbers": [
            "window.requestFileSystem"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/resolveLocalFileSystemURI.js",
        "id": "cordova-plugin-file.resolveLocalFileSystemURI",
        "merges": [
            "window"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/ios/FileSystem.js",
        "id": "cordova-plugin-file.iosFileSystem",
        "merges": [
            "FileSystem"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/fileSystems-roots.js",
        "id": "cordova-plugin-file.fileSystems-roots",
        "runs": true
    },
    {
        "file": "plugins/cordova-plugin-file/www/fileSystemPaths.js",
        "id": "cordova-plugin-file.fileSystemPaths",
        "merges": [
            "cordova"
        ],
        "runs": true
    },
    {
        "file": "plugins/cordova-plugin-file-transfer/www/FileTransferError.js",
        "id": "cordova-plugin-file-transfer.FileTransferError",
        "clobbers": [
            "window.FileTransferError"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file-transfer/www/FileTransfer.js",
        "id": "cordova-plugin-file-transfer.FileTransfer",
        "clobbers": [
            "window.FileTransfer"
        ]
    },
    {
        "file": "plugins/nl.x-services.plugins.actionsheet/www/ActionSheet.js",
        "id": "nl.x-services.plugins.actionsheet.ActionSheet",
        "clobbers": [
            "window.plugins.actionsheet"
        ]
    },
    {
        "file": "plugins/nl.x-services.plugins.socialsharing/www/SocialSharing.js",
        "id": "nl.x-services.plugins.socialsharing.SocialSharing",
        "clobbers": [
            "window.plugins.socialsharing"
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
        "file": "plugins/com.dieam.searchbar/www/searchbar.js",
        "id": "com.dieam.searchbar.searchbar",
        "clobbers": [
            "cordova.searchbar"
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
        "file": "plugins/org.nypr.cordova.vlcplugin/www/audioplayer.js",
        "id": "org.nypr.cordova.vlcplugin.AudioPlayer",
        "clobbers": [
            "audioplayer"
        ]
    },
    {
        "file": "plugins/com.drifty.cordova.cameraroll/www/CameraRoll.js",
        "id": "com.drifty.cordova.cameraroll.CameraRoll",
        "clobbers": [
            "CameraRoll"
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
        "file": "plugins/io.litehelpers.cordova.sqlite/www/SQLitePlugin.js",
        "id": "io.litehelpers.cordova.sqlite.SQLitePlugin",
        "clobbers": [
            "SQLitePlugin"
        ]
    },
    {
        "file": "plugins/org.apache.cordova.background-download/www/Promise.js",
        "id": "org.apache.cordova.background-download.Promise"
    },
    {
        "file": "plugins/org.apache.cordova.background-download/www/BackgroundDownloader.js",
        "id": "org.apache.cordova.background-download.BackgroundDownloader",
        "clobbers": [
            "BackgroundTransfer.BackgroundDownloader"
        ]
    },
    {
        "file": "plugins/org.apache.cordova.background-download/www/DownloadOperation.js",
        "id": "org.apache.cordova.background-download.DownloadOperation",
        "clobbers": [
            "DownloadOperation"
        ]
    },
    {
        "file": "plugins/uk.co.whiteoctober.cordova.appversion/www/AppVersionPlugin.js",
        "id": "uk.co.whiteoctober.cordova.appversion.AppVersionPlugin",
        "clobbers": [
            "cordova.getAppVersion"
        ]
    },
    {
        "file": "plugins/com.squerb.cordova.plugins.ios-tab-bar/www/tab-bar.js",
        "id": "com.squerb.cordova.plugins.ios-tab-bar.TabBar",
        "clobbers": [
            "window.TabBar"
        ]
    },
    {
        "file": "plugins/com.telerik.plugins.wkwebview/www/wkwebview.js",
        "id": "com.telerik.plugins.wkwebview.wkwebview",
        "clobbers": [
            "wkwebview"
        ]
    },
    {
        "file": "plugins/cc.fovea.cordova.purchase/www/store-ios.js",
        "id": "cc.fovea.cordova.purchase.InAppPurchase",
        "clobbers": [
            "store"
        ]
    },
    {
        "file": "plugins/com.mallzee.collectionrepeatimage/www/CollectionRepeatImage.js",
        "id": "com.mallzee.collectionrepeatimage.CollectionRepeatImage",
        "clobbers": [
            "window.CollectionRepeatImage"
        ]
    },
    {
        "file": "plugins/com.mallzee.collectionrepeatimage/www/CollectionRepeatImageOptions.js",
        "id": "com.mallzee.collectionrepeatimage.CollectionRepeatImageOptions",
        "clobbers": [
            "window.CollectionRepeatImageOptions"
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
        "file": "plugins/org.apache.cordova.media/www/MediaError.js",
        "id": "org.apache.cordova.media.MediaError",
        "clobbers": [
            "window.MediaError"
        ]
    },
    {
        "file": "plugins/org.apache.cordova.media/www/Media.js",
        "id": "org.apache.cordova.media.Media",
        "clobbers": [
            "window.Media"
        ]
    },
    {
        "file": "plugins/org.apache.cordova.network-information/www/network.js",
        "id": "org.apache.cordova.network-information.network",
        "clobbers": [
            "navigator.connection",
            "navigator.network.connection"
        ]
    },
    {
        "file": "plugins/org.apache.cordova.network-information/www/Connection.js",
        "id": "org.apache.cordova.network-information.Connection",
        "clobbers": [
            "Connection"
        ]
    }
];
module.exports.metadata = 
// TOP OF METADATA
{
    "cordova-plugin-whitelist": "1.0.1-dev",
    "cordova-plugin-chrome-apps-sockets-udp": "1.2.2",
    "com.crewmeister.cordova-ios-backgroundcolor": "0.0.1",
    "hu.dpal.phonegap.plugins.UniqueDeviceID": "1.2.0",
    "com.connectsdk.cordovaplugin": "1.2.0",
    "de.appplant.cordova.plugin.local-notification": "0.8.2dev",
    "com.hutchind.cordova.plugins.launcher": "0.2.2",
    "com.mesmotronic.plugins.fullscreen": "1.0.1",
    "cordova-plugin-file": "3.0.0",
    "cordova-plugin-file-transfer": "1.1.0",
    "nl.x-services.plugins.actionsheet": "1.1.7",
    "nl.x-services.plugins.backgroundaudio": "1.0.1",
    "nl.x-services.plugins.socialsharing": "4.3.18",
    "cordova-plugin-device": "1.0.1",
    "cordova-plugin-webserver": "1.0.3",
    "com.dieam.searchbar": "2.1.6",
    "cordova-plugin-dialogs": "1.1.1",
    "org.nypr.cordova.vlcplugin": "0.1.0",
    "com.drifty.cordova.cameraroll": "0.2.1",
    "cordova-plugin-statusbar": "2.0.0-dev",
    "io.litehelpers.cordova.sqlite": "0.7.11-dev",
    "org.apache.cordova.background-download": "0.0.2",
    "uk.co.whiteoctober.cordova.appversion": "0.1.7",
    "com.squerb.cordova.plugins.ios-tab-bar": "1.0.0",
    "com.telerik.plugins.wkwebview": "0.6.3",
    "cc.fovea.cordova.purchase": "4.0.0",
    "cordova-plugin-crosswalk-webview": "1.4.0-dev",
    "com.mallzee.collectionrepeatimage": "0.0.1",
    "cordova-plugin-chrome-apps-common": "1.0.7",
    "cordova-plugin-chrome-apps-iossocketscommon": "1.0.2",
    "de.appplant.cordova.common.registerusernotificationsettings": "1.0.1",
    "org.apache.cordova.media": "0.2.16",
    "org.apache.cordova.network-information": "0.2.15",
    "org.nypr.cordova.nslogger-cocoalumberjack-connector-plugin": "0.1.0"
}
// BOTTOM OF METADATA
});