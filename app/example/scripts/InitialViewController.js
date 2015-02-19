angular
  .module('example')
  .controller('InitialViewController', function ($scope, supersonic) {

      $scope.next = function () {

          App.navigateToConnectSignIn();
      };

      function showWelcome() {

          App.setBackgroundImage("images/splash.jpg");
          steroids.view.removeLoading();
      }

      function startConnectionProcess() {

          steroids.logger.log('Calling App.connect');
          App.connect().done(function (result) {

              steroids.logger.log('App.connect done');
              steroids.logger.log('result.State: ' + result.State);

              switch (result.State) {

                  case MediaBrowser.ConnectionState.ConnectSignIn:
                      showWelcome();
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

      steroids.view.displayLoading();

      App.clearBackgroundImage();
      $scope.navbarTitle = "Login";

      supersonic.ui.views.start("example#global").then(function () {
          // https://github.com/AppGyver/steroids/issues/614
          setTimeout(startConnectionProcess, 500);
      });
  });
