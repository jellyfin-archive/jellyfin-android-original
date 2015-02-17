angular
  .module('example')
  .controller('SelectServerController', function ($scope, supersonic) {

      App.setBackgroundImage("images/splash.jpg");

      function navigateToNewServer() {

          supersonic.ui.modal.show(new supersonic.ui.View("example#newserver"), {
              animate: true
          });
      }

      function loadServers() {

          steroids.logger.log('Calling App.connectionManager');
          App.connectionManager().done(function (connectionManager) {

              steroids.logger.log('Calling connectionManager.getServers');
              connectionManager.getAvailableServers().done(function (result) {

                  $scope.servers = result;
              });
          });
      }

      function processConnectionResult(result) {

          switch (result.State) {

              case MediaBrowser.ConnectionState.Unavailable:

                  supersonic.ui.dialog.alert("Connection Failure", {
                      message: "We're unable to reach this server. Please ensure it is running and try again."
                  });
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
      }

      $scope.newServer = function () {
          navigateToNewServer();
      };

      supersonic.data.channel('newserverresult').subscribe(function (message) {
          processConnectionResult(message);
      });

      $scope.connectToServer = function (server) {

          steroids.view.displayLoading();

          steroids.logger.log('Calling App.connectionManager');
          App.connectionManager().done(function (connectionManager) {

              steroids.logger.log('Calling connectionManager.connectToServer');
              connectionManager.connectToServer(server).done(function (result) {

                  steroids.logger.log('result.State: ' + result.State);

                  processConnectionResult(result);
              });
          });

      };

      loadServers();
  });
