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
        RELEASE_OUTPUT_DIR="release"
    ;;
    'unminified')
        RELEASE_SUFFIX="unminified_"
        NODE_ENV="development"
        RFLAG="--release"
        RELEASE_OUTPUT_DIR="release"
    ;;
    'foss')
        RELEASE_SUFFIX="foss_"
        NODE_ENV="production"
        RFLAG="--release"
        RELEASE_OUTPUT_DIR="release"
    ;;
    'debug')
        RELEASE_SUFFIX=""
        NODE_ENV="development"
        RFLAG=""
        RELEASE_OUTPUT_DIR="debug"
    ;;
esac

# Export environment variables
export ANDROID_HOME=${ANDROID_DIR}
export NODE_ENV

# Move to source directory
pushd ${SOURCE_DIR}

# Install dependencies
npm cache verify
npm install
npx gulp
npx cordova prepare

if [ ${RELEASE} == 'foss' ]
then
    npx cordova plugin rm cordova-plugin-chromecast
fi

# Build APK
npx cordova build android ${RFLAG}

# Move the artifacts out
mkdir -p ${ARTIFACT_DIR}/apk
mmv "${SOURCE_DIR}/platforms/android/app/build/outputs/apk/${RELEASE_OUTPUT_DIR}/app-*.apk" "${ARTIFACT_DIR}/apk/jellyfin-android_${RELEASE_SUFFIX}#1.apk"
