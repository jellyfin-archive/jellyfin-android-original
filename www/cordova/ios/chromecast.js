define(['appSettings', 'events', 'jQuery'], function (appSettings, events, $) {

    var GCK = cordova.require('fw-cordova-chromecast.FWChromecast');

    function chromecastPlayer() {

        var self = this;

        var PlayerName = "Chromecast";
        var ApplicationID = "2D4B1DA3";
        var currentDeviceId;
        var currentDeviceFriendlyName;

        // MediaController needs this
        self.name = PlayerName;

        self.getItemsForPlayback = function (query) {

            var userId = Dashboard.getCurrentUserId();

            if (query.Ids && query.Ids.split(',').length == 1) {
                return new Promise(function (resolve, reject) {

                    ApiClient.getItem(userId, query.Ids.split(',')).then(function (item) {
                        resolve({
                            Items: [item],
                            TotalRecordCount: 1
                        });
                    });
                });
            }
            else {

                query.Limit = query.Limit || 100;
                query.ExcludeLocationTypes = "Virtual";

                return ApiClient.getItems(userId, query);
            }
        };

        var castPlayer = {};

        Events.on(castPlayer, "playbackstart", function (e, data) {

            console.log('cc: playbackstart');

            var state = self.getPlayerStateInternal(data);
            Events.trigger(self, "playbackstart", [state]);
        });

        Events.on(castPlayer, "playbackstop", function (e, data) {

            console.log('cc: playbackstop');
            var state = self.getPlayerStateInternal(data);

            Events.trigger(self, "playbackstop", [state]);

            // Reset this so the next query doesn't make it appear like content is playing.
            self.lastPlayerData = {};
        });

        Events.on(castPlayer, "playbackprogress", function (e, data) {

            console.log('cc: positionchange');
            var state = self.getPlayerStateInternal(data);

            Events.trigger(self, "positionchange", [state]);
        });

        function sendMessageToDevice(message) {

            message = Object.assign(message, {
                userId: Dashboard.getCurrentUserId(),
                deviceId: ApiClient.deviceId(),
                accessToken: ApiClient.accessToken(),
                serverAddress: ApiClient.serverAddress(),
                receiverName: currentDeviceFriendlyName || PlayerName
            });

            var bitrateSetting = appSettings.maxChromecastBitrate();
            if (bitrateSetting) {
                message.maxBitrate = bitrateSetting;
            }

            console.log('Sending command to Chromecast: ' + message.command);

            require(['chromecasthelpers'], function (chromecasthelpers) {

                chromecasthelpers.getServerAddress(ApiClient).then(function (serverAddress) {
                    message.serverAddress = serverAddress;
                    sendMessageInternal(message);
                });
            });
        }

        function sendMessageInternal(message) {
            var json = JSON.stringify(message);

            // TODO: Send message
            GCK.sendMessage(json);
        }

        self.play = function (options) {

            Dashboard.getCurrentUser().then(function (user) {

                if (options.items) {

                    self.playWithCommand(options, 'PlayNow');

                } else {

                    self.getItemsForPlayback({

                        Ids: options.ids.join(',')

                    }).then(function (result) {

                        options.items = result.Items;
                        self.playWithCommand(options, 'PlayNow');

                    });
                }

            });

        };

        self.playWithCommand = function (options, command) {

            if (!options.items) {
                ApiClient.getItem(Dashboard.getCurrentUserId(), options.ids[0]).then(function (item) {

                    options.items = [item];
                    self.playWithCommand(options, command);
                });

                return;
            }

            // Convert the items to smaller stubs to send the minimal amount of information
            options.items = options.items.map(function (i) {

                return {
                    Id: i.Id,
                    Name: i.Name,
                    Type: i.Type,
                    MediaType: i.MediaType,
                    IsFolder: i.IsFolder
                };
            });

            sendMessageToDevice({
                options: options,
                command: command
            });
        };

        self.unpause = function () {
            sendMessageToDevice({
                command: 'Unpause'
            });
        };

        self.pause = function () {
            sendMessageToDevice({
                command: 'Pause'
            });
        };

        self.shuffle = function (id) {

            var userId = Dashboard.getCurrentUserId();

            ApiClient.getItem(userId, id).then(function (item) {

                self.playWithCommand({

                    items: [item]

                }, 'Shuffle');

            });

        };

        self.instantMix = function (id) {

            var userId = Dashboard.getCurrentUserId();

            ApiClient.getItem(userId, id).then(function (item) {

                self.playWithCommand({

                    items: [item]

                }, 'InstantMix');

            });

        };

        self.canQueueMediaType = function (mediaType) {
            return mediaType == "Audio";
        };

        self.queue = function (options) {
            self.playWithCommnd(options, 'PlayLast');
        };

        self.queueNext = function (options) {
            self.playWithCommand(options, 'PlayNext');
        };

        self.stop = function () {
            sendMessageToDevice({
                command: 'Stop'
            });
        };

        self.displayContent = function (options) {

            sendMessageToDevice({
                options: options,
                command: 'DisplayContent'
            });
        };

        self.mute = function () {
            sendMessageToDevice({
                command: 'Mute'
            });
        };

        self.unMute = function () {
            self.setVolume(getCurrentVolume() + 2);
        };

        self.toggleMute = function () {

            var state = self.lastPlayerData || {};
            state = state.PlayState || {};

            if (state.IsMuted) {
                self.unMute();
            } else {
                self.mute();
            }
        };

        function getBaseTargetInfo() {
            var target = {};

            target.playerName = PlayerName;
            target.playableMediaTypes = ["Audio", "Video"];
            target.isLocalPlayer = false;
            target.appName = PlayerName;
            target.supportedCommands = [
                "VolumeUp",
                "VolumeDown",
                "Mute",
                "Unmute",
                "ToggleMute",
                "SetVolume",
                "SetAudioStreamIndex",
                "SetSubtitleStreamIndex",
                "DisplayContent",
                "SetRepeatMode",
                "EndSession"
            ];

            return target;
        }

        function isChromecastName(name) {

            name = (name || '').toLowerCase();
            var validTokens = [];
            //validTokens.push('chromecast');
            //validTokens.push('eurekadongle');
            validTokens.push('nexusplayer');

            return validTokens.filter(function (t) {

                return name.replace(' ', '').indexOf(t) != -1;

            }).length > 0;
        }

        function getDeviceList() {

            var devices = GCK.devices || {};

            var list = [];

            for (var id in devices) {

                var device = devices[id];

                list.push({
                    name: device.friendlyName,
                    deviceName: device.friendlyName,
                    playerName: PlayerName,
                    playableMediaTypes: ["Audio", "Video"],
                    isLocalPlayer: false,
                    id: device.id,
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
                });
            }

            return Promise.resolve(list);
        }

        self.getTargets = function () {

            return getDeviceList();
        };

        self.seek = function (position) {

            position = parseInt(position);
            position = position / 10000000;

            sendMessageToDevice({
                options: {
                    position: position
                },
                command: 'Seek'
            });
        };

        self.setAudioStreamIndex = function (index) {
            sendMessageToDevice({
                options: {
                    index: index
                },
                command: 'SetAudioStreamIndex'
            });
        };

        self.setSubtitleStreamIndex = function (index) {
            sendMessageToDevice({
                options: {
                    index: index
                },
                command: 'SetSubtitleStreamIndex'
            });
        };

        self.nextTrack = function () {
            sendMessageToDevice({
                options: {},
                command: 'NextTrack'
            });
        };

        self.previousTrack = function () {
            sendMessageToDevice({
                options: {},
                command: 'PreviousTrack'
            });
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

            sendMessageToDevice({
                options: {},
                command: 'VolumeDown'
            });
        };

        self.setRepeatMode = function (mode) {
            sendMessageToDevice({
                options: {
                    RepeatMode: mode
                },
                command: 'SetRepeatMode'
            });
        };

        self.volumeUp = function () {

            sendMessageToDevice({
                options: {},
                command: 'VolumeUp'
            });
        };

        self.setVolume = function (vol) {

            vol = Math.min(vol, 100);
            vol = Math.max(vol, 0);

            sendMessageToDevice({
                options: {
                    volume: vol
                },
                command: 'SetVolume'
            });
        };

        self.getPlayerState = function () {

            return new Promise(function (resolve, reject) {

                var result = self.getPlayerStateInternal();
                resolve(result);
            });
        };

        self.lastPlayerData = {};

        self.getPlayerStateInternal = function (data) {

            data = data || self.lastPlayerData;
            self.lastPlayerData = data;

            console.log(JSON.stringify(data));
            return data;
        };

        function onMessage(message) {

            if (message.type == 'playbackerror') {

                var errorCode = message.data;

                setTimeout(function () {
                    Dashboard.alert({
                        message: Globalize.translate('MessagePlaybackError' + errorCode),
                        title: Globalize.translate('HeaderPlaybackError')
                    });
                }, 300);

            }
            else if (message.type == 'connectionerror') {

                setTimeout(function () {
                    Dashboard.alert({
                        message: Globalize.translate('MessageChromecastConnectionError'),
                        title: Globalize.translate('HeaderError')
                    });
                }, 300);

            }
            else if (message.type && message.type.indexOf('playback') == 0) {
                Events.trigger(castPlayer, message.type, [message.data]);
            }
        }

        function handleMessage(message) {
            // message could be either a string or an object
            if (typeof message === 'string') {
                onMessage(JSON.parse(message));
            } else {
                onMessage(message);
            }
        }

        function sendIdentifyMessage() {
            sendMessageToDevice({
                options: {},
                command: 'Identify'
            });
        }

        function handleSessionError() {
            console.log('chromecast session connect error');
            cleanupSession();
            MediaController.removeActivePlayer(PlayerName);
        }

        function cleanupSession() {

            self.lastPlayerData = {};
        }

        self.tryPair = function (target) {

            console.log('Will attempt to connect to Chromecast');

            return GCK.selectDevice(target.id).then(function () {

                var onLaunch = function () {
                    currentDeviceId = target.id;
                    currentDeviceFriendlyName = target.name;

                    // in iOS, we don't wait for the opening of the text channel, so give this sufficient time
                    setTimeout(sendIdentifyMessage, 2000);
                };
                return GCK.joinApplication().then(onLaunch, function () {

                    return GCK.launchApplication().then(onLaunch);
                });
            });
        };

        self.endSession = function () {

            console.log('Ending Chromecast session');

            self.stop();

            setTimeout(function () {

                endSessionInternal(true, false);

            }, 1000);
        };

        function endSessionInternal(closeWebApp, retainDeviceId) {

            if (closeWebApp) {
                GCK.disconnect();
            }

            cleanupSession();
            currentDeviceFriendlyName = null;

            if (!retainDeviceId) {
                currentDeviceId = null;
            }
        }

        function tryResume(deviceId, retry) {

            var device = getDeviceList().filter(function (d) {

                return d.getId() == deviceId;
            })[0];

            if (device) {
                self.tryPair(deviceId);
            } else if (retry) {
                setTimeout(function () {
                    tryResume(deviceId, false);
                }, 2000);
            }
        }

        function onResume() {

            var deviceId = currentDeviceId;

            if (deviceId) {

                setTimeout(function () {
                    tryResume(deviceId, true);
                }, 0);
            }
        }

        //document.addEventListener("pause", function () {

        //    endSessionInternal(false, true);

        //}, false);

        document.addEventListener("resume", onResume, false);

        GCK.scanForDevices(ApplicationID);

        $(GCK).on('disconnectWithError', handleSessionError);
        $(GCK).on('deviceDidGoOffline', function (e, data) {

            if (data.id == currentDeviceId) {
                handleSessionError();
            }
        });
        $(GCK).on('receiveMessage', function (e, message) {

            handleMessage(message);
        });
    }

    MediaController.registerPlayer(new chromecastPlayer());

});