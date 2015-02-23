angular
  .module('example')
  .controller('DrawerController', function ($scope, supersonic) {

      $scope.showView = function (view) {

          supersonic.ui.drawers.close();
      };

      function onLocalUserSignedIn(user) {

          $scope.$apply(function () {

              $scope.views = [
              {
                  Name: "test"
              }];
          });

      }

      function onLocalUserSignedOut(logoutInfo) {

          $scope.$apply(function () {

              $scope.views = [];
          });
      }

      supersonic.data.channel('connectionmanagerevents').subscribe(function (message) {

          if (message.type == "localusersignedin") {
              onLocalUserSignedIn(message.data);
          }
          else if (message.type == "localusersignedout") {
              onLocalUserSignedOut(message.data);
          }
      });

  });
