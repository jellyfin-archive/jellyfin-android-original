(function () {


    function onDeviceFound() {

    }

    function onDeviceLost() {

    }

    function initSdk() {

        var manager = discoveryManager;

        manager.setPairingLevel(ConnectSDK.PairingLevel.OFF);
        manager.setAirPlayServiceMode(ConnectSDK.AirPlayServiceMode.Media);

        // Show devices that support playing videos and pausing
        manager.discoveryManager.setCapabilityFilters([
            new ConnectSDK.CapabilityFilter(["MediaPlayer.Display.Video", "MediaControl.Pause"])
        ]);

        manager.addListener('devicefound', onDeviceFound);
        manager.addListener('devicelost', onDeviceLost);

        requirejs(['thirdparty/cordova/chromecast']);
    }

    document.addEventListener("deviceready", function () {

        initSdk();

    }, false);


})();