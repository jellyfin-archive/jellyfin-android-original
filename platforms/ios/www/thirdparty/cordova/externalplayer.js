(function () {

    function showPlayerSelectionMenu(item, url) {

        window.plugins.launcher.launch({
            packageName: 'com.mxtech.videoplayer.ad',
            uri: url,
            dataType: 'application/x-mpegURL'

        }, function () {

            console.log('plugin launch success');

        }, function () {

            console.log('plugin launch error');
        });
    }

    window.ExternalPlayer.showPlayerSelectionMenu = showPlayerSelectionMenu;

})();