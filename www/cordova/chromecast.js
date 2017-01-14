define(['pluginManager'], function (pluginManager) {

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

        var plugin = pluginManager.plugins().filter(function (p) {
            return p.id === 'chromecast';
        })[0];

        if (plugin) {
            plugin.tryPair = tryPair;
            plugin.getTargets = getTargets;
        }
    }

    if (MainActivity.supportsPlayStore()) {
        // Use native chromecast support
        document.addEventListener('chromecastloaded', onChromecastLoaded);
    }

    onChromecastLoaded();

});