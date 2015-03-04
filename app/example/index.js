angular.module('example', [
  // Declare here all AngularJS dependencies that are shared by the example module.
  'supersonic'

]).directive('carditem', function () {

    function onItemClick(e) {

        var card = jQuery(this).parents('.cardItem');
        var itemId = card[0].getAttribute('data-itemid');

        App.navigateToItemId(itemId);
    }

    return {
        restrict: "E",
        replace: true,
        link: function (scope, element, attrs) {

            var item = scope.item;

            var options = item.options;
            var apiClient = item.apiClient;

            // Todo: 
            // set listIndex on each item
            // put options somewhere
            // normalize options
            // normalize aspect ratio

            element.html(LibraryBrowser.getCardItemHtml(item, options, apiClient));

            $('.cardAction', element).on('click', onItemClick);
        }
    };
});
