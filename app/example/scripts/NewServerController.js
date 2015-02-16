angular
  .module('example')
  .controller('NewServerController', function($scope, supersonic) {

    App.setBackgroundImage("images/splash.jpg");
	
	$scope.port = 8096;
	
	$scope.connect = function() {
	
		var address = $scope.address;
		
		if ($scope.port) {
			address += ':' + $scope.port;
		}
		
		steroids.view.displayLoading();
	
		steroids.logger.log('Calling App.connectionManager');
		
		var connectionManager = App.connectionManager();
		
		steroids.logger.log('Calling connectionManager.connectToAddress');
		connectionManager.connectToAddress(address).done(function (result) {

			steroids.logger.log('newServer connectToAddress done');
			steroids.logger.log('newServer result.State: ' + result.State);
			steroids.view.removeLoading();
			
			switch (result.State) {
			
				case MediaBrowser.ConnectionState.Unavailable:
					supersonic.ui.dialog.alert("Connection Failure", {
						message: "We're unable to reach this server. Please ensure it is running and try again."
					});
					break;
				case MediaBrowser.ConnectionState.SignedIn:
				case MediaBrowser.ConnectionState.ServerSignIn:
					sendResultToParent(result);
					break;
				default:
					steroids.logger.log('Unhandled ConnectionState: ' + result.State);
					break;
			}
			
		});  
	};
	
	$scope.cancel = function() {
	
		supersonic.ui.modal.hide({
			animate: true
		});
	};
	
	function sendResultToParent(result) {
	
		supersonic.ui.modal.hide({
			animate: true
		});

		supersonic.data.channel('newserverresult').publish(result);
	}
	
});
