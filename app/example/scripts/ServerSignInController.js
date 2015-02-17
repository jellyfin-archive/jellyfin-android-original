angular
  .module('example')
  .controller('ServerSignInController', function ($scope, supersonic) {

      function login(username, password) {

          var serverId = $scope.serverId;

          var apiClient = $scope.apiClient;

		  apiClient.authenticateUserByName(username, password)
		  .done(function (result) {
			  supersonic.ui.dialog.alert("OK!", {
				  message: "OK!"
			  });
		  })
		  .fail(function () {
			  supersonic.ui.dialog.alert("Sign In Failure", {
				  message: "Invalid username or password entered. Please try again."
			  });
		  });
      }

      $scope.showManualLogin = function (username) {

          var url = "example#manuallogin?serverid=" + $scope.serverId;

          if (username) {
              url += "&username=" + username;
          }

          supersonic.ui.modal.show(new supersonic.ui.View(url), {
              animate: true
          });
      };

      $scope.loginUser = function (user) {

          if (user.HasPassword) {
              $scope.showManualLogin(user.Name);
          }
          else {
              login(user.Name);
          }
      };

      $scope.changeServer = function () {
          App.navigateToServerSelection();
      };

      function loadServer(id) {

          $scope.serverId = id;

          App.connectionManager().done(function (connectionManager) {

              var apiClient = connectionManager.getOrCreateApiClient(id);
			  $scope.apiClient = apiClient;
			  
              apiClient.getPublicUsers().done(function (users) {
                  renderUsers(apiClient, users);
              });

          });
      }

      function renderUsers(apiClient, users) {

          $scope.$apply(function () {

              $scope.users = users.map(function (u) {

                  if (u.PrimaryImageTag) {
                      u.ImageUrl = apiClient.getUserImageUrl(u.Id, {
                          type: 'Primary',
                          tag: u.PrimaryImageTag
                      });
                  }
                  else {
                      u.ImageUrl = "/images/user.png";
                  }

                  return u;
              });
          });
      }

      App.setBackgroundImage("images/splash.jpg");
      loadServer(steroids.view.params.serverid);
  });
