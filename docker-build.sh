#!/bin/bash

# Builds the APK inside the Docker container

set -o errexit
set -o xtrace

# Handle the release type
case ${RELEASE} in
    'production')
        RELEASE_SUFFIX=""
        NODE_ENV="production"
        RFLAG="--release"
    ;;
    'unminified')
        RELEASE_SUFFIX="unminified_"
        NODE_ENV="development"
        RFLAG="--release"
    ;;
    'debug')
        RELEASE_SUFFIX=""
        NODE_ENV="development"
        RFLAG=""
    ;;
esac

# Export environment variables
export ANDROID_HOME=${ANDROID_DIR}
export NODE_ENV

# Move to source directory
pushd ${SOURCE_DIR}

# Install dependencies
npm cache verify
sed -i 's/"src/"file:src/g' package.json
npm install
sed -i 's/"file:src/"src/g' package.json
npx gulp
npx cordova platform rm android
npx cordova platform add android

# Build APK
npx cordova build android ${RFLAG}

# Move the artifacts out
mkdir -p ${ARTIFACT_DIR}/apk
mmv "${SOURCE_DIR}/platforms/android/build/outputs/apk/android-*.apk" "${ARTIFACT_DIR}/apk/jellyfin-android_${RELEASE_SUFFIX}#1.apk"
