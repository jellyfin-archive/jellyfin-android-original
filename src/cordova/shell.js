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
            window.NativeShell.enableFullscreen(function() {}, function() {});
        },
        disableFullscreen: function () {
            window.NativeShell.disableFullscreen(function() {}, function() {});
        }
    };
});