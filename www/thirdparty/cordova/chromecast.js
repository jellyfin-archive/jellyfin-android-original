(function () {

    var PlayerName = "Chromecast";
    var ApplicationID = "F4EB2E8E";
    var currentDeviceId;
    var currentWebAppSession;

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

        $(castPlayer).on("connect", function (e) {

            console.log('cc: connect');
            // Reset this so the next query doesn't make it appear like content is playing.
            self.lastPlayerData = {};
        });

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

        function sendMessageToDevice() {

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
                command: 'unpause'
            });
        };

        self.pause = function () {
            sendMessageToDevice({
                command: 'pause'
            });
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
                command: 'stop'
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
                command: 'mute'
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
                "DisplayContent"
            ];

            return target;
        }

        function convertDeviceToTarget(device) {

            var target = getBaseTargetInfo();

            target.name = target.deviceName = device.getFriendlyName();
            target.id = device.getId();

            return target;
        }

        function isChromecast(name) {

            var validTokens = ['nexusplayer', 'chromecast', 'eurekadongle'];

            return validTokens.filter(function (t) {

                return name.toLowerCase().indexOf(t) != -1;

            }).length > 0;
        }

        self.getTargets = function () {

            var manager = ConnectSDK.discoveryManager;

            return manager.getDeviceList().filter(function (d) {

                return isChromecast(d.getModelName()) || isChromecast(d.getFriendlyName());

            }).map(convertDeviceToTarget);
        };

        self.seek = function (position) {
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

            self.setVolume(getCurrentVolume() - 2);
        };

        self.volumeUp = function () {

            self.setVolume(getCurrentVolume() + 2);
        };

        self.setVolume = function (vol) {

            vol = Math.min(vol, 100);
            vol = Math.max(vol, 0);

            sendMessageToDevice({
                options: {
                    volume: (vol / 100)
                },
                command: 'SetVolume'
            });
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

        var readyHandlers = [];

        function onDeviceReady(device) {

            if (currentDeviceId != device.getId()) {
                return;
            }

            device.getWebAppLauncher().launchWebApp(ApplicationID).success(function (session) {

                currentWebAppSession = session.acquire(); // hold on to a reference

                session.connect().success(function () {
                    //session.sendText("Hello world");
                });

                session.on('message', function (message) {
                    // message could be either a string or an object
                    if (typeof message === 'string') {
                        console.log("received string message: " + message);
                    } else {
                        console.log("received object message: " + JSON.stringify(message));
                    }
                });

                session.on('disconnect', function () {
                    console.log("session disconnected");

                    if (currentDeviceId == device.getId()) {
                        currentWebAppSession = null;
                        MediaController.removeActivePlayer(PlayerName);
                    }

                });

            });
        }

        self.tryPair = function (target) {

            var deferred = $.Deferred();

            var manager = ConnectSDK.discoveryManager;

            var device = manager.getDeviceList().filter(function (d) {

                return g.getId() == target.id;
            })[0];

            if (device) {

                if (device.isReady()) {
                    onDeviceReady(device);
                } else {

                    var deviceId = device.getId();

                    if (readyHandlers.indexOf(deviceId) == -1) {
                        readyHandlers.push(deviceId);

                        device.on("ready", function () {
                            onDeviceReady(device);
                        });
                    }

                    device.connect();
                }

                currentDeviceId = device.getId();
                deferred.resolve();
            } else {
                deferred.reject();
            }

            return deferred.promise();
        };
    }

    function initSdk() {

        MediaController.registerPlayer(new chromecastPlayer());
    }

    initSdk();

})();