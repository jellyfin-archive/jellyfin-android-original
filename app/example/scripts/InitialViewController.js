angular
  .module('example')
  .controller('InitialViewController', function($scope, supersonic) {
	
	$scope.next = function() {
	
		App.navigateToConnectSignIn();
	};
	
	function startConnectionProcess() {
	
		steroids.view.displayLoading();
	
		steroids.logger.log('Calling App.connectionManager');
		var connectionManager = App.connectionManager();

		steroids.logger.log('Calling connectionManager.connect');
		connectionManager.connect().done(function (result) {

			steroids.logger.log('connectionManager.connect done');
			steroids.logger.log('result.State: ' + result.State);
			steroids.view.removeLoading();
			
			switch (result.State) {
			
				case MediaBrowser.ConnectionState.ConnectSignIn:
					//App.navigateToConnectSignIn();
					break;
				case MediaBrowser.ConnectionState.SignedIn:
					App.handleSignedInResult(result);
					break;
				case MediaBrowser.ConnectionState.ServerSignIn:
					App.handleServerSignInResult(result);
					break;
				case MediaBrowser.ConnectionState.ServerSelection:
					App.navigateToServerSelection();
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
