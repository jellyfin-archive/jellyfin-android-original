define([], function () {
    'use strict';

    var currentResolve;

    window.ServerDiscoveryCallback = function (result) {
    };

    return {
        findServers: function (timeoutMs) {
            return Promise.reject();
        }
    };

});