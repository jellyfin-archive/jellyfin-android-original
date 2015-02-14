angular
  .module('example')
  .controller('ConnectSignInController', function($scope, supersonic) {

    App.setBackgroundImage("images/splash.jpg");
	
	function navigateToServerSelection() {
	
		supersonic.ui.layers.push(new supersonic.ui.View("example#selectserver"), {
		  animate: true
		});
	}
	
	function login(username, password) {
	
		
	}
	
	$scope.login = function() {
	
		login($scope.username, $scope.password);
	};
	
	$scope.skip = function() {
	
		navigateToServerSelection();
	};

});
