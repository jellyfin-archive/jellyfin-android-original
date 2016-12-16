define([], function () {

    function showMenu(options, successCallback, cancelCallback) {
        return new Promise(function (resolve, reject) {
            var shareInfo = options.share;
            MainActivity.share(shareInfo.Overview, shareInfo.Name, shareInfo.ImageUrl, shareInfo.Url);
            resolve();
        });
    }

    return {
        showMenu: showMenu
    };

});