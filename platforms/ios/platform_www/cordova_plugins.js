cordova.define('cordova/plugin_list', function(require, exports, module) {
module.exports = [
    {
        "file": "plugins/com.connectsdk.cordovaplugin/www/ConnectSDK.js",
        "id": "com.connectsdk.cordovaplugin.ConnectSDK.js",
        "pluginId": "com.connectsdk.cordovaplugin",
        "merges": [
            "navigator.ConnectSDK",
            "ConnectSDK"
        ]
    },
    {
        "file": "plugins/com.hutchind.cordova.plugins.launcher/www/Launcher.js",
        "id": "com.hutchind.cordova.plugins.launcher.Launcher",
        "pluginId": "com.hutchind.cordova.plugins.launcher",
        "clobbers": [
            "plugins.launcher"
        ]
    },
    {
        "file": "plugins/nl.x-services.plugins.actionsheet/www/ActionSheet.js",
        "id": "nl.x-services.plugins.actionsheet.ActionSheet",
        "pluginId": "nl.x-services.plugins.actionsheet",
        "clobbers": [
            "window.plugins.actionsheet"
        ]
    },
    {
        "file": "plugins/com.dieam.searchbar/www/searchbar.js",
        "id": "com.dieam.searchbar.searchbar",
        "pluginId": "com.dieam.searchbar",
        "clobbers": [
            "cordova.searchbar"
        ]
    },
    {
        "file": "plugins/org.nypr.cordova.vlcplugin/www/audioplayer.js",
        "id": "org.nypr.cordova.vlcplugin.AudioPlayer",
        "pluginId": "org.nypr.cordova.vlcplugin",
        "clobbers": [
            "audioplayer"
        ]
    },
    {
        "file": "plugins/com.drifty.cordova.cameraroll/www/CameraRoll.js",
        "id": "com.drifty.cordova.cameraroll.CameraRoll",
        "pluginId": "com.drifty.cordova.cameraroll",
        "clobbers": [
            "CameraRoll"
        ]
    },
    {
        "file": "plugins/org.apache.cordova.background-download/www/Promise.js",
        "id": "org.apache.cordova.background-download.Promise",
        "pluginId": "org.apache.cordova.background-download"
    },
    {
        "file": "plugins/org.apache.cordova.background-download/www/BackgroundDownloader.js",
        "id": "org.apache.cordova.background-download.BackgroundDownloader",
        "pluginId": "org.apache.cordova.background-download",
        "clobbers": [
            "BackgroundTransfer.BackgroundDownloader"
        ]
    },
    {
        "file": "plugins/org.apache.cordova.background-download/www/DownloadOperation.js",
        "id": "org.apache.cordova.background-download.DownloadOperation",
        "pluginId": "org.apache.cordova.background-download",
        "clobbers": [
            "DownloadOperation"
        ]
    },
    {
        "file": "plugins/uk.co.whiteoctober.cordova.appversion/www/AppVersionPlugin.js",
        "id": "uk.co.whiteoctober.cordova.appversion.AppVersionPlugin",
        "pluginId": "uk.co.whiteoctober.cordova.appversion",
        "clobbers": [
            "cordova.getAppVersion"
        ]
    },
    {
        "file": "plugins/com.squerb.cordova.plugins.ios-tab-bar/www/tab-bar.js",
        "id": "com.squerb.cordova.plugins.ios-tab-bar.TabBar",
        "pluginId": "com.squerb.cordova.plugins.ios-tab-bar",
        "clobbers": [
            "window.TabBar"
        ]
    },
    {
        "file": "plugins/com.telerik.plugins.wkwebview/www/wkwebview.js",
        "id": "com.telerik.plugins.wkwebview.wkwebview",
        "pluginId": "com.telerik.plugins.wkwebview",
        "clobbers": [
            "wkwebview"
        ]
    },
    {
        "file": "plugins/com.mallzee.collectionrepeatimage/www/CollectionRepeatImage.js",
        "id": "com.mallzee.collectionrepeatimage.CollectionRepeatImage",
        "pluginId": "com.mallzee.collectionrepeatimage",
        "clobbers": [
            "window.CollectionRepeatImage"
        ]
    },
    {
        "file": "plugins/com.mallzee.collectionrepeatimage/www/CollectionRepeatImageOptions.js",
        "id": "com.mallzee.collectionrepeatimage.CollectionRepeatImageOptions",
        "pluginId": "com.mallzee.collectionrepeatimage",
        "clobbers": [
            "window.CollectionRepeatImageOptions"
        ]
    },
    {
        "file": "plugins/cordova-plugin-chrome-apps-common/events.js",
        "id": "cordova-plugin-chrome-apps-common.events",
        "pluginId": "cordova-plugin-chrome-apps-common",
        "clobbers": [
            "chrome.Event"
        ]
    },
    {
        "file": "plugins/cordova-plugin-chrome-apps-common/errors.js",
        "id": "cordova-plugin-chrome-apps-common.errors",
        "pluginId": "cordova-plugin-chrome-apps-common"
    },
    {
        "file": "plugins/cordova-plugin-chrome-apps-common/stubs.js",
        "id": "cordova-plugin-chrome-apps-common.stubs",
        "pluginId": "cordova-plugin-chrome-apps-common"
    },
    {
        "file": "plugins/cordova-plugin-chrome-apps-common/helpers.js",
        "id": "cordova-plugin-chrome-apps-common.helpers",
        "pluginId": "cordova-plugin-chrome-apps-common"
    },
    {
        "file": "plugins/cordova-plugin-file/www/DirectoryEntry.js",
        "id": "cordova-plugin-file.DirectoryEntry",
        "pluginId": "cordova-plugin-file",
        "clobbers": [
            "window.DirectoryEntry"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/DirectoryReader.js",
        "id": "cordova-plugin-file.DirectoryReader",
        "pluginId": "cordova-plugin-file",
        "clobbers": [
            "window.DirectoryReader"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/Entry.js",
        "id": "cordova-plugin-file.Entry",
        "pluginId": "cordova-plugin-file",
        "clobbers": [
            "window.Entry"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/File.js",
        "id": "cordova-plugin-file.File",
        "pluginId": "cordova-plugin-file",
        "clobbers": [
            "window.File"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/FileEntry.js",
        "id": "cordova-plugin-file.FileEntry",
        "pluginId": "cordova-plugin-file",
        "clobbers": [
            "window.FileEntry"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/FileError.js",
        "id": "cordova-plugin-file.FileError",
        "pluginId": "cordova-plugin-file",
        "clobbers": [
            "window.FileError"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/FileReader.js",
        "id": "cordova-plugin-file.FileReader",
        "pluginId": "cordova-plugin-file",
        "clobbers": [
            "window.FileReader"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/FileSystem.js",
        "id": "cordova-plugin-file.FileSystem",
        "pluginId": "cordova-plugin-file",
        "clobbers": [
            "window.FileSystem"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/FileUploadOptions.js",
        "id": "cordova-plugin-file.FileUploadOptions",
        "pluginId": "cordova-plugin-file",
        "clobbers": [
            "window.FileUploadOptions"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/FileUploadResult.js",
        "id": "cordova-plugin-file.FileUploadResult",
        "pluginId": "cordova-plugin-file",
        "clobbers": [
            "window.FileUploadResult"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/FileWriter.js",
        "id": "cordova-plugin-file.FileWriter",
        "pluginId": "cordova-plugin-file",
        "clobbers": [
            "window.FileWriter"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/Flags.js",
        "id": "cordova-plugin-file.Flags",
        "pluginId": "cordova-plugin-file",
        "clobbers": [
            "window.Flags"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/LocalFileSystem.js",
        "id": "cordova-plugin-file.LocalFileSystem",
        "pluginId": "cordova-plugin-file",
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
        "pluginId": "cordova-plugin-file",
        "clobbers": [
            "window.Metadata"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/ProgressEvent.js",
        "id": "cordova-plugin-file.ProgressEvent",
        "pluginId": "cordova-plugin-file",
        "clobbers": [
            "window.ProgressEvent"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/fileSystems.js",
        "id": "cordova-plugin-file.fileSystems",
        "pluginId": "cordova-plugin-file"
    },
    {
        "file": "plugins/cordova-plugin-file/www/requestFileSystem.js",
        "id": "cordova-plugin-file.requestFileSystem",
        "pluginId": "cordova-plugin-file",
        "clobbers": [
            "window.requestFileSystem"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/resolveLocalFileSystemURI.js",
        "id": "cordova-plugin-file.resolveLocalFileSystemURI",
        "pluginId": "cordova-plugin-file",
        "merges": [
            "window"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/ios/FileSystem.js",
        "id": "cordova-plugin-file.iosFileSystem",
        "pluginId": "cordova-plugin-file",
        "merges": [
            "FileSystem"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file/www/fileSystems-roots.js",
        "id": "cordova-plugin-file.fileSystems-roots",
        "pluginId": "cordova-plugin-file",
        "runs": true
    },
    {
        "file": "plugins/cordova-plugin-file/www/fileSystemPaths.js",
        "id": "cordova-plugin-file.fileSystemPaths",
        "pluginId": "cordova-plugin-file",
        "merges": [
            "cordova"
        ],
        "runs": true
    },
    {
        "file": "plugins/cordova-plugin-file-transfer/www/FileTransferError.js",
        "id": "cordova-plugin-file-transfer.FileTransferError",
        "pluginId": "cordova-plugin-file-transfer",
        "clobbers": [
            "window.FileTransferError"
        ]
    },
    {
        "file": "plugins/cordova-plugin-file-transfer/www/FileTransfer.js",
        "id": "cordova-plugin-file-transfer.FileTransfer",
        "pluginId": "cordova-plugin-file-transfer",
        "clobbers": [
            "window.FileTransfer"
        ]
    },
    {
        "file": "plugins/cordova-plugin-chrome-apps-sockets-udp/sockets.udp.js",
        "id": "cordova-plugin-chrome-apps-sockets-udp.sockets.udp",
        "pluginId": "cordova-plugin-chrome-apps-sockets-udp",
        "clobbers": [
            "chrome.sockets.udp"
        ]
    },
    {
        "file": "plugins/cordova-plugin-device/www/device.js",
        "id": "cordova-plugin-device.device",
        "pluginId": "cordova-plugin-device",
        "clobbers": [
            "device"
        ]
    },
    {
        "file": "plugins/cordova-plugin-dialogs/www/notification.js",
        "id": "cordova-plugin-dialogs.notification",
        "pluginId": "cordova-plugin-dialogs",
        "merges": [
            "navigator.notification"
        ]
    },
    {
        "file": "plugins/cordova-plugin-statusbar/www/statusbar.js",
        "id": "cordova-plugin-statusbar.statusbar",
        "pluginId": "cordova-plugin-statusbar",
        "clobbers": [
            "window.StatusBar"
        ]
    },
    {
        "file": "plugins/cordova-plugin-network-information/www/network.js",
        "id": "cordova-plugin-network-information.network",
        "pluginId": "cordova-plugin-network-information",
        "clobbers": [
            "navigator.connection",
            "navigator.network.connection"
        ]
    },
    {
        "file": "plugins/cordova-plugin-network-information/www/Connection.js",
        "id": "cordova-plugin-network-information.Connection",
        "pluginId": "cordova-plugin-network-information",
        "clobbers": [
            "Connection"
        ]
    },
    {
        "file": "plugins/cordova-plugin-media/www/MediaError.js",
        "id": "cordova-plugin-media.MediaError",
        "pluginId": "cordova-plugin-media",
        "clobbers": [
            "window.MediaError"
        ]
    },
    {
        "file": "plugins/cordova-plugin-media/www/Media.js",
        "id": "cordova-plugin-media.Media",
        "pluginId": "cordova-plugin-media",
        "clobbers": [
            "window.Media"
        ]
    },
    {
        "file": "plugins/cordova-plugin-x-socialsharing/www/SocialSharing.js",
        "id": "cordova-plugin-x-socialsharing.SocialSharing",
        "pluginId": "cordova-plugin-x-socialsharing",
        "clobbers": [
            "window.plugins.socialsharing"
        ]
    },
    {
        "file": "plugins/cc.fovea.cordova.purchase/www/store-ios.js",
        "id": "cc.fovea.cordova.purchase.InAppPurchase",
        "pluginId": "cc.fovea.cordova.purchase",
        "clobbers": [
            "store"
        ]
    }
];
module.exports.metadata = 
// TOP OF METADATA
{
    "com.crewmeister.cordova-ios-backgroundcolor": "0.0.1",
    "com.connectsdk.cordovaplugin": "1.2.0",
    "com.hutchind.cordova.plugins.launcher": "0.2.2",
    "nl.x-services.plugins.actionsheet": "1.1.7",
    "nl.x-services.plugins.backgroundaudio": "1.0.1",
    "cordova-plugin-webserver": "1.0.3",
    "com.dieam.searchbar": "2.1.6",
    "org.nypr.cordova.vlcplugin": "0.1.0",
    "com.drifty.cordova.cameraroll": "0.2.1",
    "org.apache.cordova.background-download": "0.0.2",
    "uk.co.whiteoctober.cordova.appversion": "0.1.7",
    "com.squerb.cordova.plugins.ios-tab-bar": "1.0.0",
    "com.telerik.plugins.wkwebview": "0.6.3",
    "com.mallzee.collectionrepeatimage": "0.0.1",
    "cordova-plugin-chrome-apps-common": "1.0.7",
    "cordova-plugin-chrome-apps-iossocketscommon": "1.0.2",
    "org.nypr.cordova.nslogger-cocoalumberjack-connector-plugin": "0.1.0",
    "cordova-plugin-file": "3.0.0",
    "cordova-plugin-file-transfer": "1.4.0",
    "cordova-plugin-chrome-apps-sockets-udp": "1.2.2",
    "cordova-plugin-device": "1.1.0",
    "cordova-plugin-dialogs": "1.2.0",
    "cordova-plugin-statusbar": "2.0.0",
    "cordova-plugin-network-information": "1.1.0",
    "cordova-plugin-media": "1.0.1",
    "cordova-plugin-x-socialsharing": "5.0.7",
    "cc.fovea.cordova.purchase": "4.0.0",
    "cordova-plugin-crosswalk-webview": "1.5.0"
}
// BOTTOM OF METADATA
});