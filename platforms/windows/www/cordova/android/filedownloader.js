define(['connectionManager'], function (connectionManager) {

    return {
        download: function (downloadItems) {
            downloadItems.forEach(function (downloadItem) {

                var apiClient = connectionManager.getApiClient(downloadItem.serverId);
                apiClient.getItem(apiClient.getCurrentUserId(), downloadItem.itemId).then(function (item) {
                    MainActivity.downloadFile(downloadItem.url, item.Path);
                });
            });
        }
    };
});