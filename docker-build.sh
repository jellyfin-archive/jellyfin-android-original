#!/bin/bash

# Builds the APK inside the Docker container

set -o errexit
set -o xtrace

# Move to source directory
pushd ${SOURCE_DIR}

# Export environment variables
export ANDROID_HOME=${ANDROID_DIR}
export NODE_ENV=production

# Install dependencies
npm cache verify
sed -i 's/"src/"file:src/g' package.json
npm install
sed -i 's/"file:src/"src/g' package.json
npx gulp
npx cordova platform rm android
npx cordova platform add android

# Build APK
npx cordova build android --release

# Move the artifacts out
mkdir -p ${ARTIFACT_DIR}/apk
mmv "${SOURCE_DIR}/platforms/android/build/outputs/apk/android-*.apk" "${ARTIFACT_DIR}/apk/jellyfin-android_#1.apk"
