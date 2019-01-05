/**
 * src/cordova/transfermanager.js
 * Part of the Jellyfin project (https://jellyfin.media)
 *
 * All copyright belongs to the Jellyfin contributors; a full list can
 * be found in the file CONTRIBUTORS.md
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

// TODO: This is non-functional
define([], function() {
    "use strict";

    function downloadFile(url, folder, localItem, imageUrl) {
        return Promise.reject();
    }

    function downloadSubtitles(url, folder, fileName) {
        return Promise.reject();
    }

    function downloadImage(url, folder, fileName) {
        return Promise.reject();
    }

    function resyncTransfers() {
        return Promise.resolve();
    }

    function getDownloadItemCount() {
        return Promise.resolve(0);
    }

    return {
        downloadFile: downloadFile,
        downloadSubtitles: downloadSubtitles,
        downloadImage: downloadImage,
        resyncTransfers: resyncTransfers,
        getDownloadItemCount: getDownloadItemCount
    };
});
