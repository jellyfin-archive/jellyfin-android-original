#!/bin/bash

# Builds the APK inside the Docker container

set -o errexit

cd /repo

npm cache verify
npm install
npx gulp
npx cordova prepare

export ANDROID_HOME=/usr/lib/android-sdk
npx cordova build android --release

mkdir -p /dist/apk
mmv "/repo/platforms/android/build/outputs/apk/android-*.apk" "/dist/apk/jellyfin-android_#1.apk"
