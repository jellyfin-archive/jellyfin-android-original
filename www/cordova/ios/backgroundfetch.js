(function () {

    var lastStart = 0;

    function onDeviceReady() {

        //var fetcher = window.BackgroundFetch;

        //fetcher.configure(onBackgroundFetch, onBackgroundFetchFailed, {
        //    stopOnTerminate: false  // <-- false is default
        //});
    }

    function onSyncFinish() {

        console.log('BackgroundFetch completed');

        var fetcher = window.BackgroundFetch;
        fetcher.finish();   // <-- N.B. You MUST called #finish so that native-side can signal completion of the background-thread to the os.
    }

    function onSyncFail() {

        console.log('BackgroundFetch completed - sync failed');

        var fetcher = window.BackgroundFetch;
        fetcher.finish();   // <-- N.B. You MUST called #finish so that native-side can signal completion of the background-thread to the os.
    }

    function startSync(reportToFetcher, syncOptions) {
        lastStart = new Date().getTime();

        require(['localsync'], function (localSync) {

            if (localSync.getSyncStatus() == 'Syncing') {
                onSyncFinish();
                return;
            }

            var promise = localSync.sync(syncOptions);

            if (reportToFetcher) {
                promise.then(onSyncFinish, onSyncFail);
            }
        });
    }

    function onBackgroundFetch() {

        console.log('BackgroundFetch initiated');

        startSync(true, {
            uploadPhotos: false,
            enableNewDownloads: true
        });
    }

    function onBackgroundFetchFailed() {
        console.log('- BackgroundFetch failed');
    }

    var syncInterval = 900000;
    var photoUploadInterval = 21600000;
    var offlineUserSyncInterval = 43200000;
    function startIntervalSync() {

        startSync(false, {
            uploadPhotos: true,
            enableNewDownloads: true
        });
    }

    function normalizeSyncOptions(options) {

        options.enableBackgroundTransfer = true;

        options.uploadPhotos = (new Date().getTime() - lastStart) >= photoUploadInterval;
        options.syncOfflineUsers = (new Date().getTime() - lastStart) >= offlineUserSyncInterval;
    }

    pageClassOn('pageshow', "libraryPage", function () {

        if (!Dashboard.getCurrentUserId()) {
            return;
        }

        if ((new Date().getTime() - lastStart) >= syncInterval) {

            setTimeout(function () {
                startIntervalSync();

            }, 10000);
        }

    });

    onDeviceReady();
})();