(function () {

    function showPlayerSelectionMenu(item, url, mimeType) {

        var pathToSend = url;
        console.log('Sending path to external player: ' + pathToSend);

        window.plugins.launcher.launch({
            uri: pathToSend,
            dataType: mimeType

        }, function () {

            console.log('plugin launch success');
            ExternalPlayer.onPlaybackStart();

        }, function () {

            console.log('plugin launch error');
            ExternalPlayer.onPlaybackStart();
        });
    }

    window.ExternalPlayer.showPlayerSelectionMenu = showPlayerSelectionMenu;

})();