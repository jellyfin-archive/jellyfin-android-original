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
            supportedCommands: []
        };
    }

    function getTargets() {
        return Promise.reject();
    }

    function tryPair(target) {
        return false;
    }
});
