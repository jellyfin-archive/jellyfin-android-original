(function (globalScope) {

    function stringToArrayBuffer(string) {
        // UTF-16LE
        var buf = new ArrayBuffer(string.length * 2);
        var bufView = new Uint16Array(buf);
        for (var i = 0, strLen = string.length; i < strLen; i++) {
            bufView[i] = string.charCodeAt(i);
        }
        return buf;
    }

    globalScope.ServerDiscovery = {

        findServers: function (timeoutMs) {

            var deferred = DeferredBuilder.Deferred();

            var servers = [];

            // Expected server properties
            // Name, Id, Address, EndpointAddress (optional)

            var chrome = globalScope.chrome;

            if (!chrome) {
                deferred.resolveWith(null, [servers]);
                return deferred.promise();
            }

            var data = stringToArrayBuffer('who is EmbyServer?');

            var port = 7359;
            chrome.sockets.udp.create(function (createInfo) {
                chrome.sockets.udp.bind(createInfo.socketId, '0.0.0.0', port, function (result) {
                    chrome.sockets.udp.send(createInfo.socketId, data, '255.255.255.255', port, function (result) {
                        if (result < 0) {
                            console.log('send fail: ' + result);
                            chrome.sockets.udp.close(createInfo.socketId);
                            deferred.resolveWith(null, [servers]);
                        } else {
                            console.log('sendTo: success ' + port);
                            chrome.sockets.udp.close(createInfo.socketId);
                            deferred.resolveWith(null, [servers]);
                        }
                    });
                });
            });

            return deferred.promise();
        }
    };

})(window);