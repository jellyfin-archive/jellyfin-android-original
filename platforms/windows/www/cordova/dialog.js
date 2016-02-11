define([], function () {
    return function (options) {

        var title = options.title;
        var message = options.message;
        var buttons = options.buttons;
        var callback = options.callback;

        navigator.notification.confirm(message, function (index) {

            // The callback takes the argument buttonIndex (Number), which is the index of the pressed button. 
            // Note that the index uses one-based indexing, so the value is 1, 2, 3, etc.
            if (!index) {
                callback(-1);
            } else {
                callback(index - 1);
            }

        }, title, buttons.join(','));
    };
});