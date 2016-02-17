(function () {

    function searchMenu() {

        var self = this;

        self.show = function () {

            require(['css!css/search.css'], function () {
                cordova.searchbar.show();
            });
        };

        self.hide = function () {

            cordova.searchbar.hide();
        };

        document.addEventListener('searchEvent', function (data) {

            Events.trigger(self, 'change', [data.text || '']);

        }, true);

        document.addEventListener('searchClosed', function (data) {

            Events.trigger(self, 'closed');

        }, true);
    }

    window.SearchMenu = new searchMenu();

})();