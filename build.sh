#!/bin/bash

# Builds the APK inside the Docker container

set -o errexit
set -o xtrace

cd ${SOURCE_DIR}

# Trap cleanup for latter sections
cleanup() {
    rm -rf node_modules/ platforms/ plugins/ www/
}
trap cleanup EXIT INT

export NODE_ENV=production
#export NODE_ENV=development

npm --version
npm cache verify
sed -i 's/src/file:src/g' package.json
npm install
sed -i 's/file:src/src/g' package.json
npx gulp
npx cordova prepare

export ANDROID_HOME=${ANDROID_DIR}
npx cordova build android --release
#npx cordova build android --debug

mkdir -p ${ARTIFACT_DIR}/apk
mmv "${SOURCE_DIR}/platforms/android/build/outputs/apk/android-*.apk" "${ARTIFACT_DIR}/apk/jellyfin-android_#1.apk"
