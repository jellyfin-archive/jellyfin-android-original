# Emby.Mobile
A Cordova-based mobile app for Emby

iOS Build Notes:

The project adds the LG Connect SDK as a sub-project:

https://github.com/ConnectSDK/Connect-SDK-iOS

The following changes were made to the Connect project:

* Imported Google Cast (instructions on Connect SDK install steps)
* Removed the entire Amazon Fling folder
* Removed all GCDWebServer .m files, as the implementation is already supplied by the cordova web server plugin
