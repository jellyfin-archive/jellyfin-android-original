(function (globalScope) {

    globalScope.App = function () {

        var self = {};

        self.appName = "Media Browser";
        self.appVersion = "3";

        self.capabilities = function() {

            return {

            };
        };

        self.connectionManager = function() {

            var credentialProvider = new MediaBrowser.CredentialProvider();

			var manager = new MediaBrowser.ConnectionManager(jQuery, Logger, credentialProvider, self.appName, self.appVersion, "deviceName", "deviceId", self.capabilities());
			
			return manager;
        };

        return self;
    }();

})(this);