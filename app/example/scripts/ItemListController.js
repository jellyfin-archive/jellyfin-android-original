angular
  .module('example')
  .controller('ItemListController', function ($scope, supersonic) {

      supersonic.ui.views.current.whenVisible(function () {

          Header.load();
      });

  });
