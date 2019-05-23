
function invokeMethod(method, args, successCallback, errorCallback) {
    return window.NativeShell.invokeMethod('exoPlayer.' + method, args, successCallback, errorCallback);
}

module.exports = {
    loadPlayer: function (url) {
        invokeMethod('loadPlayer', [url]);
    },

    checkTracksSupport: function (videoTracks, audioTracks, subtitleTracks) {
        return new Promise(function (resolve) {

            let successCallback = function (result) {
                resolve({
                    videoTracks: result.videoTracks,
                    audioTracks: result.audioTracks,
                    subtitleTracks: result.subtitleTracks
                });
            };

            let errorCallback = function () {
                resolve(false);
            };

            invokeMethod('checkTracksSupport', [videoTracks, audioTracks, subtitleTracks], successCallback, errorCallback);
        });
    }
};