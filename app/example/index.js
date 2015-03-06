angular.module('example', [
  // Declare here all AngularJS dependencies that are shared by the example module.
  'supersonic'

]).directive('carditem', function () {

    return {
        restrict: "E",
        replace: true,
        link: function (scope, element, attrs) {

            var item = scope.item;

            var options = item.options;
            var apiClient = item.apiClient;

            element.html(LibraryBrowser.getCardItemHtml(item, options, apiClient));
        }
    };
});
