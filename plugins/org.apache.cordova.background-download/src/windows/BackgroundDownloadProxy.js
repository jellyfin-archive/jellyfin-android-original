module.exports = {
    startAsync: function (success, fail, args) {
        try {
            var uri = new Windows.Foundation.Uri(args[0]),
                resultFilePath = args[1];

            // Forward slashes (/) are not accepted by getFileFromPathAsync
            // http://msdn.microsoft.com/en-us/library/windows/apps/windows.storage.storagefile.getfilefrompathasync
            resultFilePath = resultFilePath.split('/').join('\\');

            var completeHandler = function() {
                success();
            };
            var errorHandler = function(err) {
                fail(err);
            };
            var progressHandler = function (operation) {

                var progress = {
                    bytesReceived: operation.progress.bytesReceived,
                    totalBytesToReceive: operation.progress.totalBytesToReceive
                };

                success({
                    progress: progress,
                    keepCallback: true
                });
            };

            var downloadLocation;
            
            var downloadPromise = Windows.Storage.StorageFile.getFileFromPathAsync(resultFilePath).then(
                function (file) {
                    downloadLocation = file;
                }, errorHandler).then(function () {
                    return Windows.Networking.BackgroundTransfer.BackgroundDownloader.getCurrentDownloadsAsync();
                }, errorHandler).then(function (downloads) {
                    // After app termination, an app should enumerate all existing DownloadOperation instances at next start-up using
                    // GetCurrentDownloadsAsync. When a Windows Store app using Background Transfer is terminated, incomplete downloads 
                    // will persist in the background. If the app is restarted after termination and operations from the previous
                    // session are not enumerated and re-attached to the current session, they will remain incomplete and continue to occupy resources
                    // http://msdn.microsoft.com/library/windows/apps/br207126
                    for (var i = 0; i < downloads.size; i++) {
                        if (downloads[i].requestedUri.absoluteUri == uri.absoluteUri) {
                            // resume download
                            return downloads[i].attachAsync();
                        }
                    }
                    // new download
                    return Windows.Networking.BackgroundTransfer.BackgroundDownloader().createDownload(uri, downloadLocation).startAsync();
                }, errorHandler);

            // attach callbacks
            downloadPromise.then(completeHandler, errorHandler, progressHandler);
            // save operation promise to be able to stop it later
            BackgroundTransfer.activeDownloads = BackgroundTransfer.activeDownloads || [];
            BackgroundTransfer.activeDownloads[uri.absoluteUri] = downloadPromise;

        } catch(ex) {
            fail(ex);
        }

    },
    stop: function (success, fail, args) {
        try {
            var uri = new Windows.Foundation.Uri(args[0]);

            if (BackgroundTransfer.activeDownloads && BackgroundTransfer.activeDownloads[uri.absoluteUri]) {
                BackgroundTransfer.activeDownloads[uri.absoluteUri].cancel();
                BackgroundTransfer.activeDownloads[uri.absoluteUri] = null;
            }

            success();

        } catch(ex) {
            fail(ex);
        }
    }
};
require("cordova/windows8/commandProxy").add("BackgroundDownload", module.exports);
