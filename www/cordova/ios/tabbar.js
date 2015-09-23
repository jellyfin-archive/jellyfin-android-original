(function () {

    function onTabSelected(name) {
        var msg = "Selected " + name;
        console.log(msg);
    }

    var initComplete = false;

    function init() {

        // Use system defined items for this demo
        // If an image is passed, label is not used
        var items = [
          { name: 'Featured', label: 'label', image: 'tabButton:Featured', options: {} },
          { name: 'Search', label: 'label', image: 'tabButton:Search', options: {} },
          { name: 'History', label: 'label', image: 'tabButton:History', options: {} },
          { name: 'Bookmarks', label: 'label', image: 'tabButton:Bookmarks', options: { badge: '3' } }
        ];

        for (var i = 0; i < items.length; i++) {
            var item = items[i];
            var options = item.options;
            // set the function to invoke when the item is selected
            options.onSelect = onTabSelected;
            TabBar.createItem(item.name, item.label, item.image, item.options);
        };

        TabBar.showItems();
        initComplete = true;
        showTabs();
    }

    function showTabs() {

        if (!initComplete) {
            return;
        }

        TabBar.show();
    }

    function hideTabs() {

        if (!initComplete) {
            return;
        }

        TabBar.hide();
    }

    Dashboard.ready(function () {

        init();

        Events.on(ConnectionManager, 'localusersignedin', showTabs);
        Events.on(ConnectionManager, 'localusersignedout', hideTabs);
    });

    pageClassOn('pageshowready', "page", function () {

        var page = this;

        if (page.classList.contains('libraryPage')) {
            showTabs();
        }
        else {
            hideTabs();
        }
    });

})();