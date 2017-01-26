define(['appSettings', 'events', 'playbackManager', 'connectionManager', 'apphost'], function (appSettings, events, playbackManager, connectionManager, appHost) {

    return function () {

        var self = this;

        self.name = 'Vlc Player';
        self.type = 'mediaplayer';
        self.id = 'vlcplayer';
        self.isLocalPlayer = true;
        self.hasResourceLocks = true;

        window.VlcAudioPlayer = self;
        window.VlcVideoPlayer = self;

        function onEnded() {
            events.trigger(self, 'stopped');
        }

        function onTimeUpdate() {
            events.trigger(self, 'timeupdate');
        }

        function onVolumeChange() {
            events.trigger(self, 'volumechange');
        }

        function onPlaying() {
            events.trigger(self, 'playing');
        }

        function onPause() {
            events.trigger(self, 'pause');
        }

        function onError() {

            var errorCode = this.error ? this.error.code : '';
            console.log('Media element error code: ' + errorCode);

            events.trigger(self, 'error');
        }

        var playerState = {};

        self.canPlayMediaType = function (mediaType) {

            mediaType = (mediaType || '').toLowerCase();

            if (mediaType === 'audio') {
                if (window.VlcAudio) {
                    return true;
                }
                return false;
            }

            if (mediaType === 'video') {
                return true;
            }

            return false;
        };

        self.canPlayItem = function (item, options) {

            if (!options.fullscreen) {
                return false;
            }

            return true;
        };

        self.getDeviceProfile = function () {
            return appHost.getSyncProfile();
        };

        self.currentTime = function (val) {

            if (val != null) {
                return self.seek(val);
            }

            if (playerState) {
                return playerState.currentTime;
            }

            return null;
        };

        self.seek = function (val) {

            AndroidVlcPlayer.sendVlcCommand("setposition", val.toString());
        };

        self.duration = function (val) {

            if (playerState) {
                return playerState.duration;
            }

            return null;
        };

        self.stop = function (destroyPlayer) {

            AndroidVlcPlayer.sendVlcCommand("stop", "true");

            return new Promise(function (resolve, reject) {
                onEnded();
                setTimeout(resolve, 500);
            });
        };

        self.playPause = function () {
            AndroidVlcPlayer.sendVlcCommand("playpause", null);
        };

        self.pause = function () {
            AndroidVlcPlayer.sendVlcCommand("pause", null);
        };

        self.unpause = function () {
            AndroidVlcPlayer.sendVlcCommand("unpause", null);
        };

        self.setVolume = function (val) {
            if (val != null) {
                AndroidVlcPlayer.sendVlcCommand("setvolume", (val * 100).toString());
            }
        };

        self.getVolume = function () {
            if (playerState) {
                return playerState.volume;
            }
        };

        self.volume = function (val) {
            if (val != null) {
                return self.setVolume(val);
            }

            return self.getVolume();
        };

        self.volumeUp = function () {
            self.setVolume(Math.min(self.getVolume() + 2, 100));
        };

        self.volumeDown = function () {
            self.setVolume(Math.max(self.getVolume() - 2, 0));
        };

        self.setMute = function (mute) {

        };

        self.isMuted = function () {
            return false;
        };

        function getPlaybackStartInfoForVideoActivity(streamInfo, mediaSource, item) {

            var playbackStartInfo = {
                QueueableMediaTypes: item.MediaType,
                ItemId: item.Id,
                NowPlayingItem: {},
                MediaSourceId: mediaSource.Id
            };

            if (mediaSource.RunTimeTicks) {
                playbackStartInfo.NowPlayingItem.RunTimeTicks = mediaSource.RunTimeTicks;
            }

            var videoUrl = streamInfo.url;

            if (mediaSource.DefaultAudioStreamIndex != null) {
                playbackStartInfo.AudioStreamIndex = mediaSource.DefaultAudioStreamIndex;
            }

            if (mediaSource.DefaultSubtitleStreamIndex != null) {
                playbackStartInfo.SubtitleStreamIndex = mediaSource.DefaultSubtitleStreamIndex;
            }

            playbackStartInfo.PlayMethod = streamInfo.playMethod;

            playbackStartInfo.LiveStreamId = mediaSource.LiveStreamId;
            playbackStartInfo.PlaySessionId = streamInfo.playSessionId;

            // Seeing some deserialization errors around this property
            if (mediaSource.RunTimeTicks && mediaSource.RunTimeTicks > 0) {
                playbackStartInfo.CanSeek = true;
            }

            return playbackStartInfo;
        }

        self.play = function (options) {

            return new Promise(function (resolve, reject) {

                var item = options.item;
                var mediaSource = options.mediaSource;

                var val = options.url;
                var tIndex = val.indexOf('#t=');
                var startPosMs = (options.playerStartPositionTicks || 0) / 10000;

                if (tIndex != -1) {
                    val = val.split('#')[0];
                }

                var apiClient = connectionManager.getApiClient(item.ServerId);

                if (options.mediaType == 'Audio') {

                    AndroidVlcPlayer.playAudioVlc(val, JSON.stringify(item), JSON.stringify(mediaSource), options.poster);
                    resolve();

                } else {

                    var playbackStartInfo = getPlaybackStartInfoForVideoActivity(options, mediaSource, item);

                    var serverUrl = apiClient.serverAddress();

                    var videoStream = mediaSource.MediaStreams.filter(function (stream) {
                        return stream.Type == "Video";
                    })[0];
                    var videoWidth = videoStream ? videoStream.Width : null;
                    var videoHeight = videoStream ? videoStream.Height : null;

                    require(['qualityoptions'], function (qualityoptions) {

                        var bitrateSetting = appSettings.maxStreamingBitrate();

                        var videoQualityOptions = qualityoptions.getVideoQualityOptions(bitrateSetting, videoWidth).map(function (o) {
                            return {
                                Name: o.name,
                                Value: o.bitrate + "-" + o.maxHeight
                            };
                        });

                        var userStartPos = playbackStartInfo.PlayMethod == 'Transcode' ? ((options.transcodingOffsetTicks || 0) / 10000) : startPosMs;

                        self.getDeviceProfile().then(function (deviceProfile) {

                            deviceProfile.MaxStreamingBitrate = bitrateSetting;

                            AndroidVlcPlayer.playVideoVlc(val,
                                userStartPos,
                                item.Name,
                                JSON.stringify(item),
                                JSON.stringify(mediaSource),
                                JSON.stringify(playbackStartInfo),
                                apiClient.serverInfo().Id,
                                serverUrl,
                                apiClient.appName(),
                                apiClient.appVersion(),
                                apiClient.deviceId(),
                                apiClient.deviceName(),
                                apiClient.getCurrentUserId(),
                                apiClient.accessToken(),
                                JSON.stringify(deviceProfile),
                                JSON.stringify(videoQualityOptions),
                                0);

                            playerState.currentSrc = val;
                            resolve();
                            self.report('playing', null, startPosMs, false, 100);
                        });
                    });
                }
            });
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

        self.destroy = function () {

            AndroidVlcPlayer.destroyVlc();

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
            } else if (eventName == 'volumechange') {
                onVolumeChange();
            } else if (eventName == 'positionchange') {
                onTimeUpdate();
            } else if (eventName == 'paused') {
                onPause();
            } else if (eventName == 'unpaused') {
                onPlaying();
            } else if (eventName == 'playing') {
                onPlaying();
            }
        };

        self.onActivityClosed = function (wasStopped, hasError, endPositionMs) {

            playerState.currentTime = endPositionMs;

            if (wasStopped) {
                playbackManager.stop(self);
            }

            self.report('playbackstop', playerState.duration, endPositionMs, false, 100);
        };
    };
});