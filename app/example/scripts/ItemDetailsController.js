angular
  .module('example')
  .controller('ItemDetailsController', function ($scope, supersonic) {

      supersonic.ui.views.current.whenVisible(function () {

          Header.load();
      });

  });
