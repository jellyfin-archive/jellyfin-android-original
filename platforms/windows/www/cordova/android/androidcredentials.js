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

    Events.on(ConnectionManager.credentialProvider(), 'credentialsupdated', updateCredentials);

    updateCredentials();
    initNativeConnectionManager();
});