angular
  .module('example')
  .controller('SettingsController', function ($scope, supersonic) {

      supersonic.ui.views.current.whenVisible(function () {

          Header.load();
      });

  });
