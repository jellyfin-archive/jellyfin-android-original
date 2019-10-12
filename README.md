<h1 align="center">Jellyfin Mobile</h1>
<h3 align="center">Part of the <a href="https://jellyfin.media">Jellyfin Project</a></h3>

<p align="center">
Jellyfin Mobile is an Android app built with Cordova.
</p>

## Build Process

### Getting Started

1. Clone or download this repository.
   ```sh
   git clone https://github.com/jellyfin/jellyfin-android.git
   cd jellyfin-android
   ```
2. Install Cordova and other build dependencies via npm in the project directory.
   ```sh
   npm install -g cordova
   ```
3. Prepare the Cordova platforms.
   ```sh
   cordova platform add android
   ```

### Prepare Interface

If any changes are made to `src/`, the `www/` directory will need to be rebuilt using the following command.

```sh
npx gulp
```

> If `NODE_ENV=development` is set in the environment, then the source files will be copied without being minified.

> The `JELLYFIN_WEB_DIR` environment variable can be used to override the location of `jellyfin-web`.

### Build APK

You can use the `-d` flag if you want to build a debug version. A release version will need to be signed before it will run.

```sh
npx cordova build android
```

### Deploy to Emulator

```sh
npx cordova run android
```

## Troubleshooting

### Failed to find `ANDROID_HOME`

Cordova needs that environment vairable set to the location of your Android SDK to build the app. If you are using linux it will be somewhere in your home directory.

### Command failed with exit code EACCES

Android Studio will write over `gradlew` occasionally but doesn't set it as executable. Go to `platforms/android` and set that file as executable to fix the build for Cordova.

### File not Found `index.html`

Make sure the web source has been properly copied to `www` using gulp. If you are using Android Studio you have to build with cordova at least once so the files are copied into the android source files.
