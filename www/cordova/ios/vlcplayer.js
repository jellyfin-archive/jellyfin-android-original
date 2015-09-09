(function () {

    function vlcRenderer(options) {

        var self = this;

        self.enableProgressReporting = options.type == 'audio';

        function onEnded() {
            Events.trigger(self, 'ended');
        }

        function onTimeUpdate() {
            Events.trigger(self, 'timeupdate');
        }

        function onVolumeChange() {
            Events.trigger(self, 'volumechange');
        }

        function onPlaying() {
            Events.trigger(self, 'playing');
        }

        function onPlay() {
            Events.trigger(self, 'play');
        }

        function onPause() {
            Events.trigger(self, 'pause');
        }

        function onClick() {
            Events.trigger(self, 'click');
        }

        function onDblClick() {
            Events.trigger(self, 'dblclick');
        }

        function onError() {

            var errorCode = this.error ? this.error.code : '';
            Logger.log('Media element error code: ' + errorCode);

            Events.trigger(self, 'error');
        }

        var playerState = {};

        self.currentTime = function (val) {

            if (val != null) {
                window.audioplayer.seekto(function () {
                    Logger.log('Stop succeeded!');
                }, function () {
                    Logger.log('Stop failed!');

                }, val / 1000);
                return;
            }

            return playerState.currentTime;
        };

        self.duration = function (val) {

            if (playerState) {
                return playerState.duration;
            }

            return null;
        };

        self.stop = function () {
            window.audioplayer.pause(function () {
                Logger.log('Stop succeeded!');
                onEnded();

            }, function () {
                Logger.log('Stop failed!');
            });
        };

        self.pause = function () {
            window.audioplayer.pause(function () {
                Logger.log('Pause succeeded!');
                onPause();

            }, function () {
                Logger.log('Pause failed!');
            });
        };

        self.unpause = function () {
            window.audioplayer.pause(function () {
                Logger.log('Unpause succeeded!');
                onPlaying();

            }, function () {
                Logger.log('Unpause failed!');
            });
        };

        self.volume = function (val) {
            if (playerState) {
                if (val != null) {
                    // TODO
                    return;
                }

                return playerState.volume;
            }
        };

        self.setCurrentSrc = function (val, item, mediaSource, tracks) {

            if (!val) {
                self.destroy();
                return;
            }

            var tIndex = val.indexOf('#t=');
            var startPosMs = 0;

            if (tIndex != -1) {
                startPosMs = val.substring(tIndex + 3);
                startPosMs = parseFloat(startPosMs) * 1000;
                val = val.split('#')[0];
            }

            if (options.type == 'audio') {

                // TODO

                //AndroidVlcPlayer.playAudioVlc(val, JSON.stringify(item), JSON.stringify(mediaSource), options.poster);
                var artist = item.ArtistItems && item.ArtistItems.length ? item.ArtistItems[0].Name : null;

                window.audioplayer.playstream(function () {
                    Logger.log('playstream succeeded!');
                    onPlaying();
                }, function () {
                    Logger.log('playstream failed!');
                },
                               {
                                   ios: val
                               },
                                // metadata used for iOS lock screen, Android 'Now Playing' notification
                                {
                                    "title": item.Name,
                                    "artist": artist,
                                    "image": {
                                        "url": options.poster
                                    },
                                    "imageThumbnail": {
                                        "url": options.poster
                                    },
                                    "name": item.Name,
                                    "description": item.Overview
                                }
                             );

                // TODO: Fire revents
                // positionchange
                // playbackstop
                // paused
                // playing

            } else {

            }
        };

        self.currentSrc = function () {
            if (playerState) {
                return playerState.currentSrc;
            }
        };

        self.paused = function () {

            if (playerState) {
                return playerState.paused;
            }

            return false;
        };

        self.cleanup = function (destroyRenderer) {

            if (destroyRenderer !== false) {
                // TODO

            }

            playerState = {};
        };

        self.enableCustomVideoControls = function () {

            return false;
        };

        self.report = function (eventName, duration, position, isPaused, volume) {

            var state = playerState;

            state.duration = duration;
            state.currentTime = position;
            state.paused = isPaused;
            state.volume = (volume || 0) / 100;

            if (eventName == 'playbackstop') {
                onEnded();
            }
            else if (eventName == 'volumechange') {
                onVolumeChange();
            }
            else if (eventName == 'positionchange') {
                onTimeUpdate();
            }
            else if (eventName == 'paused') {
                onPause();
            }
            else if (eventName == 'unpaused') {
                onPlaying();
            }
            else if (eventName == 'playing') {
                onPlaying();
            }
        };

        self.init = function () {

            var deferred = DeferredBuilder.Deferred();
            deferred.resolve();
            return deferred.promise();
        };

        self.onActivityClosed = function (wasStopped, hasError, endPositionMs) {

            playerState.currentTime = endPositionMs;

            if (wasStopped) {
                MediaPlayer.stop(false);
            }

            self.report('playbackstop', playerState.duration, endPositionMs, false, 100);
        };

        window.AudioRenderer.Current = self;
    }

    window.AudioRenderer = function (options) {
        options = options || {};
        options.type = 'audio';

        return new vlcRenderer(options);
    };

})();