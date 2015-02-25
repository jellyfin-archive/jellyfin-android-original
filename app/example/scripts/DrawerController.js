angular
  .module('example')
  .controller('DrawerController', function ($scope, supersonic) {

      $scope.showView = function (view) {

          supersonic.ui.drawers.close();
          App.loadView(view.CollectionType, view.Id, view.Name, view.ServerId);
      };

      $scope.userProfile = function () {

          supersonic.ui.drawers.close();
      };

      $scope.settings = function () {

          supersonic.ui.drawers.close();
          App.loadView('Settings');
      };

      $scope.changeServer = function () {

          supersonic.ui.drawers.close();
          App.loadView('SelectServer');
      };

      $scope.logout = function () {

          supersonic.ui.drawers.close();
          App.logout();
      };

      function getIconClassName(view) {

          switch ((view.CollectionType || '').toLowerCase()) {

              case 'home':
                  return 'icon super-home';
              case 'channels':
                  return 'icon super-earth';
              case 'photos':
                  return 'icon super-camera';
              case 'playlists':
                  return 'icon super-music-note';
              case 'music':
                  return 'icon super-music-note';
              case 'livetv':
                  return 'icon super-videocamera';
              case 'tvshows':
                  return 'icon super-videocamera';
              case 'movies':
                  return 'icon super-videocamera';
              default:
                  return 'icon super-folder';
          }
      }

      function mapUserView(view) {

          view.iconClassName = getIconClassName(view);

          return view;
      }

      function loadUser(apiClient, user) {

          apiClient.getUserViews(user.Id).done(function (result) {

              var applyScope = function () {

                  $scope.isLocalSignedIn = true;
                  $scope.username = user.Name;

                  var imgUrl = user.PrimaryImageTag ?
                      apiClient.getUserImageUrl(user.Id, {
                          tag: user.PrimaryImageTag,
                          type: "Primary"

                      }) : null;

                  if (imgUrl) {
                      $scope.showUserIcon = false;
                      $scope.showUserImage = true;

                      imgUrl += '&height=40';

                      $('.userImageContainer').html('<img src="' + imgUrl + '" />');
                  } else {
                      $scope.showUserIcon = true;
                      $scope.showUserImage = false;
                  }

                  if ($scope.isLocalSignedIn) {
                      result.Items.splice(0, 0, {
                          
                          ServerId: user.ServerId,
                          Name: 'Home',
                          Id: 'Home',
                          CollectionType: 'Home'
                      });
                  }

                  $scope.views = result.Items.map(mapUserView);
                  $scope.isSignedIn = $scope.isLocalSignedIn || $scope.isConnectSignedIn;
              };

              $scope.$apply(applyScope);
          });
      }

      function onLocalUserSignedIn(user) {

          App.getApiClient(user.ServerId).done(function (apiClient) {

              loadUser(apiClient, user);
          });
      }

      function onLocalUserSignedOut() {

          $scope.$apply(function () {

              $scope.isLocalSignedIn = false;
              $scope.isSignedIn = $scope.isLocalSignedIn || $scope.isConnectSignedIn;
              $scope.views = [];
          });
      }

      function onConnectUserSignedIn() {

          $scope.$apply(function () {

              $scope.username = user.Name;
              $scope.isConnectSignedIn = true;
              $scope.isSignedIn = $scope.isLocalSignedIn || $scope.isConnectSignedIn;
          });
      }

      function onConnectUserSignedOut() {

          $scope.$apply(function () {

              $scope.isConnectSignedIn = false;
              $scope.isSignedIn = $scope.isLocalSignedIn || $scope.isConnectSignedIn;
          });
      }

      supersonic.data.channel('connectionmanagerevents').subscribe(function (message) {

          if (message.type == "localusersignedin") {
              onLocalUserSignedIn(message.data);
          }
          else if (message.type == "localusersignedout") {
              onLocalUserSignedOut();
          }
          else if (message.type == "connectusersignedin") {
              onConnectUserSignedIn();
          }
          else if (message.type == "connectusersignedout") {
              onConnectUserSignedOut();
          }
      });
  });
