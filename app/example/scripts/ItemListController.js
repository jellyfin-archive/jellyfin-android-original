angular
  .module('example')
  .controller('ItemListController', function ($scope, supersonic) {

      function loadMovies(apiClient, userId, parentId) {

          var options = {

              SortBy: "SortName",
              SortOrder: "Ascending",
              IncludeItemTypes: "Movie",
              Recursive: true,
              Fields: "PrimaryImageAspectRatio,SortName,MediaSourceCount,IsUnidentified,SyncInfo",
              StartIndex: 0,
              ImageTypeLimit: 1,
              EnableImageTypes: "Primary,Backdrop,Banner,Thumb",
              ParentId: parentId
          };

          apiClient.getItems(userId, options).done(function (result) {

              $scope.$apply(function () {

                  $scope.items = LibraryBrowser.mapItemsForRepeat(result.Items, {

                      shape: "portrait",
                      showTitle: false,
                      centerText: true,
                      lazy: false,
                      overlayText: true

                  }, apiClient);
              });
          });

      }

      function loadSeries(apiClient, userId, parentId) {

          var options = {

              SortBy: "SortName",
              SortOrder: "Ascending",
              IncludeItemTypes: "Series",
              Recursive: true,
              Fields: "PrimaryImageAspectRatio,SortName,SyncInfo",
              StartIndex: 0,
              ImageTypeLimit: 1,
              EnableImageTypes: "Primary,Backdrop,Banner,Thumb",
              ParentId: parentId
          };

          apiClient.getItems(userId, options).done(function (result) {

              $scope.$apply(function () {

                  $scope.items = LibraryBrowser.mapItemsForRepeat(result.Items, {

                      shape: "backdrop",
                      preferThumb: true,
                      lazy: false

                  }, apiClient);
              });
          });
      }

      function loadTrailers(apiClient, userId, parentId) {

          var options = {

              SortBy: "SortName",
              SortOrder: "Ascending",
              Recursive: true,
              Fields: "PrimaryImageAspectRatio,SortName,SyncInfo",
              StartIndex: 0,
              ImageTypeLimit: 1,
              EnableImageTypes: "Primary,Backdrop,Banner,Thumb",
              UserId: userId
          };

          apiClient.getJSON(apiClient.getUrl('Trailers', options)).done(function (result) {

              $scope.$apply(function () {

                  $scope.items = LibraryBrowser.mapItemsForRepeat(result.Items, {

                      shape: "portrait",
                      showTitle: false,
                      centerText: true,
                      lazy: false,
                      overlayText: false

                  }, apiClient);
              });
          });

      }

      function loadCollections(apiClient, userId, parentId) {

          var options = {

              SortBy: "SortName",
              SortOrder: "Ascending",
              IncludeItemTypes: "BoxSet",
              Recursive: true,
              Fields: "PrimaryImageAspectRatio,SortName,SyncInfo,CanDelete",
              StartIndex: 0,
              ImageTypeLimit: 1,
              EnableImageTypes: "Primary,Backdrop,Banner,Thumb"
          };

          apiClient.getItems(userId, options).done(function (result) {

              $scope.$apply(function () {

                  $scope.items = LibraryBrowser.mapItemsForRepeat(result.Items, {

                      shape: "auto",
                      showTitle: true,
                      centerText: true,
                      lazy: false

                  }, apiClient);
              });
          });

      }

      function loadUpcoming(apiClient, userId, parentId) {

          var options = {

              Limit: 40,
              Fields: "AirTime,UserData,SeriesStudio,SyncInfo",
              UserId: userId,
              ImageTypeLimit: 1,
              EnableImageTypes: "Primary,Backdrop,Banner,Thumb",
              ParentId: parentId
          };

          apiClient.getJSON(apiClient.getUrl("Shows/Upcoming", options)).done(function (result) {

              $scope.$apply(function () {

                  $scope.items = LibraryBrowser.mapItemsForRepeat(result.Items, {

                      showLocationTypeIndicator: false,
                      shape: "backdrop",
                      showTitle: true,
                      showPremiereDate: true,
                      showPremiereDateIndex: true,
                      preferThumb: true,
                      lazy: false

                  }, apiClient);
              });
          });

      }

      function loadContent() {
          
          var serverId = steroids.view.params.serverid;
          var parentId = steroids.view.params.parentid;

          App.getApiClient(serverId).then(function (apiClient) {

              var userId = apiClient.getCurrentUserId();
              var mode = steroids.view.params.mode;

              switch (mode) {

                  case 'movies':
                      loadMovies(apiClient, userId, parentId);
                      break;
                  case 'trailers':
                      loadTrailers(apiClient, userId, parentId);
                      break;
                  case 'collections':
                      loadCollections(apiClient, userId, parentId);
                      break;
                  case 'genres':
                      loadMovies(apiClient, userId, parentId);
                      break;
                  case 'upcoming':
                      loadUpcoming(apiClient, userId, parentId);
                      break;
                  case 'series':
                      loadSeries(apiClient, userId, parentId);
                      break;
                  case 'musicalbums':
                      loadMovies(apiClient, userId, parentId);
                      break;
                  case 'musicartists':
                      loadMovies(apiClient, userId, parentId);
                      break;
                  case 'artists':
                      loadMovies(apiClient, userId, parentId);
                      break;
                  case 'musicgenres':
                      loadMovies(apiClient, userId, parentId);
                      break;
                  default:
                      break;
              }
          });
      }

      supersonic.ui.views.current.whenVisible(function () {

          Header.load();
          loadContent();
      });

  });
