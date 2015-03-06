(function (globalScope) {

    globalScope.Events = {

        on: function (obj, eventName, selector, fn) {

            Logger.log('event.on ' + eventName);
            bean.on(obj, eventName, selector, fn);
        },

        off: function (obj, eventName, selector, fn) {

            Logger.log('event.off ' + eventName);
            bean.off(obj, eventName, selector);
        },

        trigger: function (obj, eventName, params) {
            Logger.log('event.trigger ' + eventName);
            bean.fire(obj, eventName, params);
        }
    };

})(window);