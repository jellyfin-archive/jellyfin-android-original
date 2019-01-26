#!/usr/bin/env bash

set -o errexit

dockerfile="Dockerfile"
image_name="jellyfin-android-apkbuild"

# Initialize the submodules
git submodule update --init

usage() {
    echo -e "Usage:"
    echo -e " $0 [--web/-w <branch>]"
    echo -e "The web branch defaults to 'origin/master'; specify any valid tag or branch for the 'jellyfin-web' repository."
    exit 1
}

# Check out the proper jellyfin-web branch
if [[ ${1} == '--web' || ${1} == '-w' ]]; then
    if [[ -n ${2} ]]; then
        web_branch="${2}"
    else
        usage
    fi
else
    web_branch="origin/master"
fi

pushd src/jellyfin-web
git fetch --all
git checkout ${web_branch} || usage
popd

set -o xtrace
package_temporary_dir="$( mktemp -d )"
current_user="$( whoami )"

# Trap cleanup for latter sections
cleanup() {
    set +o errexit
    docker image rm ${image_name} --force
    rm -rf "${package_temporary_dir}"
}
trap cleanup EXIT INT

# Set up the build environment docker image
docker build . -t "${image_name}" -f ./${dockerfile}
# Build the APKs and copy out to ${package_temporary_dir}
docker run --rm -v "${package_temporary_dir}:/dist" "${image_name}"
# Correct ownership on the APKs (as current user, then as root if that fails)
chown -R "${current_user}" "${package_temporary_dir}" &>/dev/null \
  || sudo chown -R "${current_user}" "${package_temporary_dir}" &>/dev/null
# Move the APKs to the parent directory
mv "${package_temporary_dir}"/apk/*.apk ../
