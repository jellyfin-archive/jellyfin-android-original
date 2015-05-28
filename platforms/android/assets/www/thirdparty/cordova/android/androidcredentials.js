(function () {

    function updateCredentials() {

        console.log('sending updated credentials to ApiClientBridge');
        ApiClientBridge.updateCredentials(ConnectionManager.credentialProvider().credentials());
    }

    function initNativeConnectionManager() {

        console.log('initNativeConnectionManager');
    }

    Events.on(ConnectionManager.credentialProvider(), 'credentialsupdated', updateCredentials);

    updateCredentials();
    initNativeConnectionManager();

})();