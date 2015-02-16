angular
  .module('example')
  .controller('InitialViewController', function($scope, supersonic, $http) {
	
	$scope.next = function() {
	
		navigateToConnectSignIn();
	};
	
	function navigateToConnectSignIn() {
	
		supersonic.ui.layers.push(new supersonic.ui.View("example#connectsignin"), {
		  animate: true
		});
	}
	
	function navigateToServerSelection() {
	
		supersonic.ui.layers.push(new supersonic.ui.View("example#selectserver"), {
		  animate: true
		});
	}
	
	function startConnectionProcess() {
	
		steroids.view.displayLoading();
	
		steroids.logger.log('Calling App.connectionManager');
		var connectionManager = App.connectionManager($http);

		steroids.logger.log('Calling connectionManager.connect');
		connectionManager.connect().done(function (result) {

			steroids.logger.log('connectionManager.connect done');
			steroids.logger.log('result.State: ' + result.State);
			steroids.view.removeLoading();
			
			switch (result.State) {
			
				case MediaBrowser.ConnectionState.ConnectSignIn:
					navigateToConnectSignIn();
					break;
				case MediaBrowser.ConnectionState.ServerSelection:
					navigateToServerSelection();
					break;
				default:
					steroids.logger.log('Unhandled ConnectionState');
					break;
			}
		});  
	}
	
	App.setBackgroundImage("images/splash.jpg");
	$scope.navbarTitle = "Login";

	startConnectionProcess();
});
