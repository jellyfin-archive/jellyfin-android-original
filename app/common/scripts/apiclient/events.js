(function (globalScope) {

    globalScope.Events = {

        on: function (obj, eventName, fn) {

            Logger.log('event.on ' + eventName);
        },

        off: function (obj, eventName, fn) {

            Logger.log('event.off ' + eventName);
        },

        trigger: function (obj, eventName, params) {
            Logger.log('event.trigger ' + eventName);
        }
    };

})(window); 