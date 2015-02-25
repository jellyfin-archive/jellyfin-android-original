(function (globalScope) {

    function showSyncMenu(options) {

    }

    function isAvailable(item, user) {

        return item.SupportsSync;
    }

    globalScope.SyncManager = {

        showMenu: showSyncMenu,

        isAvailable: isAvailable

    };

})(this);