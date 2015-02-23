angular
  .module('example')
  .controller('HomeController', function ($scope, supersonic) {

      function loadContent() {
          //App.loadViewTabs('home');
      }

      supersonic.ui.views.current.whenVisible(function () {
          loadContent();
      });
  });
