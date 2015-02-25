angular
  .module('example')
  .controller('SelectServerController', function ($scope, supersonic) {

      supersonic.ui.views.current.whenVisible(function () {

          Header.load();
      });

  });
