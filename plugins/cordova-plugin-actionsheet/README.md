# ActionSheet Cordova / PhoneGap Plugin
by [Eddy Verbruggen](http://twitter.com/eddyverbruggen)

## 0. Index

1. [Description](#1-description)
2. [Screenshots](#2-screenshots)
3. [Installation](#3-installation)
4. [Usage](#4-usage)
5. [Credits](#5-credits)
6. [License](#6-license)

## 1. Description

Show a sheet of options the user can choose from.

* Compatible with [Cordova Plugman](https://github.com/apache/cordova-plugman)
* iOS uses the native `UIActionSheet`
* Android uses the native `AlertDialog`
* WP8 uses the native `Popup`

## 2. Screenshots

iOS

<img src="screenshots/ios/ios-share.png" width="235"/>&nbsp;
<img src="screenshots/ios/ios-delete.png" width="235"/>&nbsp;
<img src="screenshots/ios/ios-logout.png" width="235"/>


Android

<img src="screenshots/android/android-share.png" width="235"/>&nbsp;
<img src="screenshots/android/android-delete.png" width="235"/>&nbsp;
<img src="screenshots/android/android-logout.png" width="235"/>

Windows Phone 8

<img src="screenshots/wp8/wp8-share.jpg" width="235"/>&nbsp;
<img src="screenshots/wp8/wp8-delete.jpg" width="235"/>&nbsp;
<img src="screenshots/wp8/wp8-logout.jpg" width="235"/>

## 3. Installation

### Automatically (CLI / Plugman)
```
$ cordova plugin add cordova-plugin-actionsheet
$ cordova prepare
```

ActionSheet.js is brought in automatically. There is no need to change or add anything in your html.

### PhoneGap Build
ActionSheet  works with PhoneGap build too! Just add the following xml to your `config.xml` to always use the latest version of this plugin:
```xml
<plugin name="cordova-plugin-actionsheet" />
```

ActionSheet.js is brought in automatically. Make sure though you include a reference to cordova.js in your index.html's head:
```html
<script type="text/javascript" src="cordova.js"></script>
```

## 4. Usage

### show

Check the [demo code](demo) to get you going quickly,
or copy-paste some of the code below to replicate the ActionSheets of the screenshots above.

Also, wait for `deviceready` to fire before using plugins in general!

```js
  var callback = function(buttonIndex) {
    setTimeout(function() {
      // like other Cordova plugins (prompt, confirm) the buttonIndex is 1-based (first button is index 1)
      alert('button index clicked: ' + buttonIndex);
    });
  };

  function testShareSheet() {
    var options = {
        androidTheme: window.plugins.actionsheet.ANDROID_THEMES.THEME_DEVICE_DEFAULT_LIGHT, // default is THEME_TRADITIONAL
        title: 'What do you want with this image?',
        subtitle: 'Choose wisely, my friend', // supported on iOS only
        buttonLabels: ['Share via Facebook', 'Share via Twitter'],
        androidEnableCancelButton : true, // default false
        winphoneEnableCancelButton : true, // default false
        addCancelButtonWithLabel: 'Cancel',
        addDestructiveButtonWithLabel : 'Delete it',
        position: [20, 40], // for iPad pass in the [x, y] position of the popover
        destructiveButtonLast: true // you can choose where the destructive button is shown
    };
    // Depending on the buttonIndex, you can now call shareViaFacebook or shareViaTwitter
    // of the SocialSharing plugin (https://github.com/EddyVerbruggen/SocialSharing-PhoneGap-Plugin)
    window.plugins.actionsheet.show(options, callback);
  };

  function testDeleteSheet() {
    var options = {
        'addCancelButtonWithLabel': 'Cancel',
        'addDestructiveButtonWithLabel' : 'Delete note'
    };
    window.plugins.actionsheet.show(options, callback);
  };

  function testLogoutSheet() {
    var options = {
        'buttonLabels': ['Log out'],
        'androidEnableCancelButton' : true, // default false
        'winphoneEnableCancelButton' : true, // default false
        'addCancelButtonWithLabel': 'Cancel'
    };
    window.plugins.actionsheet.show(options, callback);
  };
```

On iOS, you can also position the actionSheet origin by adding `position: [100, 200]`

### hide

If for some reason you want to hide the actionsheet programmatically, do this:
```js
  // options and callbacks are optional, so either approach will work:
  window.plugins.actionsheet.hide();
  window.plugins.actionsheet.hide({}, onSuccess, onError);
```

## 5. Credits
iOS and WP8 code: [Eddy Verbruggen](https://github.com/EddyVerbruggen)

Android code: mostly [Brill Papping](https://github.com/bpappin)


## 6. Change history
* 2.3.1 Added `subtitle` (iOS) and `destructiveButtonLast` preferences. Also, iOS now uses the newer `UIAlertController` instead of `UIActionSheet`.
* 2.2.2 OK, 2.2.1 has issues with Russian and the like, so reverted. Just add `<meta charset="utf-8" />` to your html file.
* 2.2.1 Encoding of diacritical characters fixed on iOS, so you can now use `Español` as a title or button label.
* 1.1.6 You can now set the iOS actionSheet origin position (uses the iOS `actionSheet.showFromRect` method)
* 1.1.2 You can now select a theme for your Android popup, see the first example above

## 7. License

[The MIT License (MIT)](http://www.opensource.org/licenses/mit-license.html)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
