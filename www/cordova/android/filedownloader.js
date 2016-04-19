define([], function () {

    return {
        download: function (downloadItems) {
            downloadItems.forEach(function (downloadItem) {
                ApiClient.getItem(Dashboard.getCurrentUserId(), downloadItem.itemId).then(function (item) {
                    MainActivity.downloadFile(downloadItem.url, item.Path);
                });
            });
        }
    };
});