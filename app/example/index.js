angular.module('example', [
  // Declare here all AngularJS dependencies that are shared by the example module.
  'supersonic'

]).directive('carditem', function () {

    return {
        restrict: "E",
        replace: true,
        template: "<div></div>",
        link: function (scope, element, attrs) {

            var item = scope.item;

            var options = item.options;
            var apiClient = item.apiClient;

            // Todo: 
            // set listIndex on each item
            // put options somewhere
            // normalize options
            // normalize aspect ratio

            var el = document.createElement('div');
            el.innerHTML = LibraryBrowser.getCardItemHtml(item, options, apiClient);

            element.replaceWith(LibraryBrowser.getCardItemHtml(item, options, apiClient));
        }
    };
});
