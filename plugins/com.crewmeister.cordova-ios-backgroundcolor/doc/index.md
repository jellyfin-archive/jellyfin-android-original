# com.crewmeister.cordova.ios-backgroundcolor

> This plugin activates the `BackgroundColor` preference for iOS.


## Installation

    cordova plugin add https://github.com/crewmeister/cordova.ios-backgroundcolor


Preferences
-----------

#### config.xml

-  __BackgroundColor__ Set the app's background color. Supports a four-byte hex
   value, with the first byte representing the alpha channel, and standard RGB
   values for the following three bytes. This example specifies blue:

        <preference name="BackgroundColor" value="0xff0000ff"/>

Permissions
-----------

#### config.xml

            <feature name="BackgroundColor">
                <param name="ios-package" value="CDVBackgroundColor" onload="true" />
            </feature>


Description
-----------

The background color is visible within the overflow area. By default, it is set
to black and cannot be changed without changing the MainViewController.m file
within the iOS platform files. Since there is already a configuration parameter
for this setting, we may just as well utilize that and stop messing with
ObjectiveC.


Supported Platforms
-------------------

- iOS

