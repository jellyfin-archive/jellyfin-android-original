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
		
		self.setBackgroundImage = function(url) {
			steroids.view.setBackgroundImage(url);
			$('body').addClass('clearBody');
		};
		
		self.clearBackgroundImage = function() {
			steroids.view.setBackgroundImage(null);
			$('body').removeClass('clearBody');
		};

        return self;
    }();

})(this);