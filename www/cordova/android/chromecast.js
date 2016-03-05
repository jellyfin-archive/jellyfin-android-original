(function () {

    var PlayerName = 'Chromecast';

    function convertRouteToTarget(route) {

        return {
            name: route.name,
            id: route.id,
            playerName: PlayerName,
            playableMediaTypes: ["Audio", "Video"],
            isLocalPlayer: false,
            appName: PlayerName,
            deviceName: route.name,
            supportedCommands: ["VolumeUp",
                                "VolumeDown",
                                "Mute",
                                "Unmute",
                                "ToggleMute",
                                "SetVolume",
                                "SetAudioStreamIndex",
                                "SetSubtitleStreamIndex",
                                "DisplayContent",
                                "SetRepeatMode",
                                "EndSession"]
        };
    }

    function getTargets() {

        return Promise.resolve(chrome.cast.getRouteList().map(convertRouteToTarget));
    }

    function tryPair(target) {
        return chrome.cast.connectToId(target.id);
    }

    function onChromecastLoaded(e) {
        var player = e.detail.player;

        player.getTargets = getTargets;
        player.tryPair = tryPair;
    }

    if (MainActivity.supportsPlayStore()) {
        // Use native chromecast support
        document.addEventListener('chromecastloaded', onChromecastLoaded);
        require(['scripts/chromecast']);
    }

})();