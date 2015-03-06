angular
  .module('example')
  .controller('HomeController', function ($scope, supersonic) {

      function loadLatest(apiClient) {

          var userId = apiClient.getCurrentUserId();
          var options = {

              Limit: 24,
              Fields: "PrimaryImageAspectRatio,SyncInfo",
              ImageTypeLimit: 1,
              EnableImageTypes: "Primary,Backdrop,Banner,Thumb"
          };

          apiClient.getJSON(apiClient.getUrl('Users/' + userId + '/Items/Latest', options)).done(function (items) {

              $scope.$apply(function () {
                  $scope.showLatest = items.length > 0;
                  $scope.latestItems = LibraryBrowser.mapItemsForRepeat(items, {

                      preferThumb: true,
                      shape: 'backdrop',
                      context: 'home',
                      showUnplayedIndicator: false,
                      showChildCountIndicator: true,
                      lazy: false

                  }, apiClient);
              });

              LibraryBrowser.bindListEvents('latestItemsContainer');
          });
      }

      function loadResume(apiClient) {

          var userId = apiClient.getCurrentUserId();
          var options = {

              SortBy: "DatePlayed",
              SortOrder: "Descending",
              MediaTypes: "Video",
              Filters: "IsResumable",
              Limit: 6,
              Recursive: true,
              Fields: "PrimaryImageAspectRatio,SyncInfo",
              CollapseBoxSetItems: false,
              ExcludeLocationTypes: "Virtual",
              ImageTypeLimit: 1,
              EnableImageTypes: "Primary,Backdrop,Banner,Thumb"
          };

          apiClient.getItems(userId, options).done(function (result) {

              $scope.$apply(function () {
                  $scope.showResume = result.Items.length > 0;
                  $scope.resumeItems = LibraryBrowser.mapItemsForRepeat(result.Items, {

                      preferThumb: true,
                      shape: 'backdrop',
                      overlayText: true,
                      showTitle: true,
                      showParentTitle: true,
                      context: 'home',
                      lazy: false

                  }, apiClient);
              });

              LibraryBrowser.bindListEvents('resumeItemsContainer');
          });

      }

      function loadContent() {

          var serverId = steroids.view.params.serverid;

          App.getApiClient(serverId).then(function (apiClient) {

              loadLatest(apiClient);
              loadResume(apiClient);

          });
      }

      supersonic.ui.views.current.whenVisible(function () {

          Header.load();
          loadContent();
      });
  });
