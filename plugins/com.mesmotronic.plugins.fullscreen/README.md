Full Screen Plugin for Cordova
==============================

Plugin for Cordova (or PhoneGap) 3.0+ to enable true full screen on Android, ported from our popular [ANE for Adobe AIR](https://github.com/mesmotronic/air-fullscreen-ane).

Released under BSD license; see LICENSE for details.

How does it work?
-----------------

This plugin enables developers to offer users a true full screen experience in their Cordova and PhoneGap apps for Android.

Using Android 4.0+, you can use true full screen in "lean mode", the way you see in apps like YouTube, expanding the app right to the edges of the screen, hiding the status and navigation bars until the user next interacts. This is ideally suited to video or cut-scene content.

In Android 4.4+, however, you can now enter true full screen, fully interactive immersive mode. In this mode, your app will remain in true full screen until you choose otherwise; users can swipe down from the top of the screen to temporarily display the system UI.

If you find that the plugin doesn't work as you might like on a specific device or Android configuration you're using, don't forget that this project is open source, so feel free to fork the project and adapt it to work in any way you like!

Installation
------------

**Cordova**

`cordova plugin add https://github.com/mesmotronic/cordova-fullscreen-plugin.git`

**PhoneGap**

`phonegap local plugin add https://github.com/mesmotronic/cordova-fullscreen-plugin.git`

Code example
------------

Using the plugin in your app couldn't be easier:

```js
function successFunction()
{
    console.log("It worked!");
}

function errorFunction(error)
{
    console.log(error);
}

function trace(value)
{
	console.log(value);
}

// Is this plugin supported?
AndroidFullScreen.isSupported();

// Is immersive mode supported?
AndroidFullScreen.isImmersiveModeSupported(successFunction, errorFunction);

// The width of the screen in immersive mode
AndroidFullScreen.immersiveWidth(trace, errorFunction);

// The height of the screen in immersive mode
AndroidFullScreen.immersiveHeight(trace, errorFunction);

// Hide system UI until user interacts
AndroidFullScreen.leanMode(successFunction, errorFunction);

// Show system UI
AndroidFullScreen.showSystemUI(successFunction, errorFunction);

// Extend your app underneath the system UI (Android 4.4+ only)
AndroidFullScreen.showUnderSystemUI(successFunction, errorFunction);

// Hide system UI and keep it hidden (Android 4.4+ only)
AndroidFullScreen.immersiveMode(successFunction, errorFunction);

```

All methods will call the successFunction callback if the action was successful and the errorFunction if it wasn't (or isn't supported); if you're using the plugin in an app for a platform other than Android, all methods will fail.

The `immersiveWidth` and `immersiveHeight` properties return the screen width and height available in immersive mode (or with the system UI hidden), where supported.

Getting the immersive screen size
---------------------------------

You can use the `immersiveWidth` and `immersiveHeight` methods to find out the dimensions of the screen with the system UI hidden, regardless of the current screen state.
