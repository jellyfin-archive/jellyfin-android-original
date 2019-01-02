define([], function () {

    return {

        sync: function () {
            AndroidSync.startSync();
        },

        getSyncStatus: function () {
            return AndroidSync.getSyncStatus();
        }
    };

});