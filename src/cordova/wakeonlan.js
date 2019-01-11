// TODO: This is non-functional
define([], function() {
    "use strict";

    function send(info) {
        return Promise.reject();
    }

    function isSupported() {
        return false;
    }

    return {
        send: send,
        isSupported: isSupported
    };
});
