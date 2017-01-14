define([], function () {
    'use strict';

    var currentResolve;

    window.ServerDiscoveryCallback = function (result) {
        var resolve = currentResolve;
        if (resolve) {

            resolve(result);
            currentResolve = null;
        }
    };

    return {

        findServers: function (timeoutMs) {

            // Expected server properties
            // Name, Id, Address, EndpointAddress (optional)
            return new Promise(function (resolve, reject) {

                MainActivity.findServers(timeoutMs);
                currentResolve = resolve;
            });
        }
    };

});