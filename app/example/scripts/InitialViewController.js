angular
  .module('example')
  .controller('InitialViewController', function ($scope, supersonic) {

      $scope.next = function () {

          App.navigateToConnectSignIn();
      };

      function showWelcomeContent(visible) {

          $scope.$apply(function () {
              $scope.showWelcome = visible !== false;
          });
      }

      function showWelcome() {

          showWelcomeContent();
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
                      showWelcomeContent();
                      App.handleServerSignInResult(result);
                      break;
                  case MediaBrowser.ConnectionState.ServerSelection:
                      showWelcomeContent();
                      App.navigateToServerSelection();
                      break;
                  default:
                      steroids.logger.log('Unhandled ConnectionState');
                      break;
              }
          });
      }

	  //localStorage.removeItem('servercredentials3');
      showWelcomeContent(false);
      steroids.view.displayLoading();

      App.clearBackgroundImage();
      $scope.navbarTitle = "Login";

      supersonic.ui.views.start("example#global").then(function () {
          // https://github.com/AppGyver/steroids/issues/614
          setTimeout(startConnectionProcess, 1000);
      });
  });
