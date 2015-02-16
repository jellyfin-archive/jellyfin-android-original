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

			var manager = new MediaBrowser.ConnectionManager(Logger, credentialProvider, self.appName, self.appVersion, "deviceName", "deviceId", self.capabilities());
			
			return manager;
        };
		
		self.setBackgroundImage = function(url) {
			steroids.view.setBackgroundImage(url);
			self.addClass(document.body, 'clearBody');
		};
		
		self.clearBackgroundImage = function() {
			steroids.view.setBackgroundImage(null);
			self.removeClass(document.body, 'clearBody');
		};
		
		self.addClass = function(elem, name) {
		
			var css = ' ' + (elem.className || '') + ' ';
			
			if (css.indexOf(' ' + name + ' ') == -1) {
				elem.className = (css + name).trim();
			}
		};
		
		self.removeClass = function(elem, name) {
		
			var css = ' ' + (elem.className || '') + ' ';
			elem.className = css.replace(' ' + name + ' ', '').trim();
		};
		
		self.navigateToServerSelection = function() {
		
			supersonic.ui.layers.push(new supersonic.ui.View("example#selectserver"), {
			  animate: true
			});
		};
		
		self.navigateToConnectSignIn = function() {
		
			supersonic.ui.layers.push(new supersonic.ui.View("example#connectsignin"), {
			  animate: true
			});
		};
		
		self.handleServerSignInResult = function(result) {
		
			var server = result.Servers[0];
			
			console.log('handleServerSignInResult');
			console.log('ServerId: ' + server.Id);
			supersonic.ui.layers.push(new supersonic.ui.View("example#serversignin?id=" + server.Id), {
			  animate: true
			});
		};
		
		self.handleSignedInResult = function(result) {
		
		};

        return self;
    }();

})(this);