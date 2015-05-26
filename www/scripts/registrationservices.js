(function () {

    var isStoreReady = false;

    function isAndroid() {

        var platform = (device.platform || '').toLowerCase();

        return platform.indexOf('android') != -1;
    }

    function validatePlayback(deferred) {

        // Don't require validation on android
        if (isAndroid()) {
            deferred.resolve();
            return;
        }

        deferred.resolve();
    }

    function validateLiveTV(deferred) {

        // Don't require validation if not android
        if (!isAndroid()) {
            deferred.resolve();
            return;
        }

        deferred.resolve();
    }

    window.RegistrationServices = {

        renderPluginInfo: function (page, pkg, pluginSecurityInfo) {


        },

        addRecurringFields: function (page, period) {

        },

        initSupporterForm: function (page) {

            $('.recurringSubscriptionCancellationHelp', page).html('');
        },

        validateFeature: function (name) {
            var deferred = DeferredBuilder.Deferred();

            if (name == 'playback') {
                validatePlayback(deferred);
            } else if (name == 'livetv') {
                validateLiveTV(deferred);
            } else {
                deferred.resolve();
            }

            return deferred.promise();
        }
    };

    function initializeStore() {

        // Let's set a pretty high verbosity level, so that we see a lot of stuff
        // in the console (reassuring us that something is happening).
        store.verbosity = store.INFO;

        if (isAndroid) {
            //store.register({
            //    id: "com.example.app.inappid1",
            //    alias: "100 coins",
            //    type: store.CONSUMABLE
            //});
        }

        // When every goes as expected, it's time to celebrate!
        // The "ready" event should be welcomed with music and fireworks,
        // go ask your boss about it! (just in case)
        store.ready(function () {
            console.log("Store ready");
            isStoreReady = true;
        });

        // After we've done our setup, we tell the store to do
        // it's first refresh. Nothing will happen if we do not call store.refresh()
        store.refresh();
    }

    // We must wait for the "deviceready" event to fire
    // before we can use the store object.
    document.addEventListener('deviceready', initializeStore, false);

})();