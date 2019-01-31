#!/bin/bash

# Builds the APK inside the Docker container

set -o errexit
set -o xtrace

cd ${SOURCE_DIR}

# Trap cleanup for latter sections
cleanup() {
    rm -rf platforms/ plugins/ www/
    rm -rf node_modules/
}
trap cleanup EXIT INT

export ANDROID_HOME=${ANDROID_DIR}
export NODE_ENV=production

npm cache verify
sed -i 's/"src/"file:src/g' package.json
npm install
sed -i 's/"file:src/"src/g' package.json
npx gulp
npx cordova platform rm android
npx cordova platform add android
npx cordova build android --release

mkdir -p ${ARTIFACT_DIR}/apk
mmv "${SOURCE_DIR}/platforms/android/build/outputs/apk/android-*.apk" "${ARTIFACT_DIR}/apk/jellyfin-android_#1.apk"
