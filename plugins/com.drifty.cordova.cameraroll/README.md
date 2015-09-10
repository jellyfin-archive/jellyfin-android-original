Cordova Camera Roll
==========================

Created by Max Lynch [@maxlynch](http://twitter.com/maxlynch). For Cordova 3.0+

The Cordova Camera Roll plugin makes it easy to read from the iOS camera roll.

Cordova comes with some camera features such as `navigator.camera.getPicture`. The problem is you have no
control over the experience beyond that. On iOS, for example, a modal slides in that lets you choose a picture.

With this plugin you can get a list of all photos and do whatever you want with them after that.

This plugin only works with iOS and was only tested on iOS 7. We will not support other platforms
with this plugin for a variety of reasons (possibly Android in the future though).


Installation
------------
To install this plugin, follow the [Command-line Interface Guide](http://cordova.apache.org/docs/en/edge/guide_cli_index.md.html#The%20Command-line%20Interface).

If you are not using the Cordova Command-line Interface, follow [Using Plugman to Manage Plugins](http://cordova.apache.org/docs/en/edge/plugin_ref_plugman.md.html).

Usage
-----

To use this plugin, make sure you install it just like any Cordova/PhoneGap plugin.

In Javascript, you can do something like this:

```javascript

// Quickly save a base-64 encoded data uri to the cameraroll.
CamerRoll.saveToCameraRoll(base64String, function() {
}, function(err) {
});

CameraRoll.getPhotos(function(photo) {
  // Photo is a URL pointing to the asset. It's prefixed asset-library:// 
  // So if you are using Angular and ng-src, make sure to whitelist this URL scheme.
  //
  // You can use "photo" directly in an img src or as a background-image in CSS.
  //
  // This callback will be called for each photo in the roll. It's async, yo!
});

```


TODO
-----

Need to do some more testing for failure conditions and permission things.

Also, I want to add block-style reading where you can read in a block of photos at a time, which gives you a little more
control and performance over the loading.
