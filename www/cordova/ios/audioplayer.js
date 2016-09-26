(function () {

    function cordovaMediaRenderer(options) {

        var self = this;

        // Need to use this to fire events because the iOS vlc callbacks always go to the first instance
        window.AudioRenderer.Current = self;

        self.enableProgressReporting = options.type == 'audio';
        var currentMedia;

        function onEnded() {
            Events.trigger(window.AudioRenderer.Current, 'ended');
        }

        function onTimeUpdate() {
            Events.trigger(window.AudioRenderer.Current, 'timeupdate');
        }

        function onVolumeChange() {
            Events.trigger(window.AudioRenderer.Current, 'volumechange');
        }

        function onPlaying() {
            Events.trigger(window.AudioRenderer.Current, 'playing');
        }

        function onPlay() {
            Events.trigger(window.AudioRenderer.Current, 'play');
        }

        function onPause() {
            Events.trigger(window.AudioRenderer.Current, 'pause');
        }

        function onClick() {
            Events.trigger(window.AudioRenderer.Current, 'click');
        }

        function onDblClick() {
            Events.trigger(window.AudioRenderer.Current, 'dblclick');
        }

        function onError() {

            var errorCode = this.error ? this.error.code : '';
            console.log('Media element error code: ' + errorCode);

            Events.trigger(window.AudioRenderer.Current, 'error');
        }

        self.playerState = {};

        self.currentTime = function (val) {

            var media = currentMedia;

            if (val != null) {
                if (media) {
                    media.seekTo(val);
                }
                return;
            }

            return self.playerState.currentTime;
        };

        self.duration = function (val) {

            // TODO
            // This value doesn't seem to be getting reported properly
            // Right now it's only used to determine if the player can seek, so for now we can mock it
            return 1;
        };

        self.stop = function () {

            var media = currentMedia;
            if (media) {
                media.stop();
            }
        };

        self.pause = function () {

            var media = currentMedia;
            if (media) {
                media.pause();
                reportEvent('paused');
            }
        };

        self.unpause = function () {

            var media = currentMedia;
            if (media) {
                media.play();
                reportEvent('playing');
            }
        };

        self.volume = function (val) {

            var media = currentMedia;

            if (val != null) {
                if (media) {
                    media.setVolume(val / 100);
                }
                return;
            }

            return self.playerState.volume;
        };

        function releaseMedia(media, isError) {

            if (media.progressInterval) {
                clearInterval(media.progressInterval);
            }
            media.release();

            if (isError) {
                onError();
            } else {
                onEnded();
            }
        }

        self.setCurrentSrc = function (streamInfo, item, mediaSource, tracks) {

            if (!streamInfo) {
                return;
            }

            var val = streamInfo.url;
            var tIndex = val.indexOf('#t=');
            var startPosMs = 0;

            if (tIndex != -1) {
                startPosMs = val.substring(tIndex + 3);
                startPosMs = parseFloat(startPosMs) * 1000;
                val = val.split('#')[0];
            }

            if (options.type == 'audio') {

                var media = new Media(val, function () {

                    releaseMedia(media, false);

                }, function () {

                    releaseMedia(media, true);

                }, function (status) {

                    if (status == 2) {
                        reportEvent('playing');
                    }
                    else if (status == 3) {
                        reportEvent('paused');
                    }
                    else if (status == 4) {

                        releaseMedia(media, false);
                    }

                });

                media.play();

                currentMedia = media;

                media.progressInterval = setInterval(function () {
                    // get media amplitude
                    media.getCurrentPosition(
                        // success callback
                        function (position) {

                            var state = AudioRenderer.Current.playerState;

                            // It will come through as -1 when unknown
                            if (isNaN(position) || position <= 0) {
                                state.currentTime = 0;
                            }
                            else {
                                state.currentTime = position * 1000;
                            }
                            onTimeUpdate();
                        }
                    );
                }, 1000);

            } else {

            }

            AudioRenderer.Current.playerState.currentSrc = val;
        };

        self.currentSrc = function () {
            return self.playerState.currentSrc;
        };

        self.paused = function () {

            return self.playerState.paused;
        };

        self.cleanup = function (destroyRenderer) {

            self.playerState = {};
        };

        function reportEvent(eventName, result) {

            var state = AudioRenderer.Current.playerState;
            state.volume = 0;

            if (eventName == 'playbackstop') {
                state.paused = false;
                onEnded();
            }
            else if (eventName == 'paused') {
                state.paused = true;
                onPause();
            }
            else if (eventName == 'unpaused') {
                state.paused = false;
                onPlaying();
            }
            else if (eventName == 'playing') {
                state.paused = false;
                onPlaying();
            }
        }

        self.init = function () {

            return Promise.resolve();
        };
    }

    window.AudioRenderer = function (options) {
        options = options || {};
        options.type = 'audio';

        return new cordovaMediaRenderer(options);
    };

})();