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
		
		
	};
	
});
