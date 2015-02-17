angular
  .module('example')
  .controller('ServerSignInController', function ($scope, supersonic) {

      App.setBackgroundImage("images/splash.jpg");

      function login(username, password) {

          steroids.view.displayLoading();

          steroids.logger.log('Calling App.connectionManager');
          App.connectionManager().done(function (connectionManager) {

              steroids.logger.log('Calling connectionManager.connectToServer');
              connectionManager.loginToConnect(username, password).done(function () {

                  steroids.logger.log('Connect authentication succeeded');

                  connectionManager.connect().done(function (result) {

                      steroids.logger.log('result.State: ' + result.State);
                      steroids.view.removeLoading();

                      switch (result.State) {

                          case MediaBrowser.ConnectionState.ServerSelection:
                              App.navigateToServerSelection();
                              break;
                          case MediaBrowser.ConnectionState.ServerSignIn:
                              App.handleServerSignInResult(result);
                              break;
                          case MediaBrowser.ConnectionState.SignedIn:
                              App.handleSignedInResult(result);
                              break;
                          default:
                              steroids.logger.log('Unhandled ConnectionState');
                              break;
                      }
                  });

              }).fail(function () {

                  steroids.logger.log('Connect authentication failed');
                  steroids.view.removeLoading();

                  supersonic.ui.dialog.alert("Sign In Error", {
                      message: "Invalid username or password. Please try again."

                  });
              });

          });
      }

      $scope.login = function () {

          login($scope.username, $scope.password);
      };

      $scope.changeServer = function () {
          App.navigateToServerSelection();
      };

      function loadServer(id) {

          App.connectionManager().done(function (connectionManager) {

              var apiClient = connectionManager.getOrCreateApiClient(id);

              apiClient.getPublicUsers().done(function (users) {
                  renderUsers(apiClient, users);
              });

          });
      }

      function renderUsers(apiClient, users) {

          $scope.$apply(function () {
              
              $scope.users = users.map(function (u) {

                  u.ImageUrl = "splash.png";
                  return u;
              });
          });
      }

      loadServer(steroids.view.params.serverid);
  });
