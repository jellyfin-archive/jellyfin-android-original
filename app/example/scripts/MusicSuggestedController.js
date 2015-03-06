angular
  .module('example')
  .controller('MusicSuggestedController', function ($scope, supersonic) {

      function loadLatest(apiClient, parentId) {

          var userId = apiClient.getCurrentUserId();
          var options = {

              IncludeItemTypes: "Audio",
              Limit: 20,
              Fields: "PrimaryImageAspectRatio,SyncInfo",
              ParentId: parentId,
              ImageTypeLimit: 1,
              EnableImageTypes: "Primary,Backdrop,Banner,Thumb"
          };

          apiClient.getJSON(apiClient.getUrl('Users/' + userId + '/Items/Latest', options)).done(function (items) {

              $scope.$apply(function () {
                  $scope.showLatest = items.length > 0;
                  $scope.latestItems = LibraryBrowser.mapItemsForRepeat(items, {

                      showUnplayedIndicator: false,
                      showChildCountIndicator: true,
                      shape: "square",
                      showTitle: true,
                      showParentTitle: true,
                      lazy: false

                  }, apiClient);
              });

              LibraryBrowser.bindListEvents('latestItemsContainer');
          });
      }

      function loadRecentlyPlayed(apiClient, parentId) {

          var userId = apiClient.getCurrentUserId();
          var options = {

              SortBy: "DatePlayed",
              SortOrder: "Descending",
              IncludeItemTypes: "Audio",
              Limit: 10,
              Recursive: true,
              Fields: "PrimaryImageAspectRatio,AudioInfo,SyncInfo",
              Filters: "IsPlayed",
              ParentId: parentId,
              ImageTypeLimit: 1,
              EnableImageTypes: "Primary,Backdrop,Banner,Thumb"
          };

          apiClient.getItems(userId, options).done(function (result) {

              $scope.$apply(function () {
                  $scope.showRecentlyPlayed = result.Items.length > 0;
                  $scope.recentlyPlayedItems = LibraryBrowser.mapItemsForRepeat(result.Items, {

                      showUnplayedIndicator: false,
                      shape: "square",
                      showTitle: true,
                      showParentTitle: true,
                      defaultAction: 'play',
                      lazy: false

                  }, apiClient);
              });

              LibraryBrowser.bindListEvents('recentlyPlayedItemsContainer');
          });
      }

      function loadFrequentlyPlayed(apiClient, parentId) {

          var userId = apiClient.getCurrentUserId();
          var options = {

              SortBy: "PlayCount",
              SortOrder: "Descending",
              IncludeItemTypes: "Audio",
              Limit: 20,
              Recursive: true,
              Fields: "PrimaryImageAspectRatio,AudioInfo,SyncInfo",
              Filters: "IsPlayed",
              ParentId: parentId,
              ImageTypeLimit: 1,
              EnableImageTypes: "Primary,Backdrop,Banner,Thumb"
          };

          apiClient.getItems(userId, options).done(function (result) {

              $scope.$apply(function () {
                  $scope.showFrequentlyPlayed = result.Items.length > 0;
                  $scope.frequentlyPlayedItems = LibraryBrowser.mapItemsForRepeat(result.Items, {

                      showUnplayedIndicator: false,
                      shape: "square",
                      showTitle: true,
                      showParentTitle: true,
                      defaultAction: 'play',
                      lazy: false

                  }, apiClient);
              });

              LibraryBrowser.bindListEvents('frequentlyPlayedItemsContainer');
          });
      }

      function loadContent() {

          var serverId = steroids.view.params.serverid;
          var parentId = steroids.view.params.parentid;

          App.getApiClient(serverId).then(function (apiClient) {

              loadLatest(apiClient, parentId);
              loadRecentlyPlayed(apiClient, parentId);
              loadFrequentlyPlayed(apiClient, parentId);
          });
      }

      supersonic.ui.views.current.whenVisible(function () {

          Header.load();
          loadContent();
      });

  });
