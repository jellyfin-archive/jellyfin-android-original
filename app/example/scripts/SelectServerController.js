angular
  .module('example')
  .controller('SelectServerController', function ($scope, supersonic) {

      function navigateToNewServer() {

          supersonic.ui.modal.show(new supersonic.ui.View("example#newserver"), {
              animate: true
          });
      }

      function loadServers() {

          steroids.logger.log('Calling App.getAvailableServers');
          App.getAvailableServers().done(function (result) {

              $scope.$apply(function () {
                  $scope.servers = result;
              });
          });
      }

      function loadConnectInfo() {

          App.isLoggedIntoConnect().done(function (result) {

              $scope.$apply(function () {

                  $scope.isNotLoggedIntoConnect = !result.isLoggedIntoConnect;
              });
          });
      }

      function loadPage() {

          App.setBackgroundImage("images/splash.jpg");

          loadServers();
          loadConnectInfo();
      }

      function processConnectionResult(result) {

          switch (result.State) {

              case MediaBrowser.ConnectionState.Unavailable:

                  steroids.view.removeLoading();
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

          steroids.logger.log('Calling App.connectToServer');
          App.connectToServer(server).done(function (result) {

              steroids.logger.log('result.State: ' + result.State);

              processConnectionResult(result);
          });

      };

      $scope.signInWithConnect = function () {

          App.navigateToConnectSignIn();
      };

      loadPage();
  });
