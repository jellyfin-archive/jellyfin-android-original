angular
  .module('example')
  .controller('MoviesSuggestedController', function ($scope, supersonic) {

      function loadLatest(apiClient, parentId) {

          var userId = apiClient.getCurrentUserId();
          var options = {

              IncludeItemTypes: "Movie",
              Limit: 30,
              Fields: "PrimaryImageAspectRatio,MediaSourceCount,SyncInfo",
              ParentId: parentId,
              ImageTypeLimit: 1,
              EnableImageTypes: "Primary,Backdrop,Banner,Thumb"
          };

          apiClient.getJSON(apiClient.getUrl('Users/' + userId + '/Items/Latest', options)).done(function (items) {

              $scope.$apply(function () {
                  $scope.latestItems = LibraryBrowser.mapItemsForRepeat(items, {

                      lazy: false,
                      shape: 'portrait',
                      overlayText: false

                  }, apiClient);
              });
          });
      }

      function loadResume(apiClient, parentId) {

          var userId = apiClient.getCurrentUserId();
          var options = {

              SortBy: "DatePlayed",
              SortOrder: "Descending",
              IncludeItemTypes: "Movie",
              Filters: "IsResumable",
              Limit: 6,
              Recursive: true,
              Fields: "PrimaryImageAspectRatio,MediaSourceCount,SyncInfo",
              CollapseBoxSetItems: false,
              ParentId: parentId,
              ImageTypeLimit: 1,
              EnableImageTypes: "Primary,Backdrop,Banner,Thumb"
          };

          apiClient.getItems(userId, options).done(function (result) {

              $scope.$apply(function () {
                  $scope.showResume = result.Items.length > 0;
                  $scope.resumeItems = LibraryBrowser.mapItemsForRepeat(result.Items, {

                      preferBackdrop: true,
                      shape: 'backdrop',
                      overlayText: true,
                      showTitle: true,
                      lazy: false

                  }, apiClient);
              });
          });

      }

      function loadContent() {

          var serverId = steroids.view.params.serverid;
          var parentId = null;

          App.getApiClient(serverId).then(function (apiClient) {

              loadLatest(apiClient, parentId);
              loadResume(apiClient, parentId);

          });
      }

      supersonic.ui.views.current.whenVisible(function () {

          Header.load();
          loadContent();
      });

  });
