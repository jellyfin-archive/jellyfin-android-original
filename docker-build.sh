#!/bin/bash

# Builds the APK inside the Docker container

set -o errexit
set -o xtrace

# Handle the release type
RELEASE="${RELEASE:-debug}"
case "${RELEASE}" in
    'production')
        RELEASE_SUFFIX=""
        NODE_ENV="production"
        RFLAG="--release"
        RELEASE_OUTPUT_DIR="release"
        ;;
    'libre')
        RELEASE_SUFFIX="libre_"
        NODE_ENV="production"
        RFLAG="--release"
        RELEASE_OUTPUT_DIR="release"
        ;;
    'debug')
        RELEASE_SUFFIX=""
        NODE_ENV="development"
        RFLAG="--debug"
        RELEASE_OUTPUT_DIR="debug"
        ;;
    *)
        echo "error: release type must be production, libre, or debug"
        exit 1
esac

# Export environment variables
export ANDROID_HOME="${ANDROID_DIR}"
export NODE_ENV

# Move to source directory
pushd "${SOURCE_DIR}"

# Install dependencies
npm cache verify
npm config set unsafe-perm true
npm ci
npx gulp
npx cordova telemetry off
npx cordova prepare

if [ "${RELEASE}" == 'libre' ]
then
    npx cordova plugin rm cordova-plugin-chromecast
fi

# Build APK
npx cordova build android "${RFLAG}"

# Move the artifacts
mmv "${SOURCE_DIR}/platforms/android/app/build/outputs/apk/*/jellyfin-android_*.apk" "${ARTIFACT_DIR}"
