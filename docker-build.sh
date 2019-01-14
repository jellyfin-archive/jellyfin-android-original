#!/usr/bin/env bash

set -o errexit
set -o xtrace

dockerfile="Dockerfile"
image_name="jellyfin-android-apkbuild"
package_temporary_dir="$( mktemp -d )"
current_user="$( whoami )"

# Trap cleanup for latter sections
cleanup() {
    set +o errexit
    docker image rm ${image_name} --force
    rm -rf "${package_temporary_dir}"
}
trap cleanup EXIT INT

# Initialize the submodules
git submodule update --init

# Check out the proper jellyfin-web branch
if [[ ${1} == '--dev' || ${1} == '-d' ]]; then
    pushd src/jellyfin-web
    git checkout dev
    popd
else
    pushd src/jellyfin-web
    git checkout master
    popd
fi

# Set up the build environment docker image
docker build . -t "${image_name}" -f ./${dockerfile}
# Build the APKs and copy out to ${package_temporary_dir}
docker run --rm -v "${package_temporary_dir}:/dist" "${image_name}"
# Correct ownership on the APKs (as current user, then as root if that fails)
chown -R "${current_user}" "${package_temporary_dir}" &>/dev/null \
  || sudo chown -R "${current_user}" "${package_temporary_dir}" &>/dev/null
# Move the APKs to the parent directory
mv "${package_temporary_dir}"/apk/*.apk ../
