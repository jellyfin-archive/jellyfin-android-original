angular
  .module('example')
  .controller('SelectServerController', function($scope, supersonic) {

    App.setBackgroundImage("images/splash.jpg");
	
	function navigateToNewServer() {
	
		supersonic.ui.layers.push(new supersonic.ui.View("example#newserver"), {
		  animate: true
		});
	}
	
	function loadServers() {
	
		steroids.logger.log('Calling App.connectionManager');
		var connectionManager = App.connectionManager();

		steroids.logger.log('Calling connectionManager.getServers');
		connectionManager.getAvailableServers().done(function (result) {

			$scope.servers = result;
		});  
	}
	
	$scope.newServer = function() {
		navigateToNewServer();
	};
	
	$scope.connectToServer = function(server) {
	
		steroids.view.displayLoading();
	
		steroids.logger.log('Calling App.connectionManager');
		var connectionManager = App.connectionManager();

		steroids.logger.log('Calling connectionManager.connectToServer');
		connectionManager.connectToServer(server).done(function (result) {

			steroids.logger.log('result.State: ' + result.State);
			steroids.view.removeLoading();
			
			switch (result.State) {
			
				case MediaBrowser.ConnectionState.Unavailable:
					alert('Unavailable');
					break;
				case MediaBrowser.ConnectionState.ServerSignIn:
					alert('ServerSignIn');
					break;
				case MediaBrowser.ConnectionState.SignedIn:
					alert('SignedIn');
					break;
				default:
					steroids.logger.log('Unhandled ConnectionState');
					break;
			}
		});  
	};
	
	loadServers();

});
