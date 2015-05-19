(function () {

    var currentPairingDeviceId;
    var currentDevice;

    function chromecastPlayer() {

        var self = this;

        // MediaController needs this
        self.name = PlayerName;

        self.getItemsForPlayback = function (query) {

            var userId = Dashboard.getCurrentUserId();

            query.Limit = query.Limit || 100;
            query.ExcludeLocationTypes = "Virtual";

            return ApiClient.getItems(userId, query);
        };

        var castPlayer = {};

        $(castPlayer).on("playbackstart", function (e, data) {

            console.log('cc: playbackstart');

            var state = self.getPlayerStateInternal(data);
            $(self).trigger("playbackstart", [state]);
        });

        $(castPlayer).on("playbackstop", function (e, data) {

            console.log('cc: playbackstop');
            var state = self.getPlayerStateInternal(data);

            $(self).trigger("playbackstop", [state]);

            // Reset this so the next query doesn't make it appear like content is playing.
            self.lastPlayerData = {};
        });

        $(castPlayer).on("playbackprogress", function (e, data) {

            console.log('cc: positionchange');
            var state = self.getPlayerStateInternal(data);

            $(self).trigger("positionchange", [state]);
        });

        var endpointInfo;
        function getEndpointInfo() {

            if (endpointInfo) {

                var deferred = $.Deferred();
                deferred.resolveWith(null, [endpointInfo]);
                return deferred.promise();
            }

            return ApiClient.getJSON(ApiClient.getUrl('System/Endpoint')).done(function (info) {

                endpointInfo = info;
            });
        }

        self.play = function (options) {

            Dashboard.getCurrentUser().done(function (user) {

                if (options.items) {

                    self.playWithCommand(options, 'PlayNow');

                } else {

                    self.getItemsForPlayback({

                        Ids: options.ids.join(',')

                    }).done(function (result) {

                        options.items = result.Items;
                        self.playWithCommand(options, 'PlayNow');

                    });
                }

            });

        };

        self.playWithCommand = function (options, command) {

            if (!options.items) {
                ApiClient.getItem(Dashboard.getCurrentUserId(), options.ids[0]).done(function (item) {

                    options.items = [item];
                    self.playWithCommand(options, command);
                });

                return;
            }

            getEndpointInfo().done(function (endpoint) {

                if (endpoint.IsLocal || endpoint.IsInNetwork) {
                    ApiClient.getSystemInfo().done(function (info) {

                        playInternal(options.items, info.LocalAddress);
                    });
                } else {
                    playInternal(options.items);
                }
            });

        };

        function playInternal(items, serverAddress) {
            
            var bitrateSetting = AppSettings.maxChromecastBitrate();

        }

        self.unpause = function () {
            currentDevice.getMediaControl().play();
        };

        self.pause = function () {
            currentDevice.getMediaControl().pause();
        };

        self.shuffle = function (id) {

            var userId = Dashboard.getCurrentUserId();

            ApiClient.getItem(userId, id).done(function (item) {

                self.playWithCommand({

                    items: [item]

                }, 'Shuffle');

            });

        };

        self.instantMix = function (id) {

            var userId = Dashboard.getCurrentUserId();

            ApiClient.getItem(userId, id).done(function (item) {

                self.playWithCommand({

                    items: [item]

                }, 'InstantMix');

            });

        };

        self.canQueueMediaType = function (mediaType) {
            return false;
        };

        self.queue = function (options) {
        };

        self.queueNext = function (options) {
        };

        self.stop = function () {
            currentDevice.getMediaControl().stop();
        };

        self.displayContent = function (options) {

        };

        self.mute = function () {
            currentDevice.getVolumeControl().setMute(true);
        };

        self.unMute = function () {
            currentDevice.getVolumeControl().setMute(false);
        };

        self.toggleMute = function () {

            var volumeControl = currentDevice.getVolumeControl();

            volumeControl.setMute(!volumeControl.getMute());
        };

        function getBaseTargetInfo() {
            var target = {};

            target.playableMediaTypes = ["Audio", "Video"];
            target.isLocalPlayer = false;
            target.supportedCommands = [
                "VolumeUp",
                "VolumeDown",
                "Mute",
                "Unmute",
                "ToggleMute",
                "SetVolume"
            ];

            return target;
        }

        function convertDeviceToTarget(device) {

            var target = getBaseTargetInfo();

            target.playerName = target.appName = target.name = target.deviceName = device.getFriendlyName();
            target.id = device.getId();

            return target;
        }

        function isValid(name) {

            var validTokens = ['airplay'];

            return validTokens.filter(function (t) {

                return name.toLowerCase().replace(' ', '').indexOf(t) != -1;

            }).length > 0;
        }

        self.getTargets = function () {

            var manager = ConnectSDK.discoveryManager;

            return manager.getDeviceList().filter(function (d) {

                return isValid(d.getModelName()) || isValid(d.getFriendlyName());

            }).map(convertDeviceToTarget);
        };

        self.seek = function (position) {
        };

        self.setAudioStreamIndex = function (index) {
        };

        self.setSubtitleStreamIndex = function (index) {
        };

        self.nextTrack = function () {
        };

        self.previousTrack = function () {
        };

        self.beginPlayerUpdates = function () {
            // Setup polling here
        };

        self.endPlayerUpdates = function () {
            // Stop polling here
        };

        function getCurrentVolume() {
            var state = self.lastPlayerData || {};
            state = state.PlayState || {};

            return state.VolumeLevel == null ? 100 : state.VolumeLevel;
        }

        self.volumeDown = function () {

            currentDevice.getVolumeControl().volumeDown();
        };

        self.volumeUp = function () {

            currentDevice.getVolumeControl().volumeUp();
        };

        self.setVolume = function (vol) {

            vol = Math.min(vol, 100);
            vol = Math.max(vol, 0);

            currentDevice.getVolumeControl().setVolume(vol / 100);
        };

        self.getPlayerState = function () {

            var deferred = $.Deferred();

            var result = self.getPlayerStateInternal();

            deferred.resolveWith(null, [result]);

            return deferred.promise();
        };

        self.lastPlayerData = {};

        self.getPlayerStateInternal = function (data) {

            data = data || self.lastPlayerData;
            self.lastPlayerData = data;

            console.log(JSON.stringify(data));
            return data;
        };

        function onDisconnected(device) {
            
            if (currentDevice && device.getId() == currentDevice.getId()) {
                MediaController.removeActiveTarget(device.getId());
            }
        }

        function onDeviceReady(device) {

            if (currentPairingDeviceId != device.getId()) {
                console.log('device ready fired for a different device. ignoring.');
                return;
            }

            currentDevice = device;
        }

        var boundHandlers = [];

        self.tryPair = function (target) {

            var deferred = $.Deferred();

            var manager = ConnectSDK.discoveryManager;

            var device = manager.getDeviceList().filter(function (d) {

                return d.getId() == target.id;
            })[0];

            if (device) {

                var deviceId = device.getId();
                currentPairingDeviceId = deviceId;

                console.log('Will attempt to connect to device');

                if (device.isReady()) {
                    console.log('Device is already ready, calling onDeviceReady');
                    onDeviceReady(device);
                } else {

                    console.log('Binding device ready handler');

                    if (boundHandlers.indexOf(deviceId) == -1) {

                        boundHandlers.push(deviceId);
                        device.on("ready", function () {
                            console.log('device.ready fired');
                            onDeviceReady(device);
                        });
                        device.on("disconnect", function () {
                            console.log('device.disconnect fired');
                            onDisconnected(device);
                        });
                    }

                    console.log('Calling device.connect');
                    device.connect();
                }
                //deferred.resolve();

            } else {
                deferred.reject();
            }

            return deferred.promise();
        };

        $(MediaController).on('playerchange', function (e, newPlayer, newTarget) {

            if (currentDevice && newTarget.id != currentDevice.getId()) {
                MediaController.removeActiveTarget(currentDevice.getId());
            }
        });
    }

    function initSdk() {

        MediaController.registerPlayer(new chromecastPlayer());
    }

    initSdk();

})();