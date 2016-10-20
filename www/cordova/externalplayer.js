(function () {

    function showPlayerSelectionMenu(item, url, mimeType) {

        var pathToSend = url;
        console.log('Sending path to external player: ' + pathToSend);

        MainActivity.launchIntent(pathToSend, mimeType);
        ExternalPlayer.onPlaybackStart();
    }

    window.ExternalPlayer.showPlayerSelectionMenu = showPlayerSelectionMenu;

})();