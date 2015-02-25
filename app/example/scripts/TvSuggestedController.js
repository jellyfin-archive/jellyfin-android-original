angular
  .module('example')
  .controller('TvSuggestedController', function ($scope, supersonic) {

      function loadLatest(apiClient, parentId) {

          var userId = apiClient.getCurrentUserId();
          var options = {

              IncludeItemTypes: "Episode",
              Limit: 24,
              Fields: "PrimaryImageAspectRatio,SyncInfo",
              ParentId: parentId,
              ImageTypeLimit: 1,
              EnableImageTypes: "Primary,Backdrop,Banner,Thumb"
          };

          apiClient.getJSON(apiClient.getUrl('Users/' + userId + '/Items/Latest', options)).done(function (items) {

              $scope.$apply(function () {
                  $scope.showLatest = items.length > 0;
                  $scope.latestItems = LibraryBrowser.mapItemsForRepeat(items, {

                      shape: "backdrop",
                      preferThumb: true,
                      inheritThumb: false,
                      showParentTitle: false,
                      showUnplayedIndicator: false,
                      showChildCountIndicator: true,
                      overlayText: true,
                      lazy: false

                  }, apiClient);
              });
          });
      }

      function loadNextUp(apiClient, parentId) {

          var userId = apiClient.getCurrentUserId();
          var query = {

              Limit: 24,
              Fields: "PrimaryImageAspectRatio,SeriesInfo,DateCreated,SyncInfo",
              UserId: userId,
              ExcludeLocationTypes: "Virtual",
              ImageTypeLimit: 1,
              EnableImageTypes: "Primary,Backdrop,Banner,Thumb"
          };

          query.ParentId = parentId;

          apiClient.getNextUpEpisodes(query).done(function (result) {

              $scope.$apply(function () {
                  $scope.showNextUp = result.Items.length > 0;
                  $scope.nextUpItems = LibraryBrowser.mapItemsForRepeat(result.Items, {

                      shape: "backdrop",
                      showTitle: true,
                      showParentTitle: true,
                      overlayText: false,
                      lazy: false,
                      preferThumb: true

                  }, apiClient);
              });
          });

      }

      function loadResume(apiClient, parentId) {

          var userId = apiClient.getCurrentUserId();
          var options = {

              SortBy: "DatePlayed",
              SortOrder: "Descending",
              IncludeItemTypes: "Episode",
              Filters: "IsResumable",
              Limit: 6,
              Recursive: true,
              Fields: "PrimaryImageAspectRatio,SeriesInfo,UserData,SyncInfo",
              ExcludeLocationTypes: "Virtual",
              ParentId: parentId,
              ImageTypeLimit: 1,
              EnableImageTypes: "Primary,Backdrop,Banner,Thumb"
          };

          apiClient.getItems(userId, options).done(function (result) {

              $scope.$apply(function () {
                  $scope.showResume = result.Items.length > 0;
                  $scope.resumeItems = LibraryBrowser.mapItemsForRepeat(result.Items, {

                      shape: "backdrop",
                      showTitle: true,
                      showParentTitle: true,
                      overlayText: true,
                      lazy: false,
                      context: 'tv'

                  }, apiClient);
              });
          });

      }

      function loadContent() {

          var serverId = steroids.view.params.serverid;
          var parentId = steroids.view.params.parentid;

          App.getApiClient(serverId).then(function (apiClient) {

              loadResume(apiClient, parentId);
              loadNextUp(apiClient, parentId);
              loadLatest(apiClient, parentId);
          });
      }

      supersonic.ui.views.current.whenVisible(function () {

          Header.load();
          loadContent();
      });

  });
