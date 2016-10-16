define(['appStorage'], function (appStorage) {

    console.log = function () { };

    function getDeviceProfile() {

        // TODO
        return null;
    }

    function getCapabilities() {

        var caps = {
            PlayableMediaTypes: ['Audio', 'Video'],

            SupportsPersistentIdentifier: false,
            DeviceProfile: getDeviceProfile()
        };

        return caps;
    }

    var appInfo;

    return {
        getWindowState: function () {
            return document.windowState || 'Normal';
        },
        setWindowState: function (state) {
            alert('setWindowState is not supported and should not be called');
        },
        exit: function () {

            if (navigator.app && navigator.app.exitApp) {
                navigator.app.exitApp();
            } else {
                window.close();
            }
        },
        supports: function (command) {

            var features = [];

            features.push('filedownload');
            features.push('sync');
            features.push('customsyncpath');
            features.push('cameraupload');
            features.push('sharing');
            features.push('exit');
            features.push('htmlaudioautoplay');
            features.push('htmlvideoautoplay');
            features.push('externallinks');

            if (MainActivity.getChromeVersion() >= 53) {
                features.push('imageanalysis');
            }

            return features.indexOf(command.toLowerCase()) != -1;
        },
        appInfo: function () {

            if (appInfo) {
                return Promise.resolve(appInfo);
            }

            return new Promise(function (resolve, reject) {

                document.addEventListener("deviceready", function () {

                    var name = "Emby for Android Mobile";

                    // Remove special characters
                    var cleanDeviceName = MainActivity.getDeviceModel().replace(/[^\w\s]/gi, '');

                    var deviceId = null;

                    if (window.MainActivity) {

                        deviceId = appStorage.getItem('legacyDeviceId');
                    }

                    appInfo = {
                        deviceId: deviceId || MainActivity.getDeviceId(),
                        deviceName: cleanDeviceName,
                        appName: name,
                        appVersion: MainActivity.getAppVersion()
                    };

                    resolve(appInfo);

                }, false);
            });
        },
        capabilities: getCapabilities,
        preferVisualCards: true,
        moreIcon: 'dots-vert'
    };
});