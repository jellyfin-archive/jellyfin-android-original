(function () {

    function updateCredentials() {

        console.log('sending updated credentials to ApiClientBridge');
        ApiClientBridge.updateCredentials(JSON.stringify(ConnectionManager.credentialProvider().credentials()));
    }

    function initNativeConnectionManager() {

        console.log('initNativeConnectionManager');

        var capabilities = ConnectionManager.capabilities();

        ApiClientBridge.init(AppInfo.appName, AppInfo.appVersion, AppInfo.deviceId, AppInfo.deviceName, JSON.stringify(capabilities));
    }

    Events.on(ConnectionManager.credentialProvider(), 'credentialsupdated', updateCredentials);

    updateCredentials();
    initNativeConnectionManager();

})();