define([], function () {
    'use strict';

    return {
        fileExists: function (path) {
            var exists = NativeFileSystem.fileExists(path);
            console.log('fileExists: ' + exists + ' - path: ' + path);
            if (exists) {
                return Promise.resolve();
            }

            return Promise.reject();
        },
        directoryExists: function (path) {
            return Promise.reject();
        }
    };
});