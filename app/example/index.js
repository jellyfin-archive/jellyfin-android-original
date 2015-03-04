angular.module('example', [
  // Declare here all AngularJS dependencies that are shared by the example module.
  'supersonic'

]).directive('carditem', function () {

    function onItemClick(e) {

        var card = this;
        while (!$(card).hasClass('cardItem')) {
            card = card.parentNode;
        }

        var href = card.getAttribute('data-href');

        var view = new supersonic.ui.View(href);
        supersonic.ui.layers.push(view);
    }

    return {
        restrict: "E",
        replace: true,
        link: function (scope, element, attrs) {

            var item = scope.item;

            var options = item.options;
            var apiClient = item.apiClient;

            element.html(LibraryBrowser.getCardItemHtml(item, options, apiClient));

            var cardAction = element[0].getElementsByClassName('cardAction')[0];

            $(cardAction).on('click', onItemClick);
        }
    };
});
