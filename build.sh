#!/bin/bash

# Builds the APK inside the Docker container

set -o errexit

cd ${SOURCE_DIR}

export NODE_ENV=production
#export NODE_ENV=development

npm cache verify
npm install
npx gulp
npx cordova prepare

export ANDROID_HOME=${ANDROID_DIR}
npx cordova build android --release
#npx cordova build android --debug

mkdir -p ${ARTIFACT_DIR}/apk
mmv "${SOURCE_DIR}/platforms/android/build/outputs/apk/android-*.apk" "${ARTIFACT_DIR}/apk/jellyfin-android_#1.apk"
