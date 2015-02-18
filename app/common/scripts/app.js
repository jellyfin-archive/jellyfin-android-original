(function (globalScope) {

    globalScope.App = function () {

        var self = {};

        self.appName = "Media Browser";
        self.appVersion = "3";

        self.capabilities = function () {

            return {

            };
        };

        self.connectionManager = function () {

            var deferred = DeferredBuilder.Deferred();

			if (self.connectionManagerInstance){
				deferred.resolveWith(null, [self.connectionManagerInstance]);
			}
			else {
				supersonic.device.ready.then(function () {
					var credentialProvider = new MediaBrowser.CredentialProvider();
					var deviceName = device.model;
					var deviceId = device.uuid;

					self.connectionManagerInstance = new MediaBrowser.ConnectionManager(Logger, credentialProvider, self.appName, self.appVersion, deviceName, deviceId, self.capabilities());

					deferred.resolveWith(null, [self.connectionManagerInstance]);
				});			
			}

            return deferred.promise();
        };

        self.setBackgroundImage = function (url) {
            steroids.view.setBackgroundImage(url);
            self.addClass(document.body, 'clearBody');
        };

        self.clearBackgroundImage = function () {
            steroids.view.setBackgroundImage(null);
            self.removeClass(document.body, 'clearBody');
        };

        self.addClass = function (elem, name) {

            var css = ' ' + (elem.className || '') + ' ';

            if (css.indexOf(' ' + name + ' ') == -1) {
                elem.className = (css + name).trim();
            }
        };

        self.removeClass = function (elem, name) {

            var css = ' ' + (elem.className || '') + ' ';
            elem.className = css.replace(' ' + name + ' ', '').trim();
        };

        self.navigateToServerSelection = function () {

            supersonic.ui.layers.push(new supersonic.ui.View("example#selectserver"), {
                animate: true
            });
        };

        self.navigateToConnectSignIn = function () {

            supersonic.ui.layers.push(new supersonic.ui.View("example#connectsignin"), {
                animate: true
            });
        };

        self.handleServerSignInResult = function (result) {

            var server = result.Servers[0];

            console.log('handleServerSignInResult');
            console.log('ServerId: ' + server.Id);
            supersonic.ui.layers.push(new supersonic.ui.View("example#serversignin?serverid=" + server.Id), {
                animate: true
            });
        };

        self.handleSignedInResult = function (result) {
			//alert('signed in');
        };

        self.authenticationResult = function (result) {
			//alert('signed in');
        };

        return self;
    }();

})(this);