angular
  .module('example')
  .controller('InitialViewController', function($scope, supersonic) {

	function navigateToConnectSignIn() {
	
		supersonic.ui.modal.show(new supersonic.ui.View("example#connectsignin"), {
		  animate: true,
		  navigationBar: false
		});
	}
	
	steroids.view.displayLoading();
	
    $scope.navbarTitle = "Login1";

	steroids.logger.log('Calling App.connectionManager');
	var connectionManager = App.connectionManager();

	steroids.logger.log('Calling connectionManager.connect');
	connectionManager.connect().done(function (result) {

		steroids.logger.log('connectionManager.connect done');
		steroids.logger.log('result.State: ' + result.State);
		steroids.view.removeLoading();
		
		switch (result.State) {
		
			case MediaBrowser.ConnectionState.ConnectSignIn:
				navigateToConnectSignIn();
				break;
			default:
				steroids.logger.log('Unhandled ConnectionState');
				break;
		}
	});  
	

});
