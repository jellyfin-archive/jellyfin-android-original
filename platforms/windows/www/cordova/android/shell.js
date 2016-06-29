define([], function () {

    return {
        openUrl: function (url) {
            window.plugins.launcher.launch({
                uri: url
            });
        },
        canExec: false,
        exec: function (options) {
            // options.path
            // options.arguments
            return Promise.reject();
        }
    };
});