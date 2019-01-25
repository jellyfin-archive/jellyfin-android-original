define([], function () {

    return {
        openUrl: function (url) {
            // TODO reimplement
        },
        canExec: false,
        exec: function (options) {
            // options.path
            // options.arguments
            return Promise.reject();
        },
        enableFullscreen: function () {
            MainActivity.enableFullscreen();
        },
        disableFullscreen: function () {
            MainActivity.disableFullscreen();
        }
    };
});