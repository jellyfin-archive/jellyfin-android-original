angular
  .module('example')
  .controller('SelectServerController', function($scope, supersonic) {

    App.setBackgroundImage("images/splash.jpg");
	
	function navigateToNewServer() {
	
		supersonic.ui.layers.push(new supersonic.ui.View("example#newserver"), {
		  animate: true
		});
	}
	
	$scope.newServer = function() {
		navigateToNewServer();
	};
	
});
