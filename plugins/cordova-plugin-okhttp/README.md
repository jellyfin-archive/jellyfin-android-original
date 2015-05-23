# cordova-plugin-okhttp

Cordova plugin that provides a shared `OkHttpClient` for other plugins to use,
as well as makes `URL.open()` use the client under-the-hood.

This will not have any affect on requests made by from within the webview (e.g.
XMLHttpRequest). However, it does change the network stack used by the
FileTransfer plugin.

Plugin relies on building with Gradle, and so requires cordova-android@4.0.0 or greater.

## Installation:

    cordova plugin add https://github.com/MobileChromeApps/cordova-plugin-okhttp.git
