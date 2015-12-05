define([], function () {
    return function (options) {

        var title = options.title;
        var message = options.message;
        var buttons = options.buttons;
        var callback = options.callback;

        navigator.notification.confirm(message, function (index) {

            callback(index);

        }, title, buttons.join(','));
    };
});