define([], function () {

    function showMenu(options, successCallback, cancelCallback) {
        return new Promise(function (resolve, reject) {
            var shareInfo = options.share;
            window.plugins.socialsharing.share(shareInfo.Overview, shareInfo.Name, shareInfo.ImageUrl, shareInfo.Url, resolve, reject);
        });
    }

    return {
        showMenu: showMenu
    };

});