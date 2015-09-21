(function () {

    function onDeviceReady() {

        var fetcher = window.BackgroundFetch;

        fetcher.configure(onBackgroundFetch, onBackgroundFetchFailed, {
            stopOnTerminate: false  // <-- false is default
        });
    }

    function onBackgroundFetch() {

        Logger.log('BackgroundFetch initiated');

        var fetcher = window.BackgroundFetch;

        require(['localsync'], function () {

            var syncOptions = {
                uploadPhotos: false
            };

            LocalSync.startSync(syncOptions).done(function () {

                fetcher.finish();   // <-- N.B. You MUST called #finish so that native-side can signal completion of the background-thread to the os.
            });
        });
    }

    function onBackgroundFetchFailed() {
        Logger.log('- BackgroundFetch failed');
    }

    onDeviceReady();
})();