angular
  .module('example')
  .controller('HomeController', function ($scope, supersonic) {

      function loadContent() {
      }

      supersonic.ui.views.current.whenVisible(function () {

          Header.load();
          loadContent();
      });
  });
