<h1 align="center">Jellyfin Android</h1>
<h3 align="center">Part of the <a href="https://jellyfin.org">Jellyfin Project</a></h3>

---

<p align="center">
<img alt="Logo Banner" src="https://raw.githubusercontent.com/jellyfin/jellyfin-ux/master/branding/SVG/banner-logo-solid.svg?sanitize=true"/>
<br/>
<br/>
<a href="https://github.com/jellyfin/jellyfin-android">
<img alt="GPL 2.0 License" src="https://img.shields.io/github/license/jellyfin/jellyfin-android.svg"/>
</a>
<a href="https://github.com/jellyfin/jellyfin-android/releases">
<img alt="Current Release" src="https://img.shields.io/github/release/jellyfin/jellyfin-android.svg"/>
</a>
<a href="https://translate.jellyfin.org/projects/jellyfin/jellyfin-web/?utm_source=widget">
<img alt="Translation Status" src="https://translate.jellyfin.org/widgets/jellyfin/-/jellyfin-web/svg-badge.svg"/>
</a>
<br/>
<a href="https://opencollective.com/jellyfin">
<img alt="Donate" src="https://img.shields.io/opencollective/all/jellyfin.svg?label=backers"/>
</a>
<a href="https://features.jellyfin.org">
<img alt="Feature Requests" src="https://img.shields.io/badge/fider-vote%20on%20features-success.svg"/>
</a>
<a href="https://forum.jellyfin.org">
<img alt="Discuss on our Forum" src="https://img.shields.io/discourse/https/forum.jellyfin.org/users.svg"/>
</a>
<a href="https://matrix.to/#/+jellyfin:matrix.org">
<img alt="Chat on Matrix" src="https://img.shields.io/matrix/jellyfin:matrix.org.svg?logo=matrix"/>
</a>
<a href="https://www.reddit.com/r/jellyfin/">
<img alt="Join our Subreddit" src="https://img.shields.io/badge/reddit-r%2Fjellyfin-%23FF5700.svg"/>
</a>
</p>

Jellyfin Mobile is an Android app built with Cordova. We welcome all contributions and pull requests! If you have a larger feature in mind please open an issue so we can discuss the implementation before you start. Even though the client is a web wrapper there are still lots of improvements and bug fixes that can be accomplished with Java.
For APKs, see <a href="https://github.com/jellyfin/jellyfin-android/releases">releases page</a> where you can choose between the regular APK and a debugging version (we're also working on a libre version that doesn't depend on Google Play Service, but it's <a href="https://github.com/jellyfin/jellyfin-android/issues/327">not ready yet</a>).

Most of the translations can be found in the web client since it's the base for the Android client as well. Translations can be improved very easily from our Weblate instance linked above. Look through the following graphic to see if your native language could use some work!

<a href="https://translate.jellyfin.org/engage/jellyfin/?utm_source=widget">
<img alt="Detailed Translation Status" src="https://translate.jellyfin.org/widgets/jellyfin/-/jellyfin-web/multi-auto.svg"/>
</a>

## Build Process

### Dependencies

- Android SDK
- Yarn or NPM

### Getting Started

1. Clone or download this repository

   ```sh
   git clone https://github.com/jellyfin/jellyfin-android.git
   cd jellyfin-android
   ```

2. Install Cordova and other build dependencies in the project directory via npm

   ```sh
   npm install
   ```

3. Prepare the Cordova platforms

   ```sh
   npx cordova prepare
   ```

### Prepare Environment

If any changes are made to `src/`, the `www/` directory will need to be rebuilt using the following command.

```sh
npx gulp
```

Set the `NODE_ENV` environment variable to `development` if you want to copy the source files without minification. The `JELLYFIN_WEB_DIR` environment variable can be used to override the location of `jellyfin-web`.

### Build APK

You can use the `-d` flag if you want to build a debug version. A release version will need to be signed before it will run.

```sh
npx cordova build android
```

### Deploy to Emulator

```sh
npx cordova run android
```

## Docker

You can optionally use Docker to build the APK to avoid the above requirements.

Replace release with the desired build flavor (production, libre, or debug) and output with the directory you want the APKs sent on completion.

```sh
docker build . -t "jellyfin-android"
docker run --rm -e "RELEASE=${release}" -v "${output}:/dist" "jellyfin-android"
```

## Troubleshooting

### Failed to find `ANDROID_HOME`

Cordova needs that environment vairable set to the location of your Android SDK to build the app. If you are using linux it will be somewhere in your home directory.

### Command failed with exit code EACCES

Android Studio will write over `gradlew` occasionally but doesn't set it as executable. Go to `platforms/android` and set that file as executable to fix the build for Cordova.

### File not Found `index.html`

Make sure the web source has been properly copied to `www` using gulp. If you are using Android Studio you have to build with cordova at least once so the files are copied into the android source files.
