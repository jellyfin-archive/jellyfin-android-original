define(['apphost'], function (appHost) {

    function updateCredentials() {

        console.log('sending updated credentials to ApiClientBridge');

        var json = JSON.stringify(ConnectionManager.credentialProvider().credentials());
        var credentials = JSON.parse(json);

        for (var i = 0, length = credentials.Servers.length; i < length; i++) {
            var server = credentials.Servers[i];

            if (server.DateLastAccessed != null) {
                server.DateLastAccessed = new Date(server.DateLastAccessed).toISOString();
            }
        }

        json = JSON.stringify(credentials);
        ApiClientBridge.updateCredentials(json);
    }

    function initNativeConnectionManager() {

        console.log('initNativeConnectionManager');

        var capabilities = ConnectionManager.capabilities();

        appHost.appInfo().then(function (appInfo) {
            ApiClientBridge.init(appInfo.appName, appInfo.appVersion, appInfo.deviceId, appInfo.deviceName, JSON.stringify(capabilities));
        });
    }

    function getDownloadSpeed(bytes, url) {

        return new Promise(function (resolve, reject) {

            ApiClientBridge.getDownloadSpeed(bytes, url);

            Events.on(AndroidAjax, 'downloadspeedresponse', function (e, response) {

                Events.off(AndroidAjax, 'downloadspeedresponse');

                if (response) {

                    resolve(response);
                }
                else {

                    // Need to mimic the jquery ajax error response
                    reject();
                }

            });

        });
    }

    function initApiClient(newApiClient) {
        newApiClient.getDownloadSpeed = function (bytes) {
            return getDownloadSpeed(bytes, newApiClient.getUrl('Playback/BitrateTest', {
                api_key: newApiClient.accessToken(),
                Size: bytes
            }));
        };
    }

    Events.on(ConnectionManager, 'apiclientcreated', function (e, newApiClient) {

        initApiClient(newApiClient);
    });

    Events.on(ConnectionManager.credentialProvider(), 'credentialsupdated', updateCredentials);

    updateCredentials();
    initNativeConnectionManager();

    if (window.ApiClient) {
        initApiClient(window.ApiClient);
    }

    window.AndroidAjax = {

        onResponse: function (id, response) {

            Events.trigger(AndroidAjax, 'response' + id, [true, response]);
        },
        onError: function (id, response) {

            Events.trigger(AndroidAjax, 'response' + id, [false, response]);
        },
        onDownloadSpeedResponse: function (response) {

            Events.trigger(AndroidAjax, 'downloadspeedresponse', [response]);
        }
    };

});