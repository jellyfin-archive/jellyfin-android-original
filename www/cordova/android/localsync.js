(function () {

    window.LocalSync = {

        sync: function () {
            AndroidSync.startSync();
        },

        getSyncStatus: function () {
            return AndroidSync.getSyncStatus();
        }
    };

})();