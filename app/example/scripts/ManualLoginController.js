angular
  .module('example')
  .controller('ManualLoginController', function ($scope, supersonic) {

      App.setBackgroundImage("images/splash.jpg");

      $scope.login = function () {

          var serverId = steroids.view.params.serverid;
          var username = $scope.username;
          var password = $scope.password;

          if (!username) {
              $('.txtUsername').focus();
              return;
          }

          App.loginToServer(serverId, username, password)
          .done(function (result) {

              $scope.cancel();

              App.handleAuthenticationResult(result);
          })
          .fail(function () {
              supersonic.ui.dialog.alert("Sign In Failure", {
                  message: "Invalid username or password entered. Please try again."
              });
          });

      };

      $scope.cancel = function () {

          supersonic.ui.modal.hide({
              animate: true
          });
      };

      function sendResultToParent(result) {

          supersonic.ui.modal.hide({
              animate: true
          });

          supersonic.data.channel('manualloginresult').publish(result);
      }

      $scope.username = steroids.view.params.username || '';

  });
