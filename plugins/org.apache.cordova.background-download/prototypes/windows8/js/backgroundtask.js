//
// A JavaScript background task runs a specified JavaScript file.
//
(function () {
    "use strict";

    //
    // The background task instance's activation parameters are available via Windows.UI.WebUI.WebUIBackgroundTaskInstance.current
    //
    var backgroundTaskInstance = Windows.UI.WebUI.WebUIBackgroundTaskInstance.current;

    console.log("Background " + backgroundTaskInstance.task.name + " Starting...");

    var settings = Windows.Storage.ApplicationData.current.localSettings,
        operations = JSON.parse(settings.values["DownloadOperationsConfig"]) || {};
    
    operations.lastInvokeTime = new Date().getTime();

    for (var p in operations) {
        var operation = operations[p],
            currTime = new Date().getTime();

        // make sure we download new content only after specified interval is reached
        if (operation.lastDownloadTime && (currTime - operation.lastDownloadTime < operation.minInterval * 1000)) {
            continue;
        }

        operation.lastDownloadTime = currTime;

        Windows.Storage.ApplicationData.current.localFolder.createFileAsync(operation.location,
            Windows.Storage.CreationCollisionOption.openIfExists).done(function (file) {
                var uri = new Windows.Foundation.Uri(operation.url);
                Windows.Networking.BackgroundTransfer.BackgroundDownloader().createDownload(uri, file).startAsync();
            });


        // TODO download
    }

    settings.values["DownloadOperationsConfig"] = JSON.stringify(operations);

    close();
    
})();

