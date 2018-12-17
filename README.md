# Jellyfin.Mobile

A Cordova-based mobile app for [Jellyfin](https://github.com/jellyfin/jellyfin/).

> This project currently uses a customized version of the Android platform for Cordova that includes native Android code that will need to be migrated to a plugin or otherwise removed. (See the `com.mb.android` package in `platforms/android/src`.) This currently blocks supporting iOS and updating Cordova and the Android platform for Cordova.

## Building Jellyfin.Mobile from source

> In order to build the cordova project, you may need to grant execute permissions to the file `/hooks/after_prepare/uglify.js`

### Getting Started

0. Clone or download this repository.
0. From the Jellyfin.Mobile directory, install Cordova and other build dependencies via npm.
   ```sh
   npm install
   ```
0. Prepare the Cordova platforms.
   ```sh
   npx cordova prepare
   ```

### Android

#### Prerequisites

* [Android Studio](https://developer.android.com/studio/install)
* Android SDK Build-Tools 23.0.2
* Android SDK Tools 25.2.3
  > Additional information on installing old versions of the SDK Tools can be found [here](https://stackoverflow.com/a/26365848)
* 32-bit versions of the following libraries
  * libgcc
  * libstdc++
  * zlib

#### Build APKs

```sh
npx cordova build android
```

#### Build and Deploy to Emulator

```sh
npx cordova run android
```

### iOS

Currently not supported.
