define([], function () {

    return {
        openUrl: function (url) {
            MainActivity.launchIntent(url, null);
        },
        canExec: false,
        exec: function (options) {
            // options.path
            // options.arguments
            return Promise.reject();
        }
    };
});