/**
 * src/cordova/filerepository.js
 * Part of the Jellyfin project (https://jellyfin.media)
 *
 *    This Source Code Form is subject to the terms of the Mozilla Public
 *    License, v. 2.0. If a copy of the MPL was not distributed with this file,
 *    You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 *    All copyright belongs to the Jellyfin contributors; a full list can
 *    be found in the file CONTRIBUTORS.md
 *
 *    Alternatively, the contents of this file may be used under the terms
 *    of the GNU General Public License Version 2 or later, as described below:
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

// TODO: This is non-functional
define([], function() {
    "use strict";

    function getValidFileName(path) {
        return path;
    }

    function getFullLocalPath(pathArray) {
        return pathArray.join("/");
    }

    function getPathFromArray(pathArray) {
        return pathArray.join("/");
    }

    function deleteFile(path) {
        return Promise.resolve();
    }

    function deleteDirectory(path) {
        return Promise.resolve();
    }

    function fileExists(path) {
        return Promise.resolve();
    }

    function getItemFileSize(path) {
        return Promise.resolve(0);
    }

    function getImageUrl(pathParts) {
        return pathParts.join("/");
    }

    return {
        getValidFileName: getValidFileName,
        getFullLocalPath: getFullLocalPath,
        getPathFromArray: getPathFromArray,
        deleteFile: deleteFile,
        deleteDirectory: deleteDirectory,
        fileExists: fileExists,
        getItemFileSize: getItemFileSize,
        getImageUrl: getImageUrl
    };
});
