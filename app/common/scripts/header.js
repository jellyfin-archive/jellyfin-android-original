(function (globalScope) {

    globalScope.Header = function () {

        var self = {};

        function onMenuButtonTap() {
            supersonic.ui.drawers.open("left");
        }

        function onSearchTap() {

        }

        function onPlayerTap() {

        }

        function getTitle() {

            return store.getItem('viewName') || "Media Browser";
        }

        function loadMenu(isSignedIn) {

            var menuButton = new steroids.buttons.NavigationBarButton();
            menuButton.imagePath = "/images/menu.png";
            menuButton.imageAsOriginal = "true";
            menuButton.onTap = onMenuButtonTap;
            menuButton.styleClass = "nav-button";

            var searchButton = new steroids.buttons.NavigationBarButton();
            searchButton.imagePath = "/images/search.png";
            searchButton.imageAsOriginal = "true";
            searchButton.onTap = onSearchTap;
            searchButton.styleClass = "nav-button";

            var playerButton = new steroids.buttons.NavigationBarButton();
            playerButton.imagePath = "/images/player.png";
            playerButton.imageAsOriginal = "true";
            playerButton.onTap = onPlayerTap;
            playerButton.styleClass = "nav-button";

            var right = isSignedIn ? [searchButton, playerButton] : [];

            steroids.view.navigationBar.update({
                overrideBackButton: true,
                buttons: {
                    left: [menuButton],
                    right: right
                },
                title: getTitle()
            }, {
                onSuccess: function () {
                    steroids.view.navigationBar.show();
                }
            });
        }

        self.load = function (isSignedIn) {

            loadMenu(isSignedIn !== false);

        };

        return self;
    }();

})(this);