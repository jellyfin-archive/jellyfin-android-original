define(['events', 'appSettings', 'filesystem', 'loading'], function (events, appSettings, fileSystem, loading) {
    "use strict";

    return function () {
        var self = this;

        window.ExoPlayer = this;

        self.name = 'ExoPlayer';
        self.type = 'mediaplayer';
        self.id = 'exoplayer';

        // Prioritize first
        self.priority = -1;
        self.supportsProgress = false;
        self.isLocalPlayer = true;
        self._currentTime = 0;
        self._paused = true;
        self.volume = 0;

        var currentSrc;

        self.invokeNativeMethod = function (method, args, successCallback, errorCallback) {
            return window.NativeShell.invokeMethod('exoplayer.' + method, args, successCallback, errorCallback);
        }

        self.canPlayMediaType = function (mediaType) {
            return mediaType === 'Video';
        };

        self.checkTracksSupport = function (videoTracks, audioTracks, subtitleTracks) {
            return new Promise(function (resolve) {
                let successCallback = function (result) {
                    resolve({
                        videoTracks: result.videoTracks,
                        audioTracks: result.audioTracks,
                        subtitleTracks: result.subtitleTracks
                    });
                };

                let errorCallback = function () {
                    resolve(false);
                };

                invokeMethod('checkTracksSupport', [videoTracks, audioTracks, subtitleTracks], successCallback, errorCallback);
            });
        };

        /*self.canPlayItem = function (item, playOptions) {
            if (!playOptions.fullscreen) {
                return false;
            }

            return true;
        };*/

        self.currentSrc = function () {
            return currentSrc;
        };

        function modifyStreamUrl(options) {
            var url = options.url;
            var mediaSource = options.mediaSource;

            if (!mediaSource || mediaSource.Protocol !== 'File' || url === mediaSource.Path) {
                return Promise.resolve(url);
            }

            var method = mediaSource.VideoType === 'BluRay' || mediaSource.VideoType === 'Dvd' || mediaSource.VideoType === 'HdDvd' ?
                'directoryExists' :
                'fileExists';

            return fileSystem[method](mediaSource.Path).then(function () {
                return mediaSource.Path;
            }, function () {
                return url;
            });
        }

        self.play = function (options) {
            return new Promise(function (resolve) {
                self._currentTime = 0;
                self._paused      = false;
                self.invokeNativeMethod('loadPlayer', [options]);
                self._volume = self.invokeNativeMethod('getVolume');
                loading.hide();
                resolve();
            });
        };

        self.setSubtitleStreamIndex = function (index) {
        };

        self.canSetAudioStreamIndex = function () {
            return false;
        };

        self.setAudioStreamIndex = function (index) {
        };

        // Save this for when playback stops, because querying the time at that point might return 0
        self.currentTime = function (val) {
            return null;
        };

        self.duration = function (val) {
            return null;
        };

        self.stop = function (destroyPlayer, reportEnded) {
            return closePlayer().then(function () {
                onEndedInternal(reportEnded);
                return Promise.resolve();
            });
        };

        self.destroy = function () {
            invokeNativeMethod('destroyPlayer');
        };

        self.pause = function () {
            // should not be necessary to implement
        };

        self.unpause = function () {
            // should not be necessary to implement
        };

        self.paused = function () {
            return self._paused;
        };

        self.volume = function (val) {
            // should not be necessary to implement
        };

        self.setMute = function (mute) {
            let unmuted = Number(self._volume) ? self._volume : '0.5'; // if volume is set to zero, then assume half as default when unmuting
            self.invokeNativeMethod('setVolume', [mute ? '0' : unmuted]);
        };

        self.isMuted = function () {
            return Number(self._volume) == 0;
        };

        function onEndedInternal(triggerEnded) {
            if (triggerEnded) {
                var stopInfo = {
                    src: currentSrc
                };

                events.trigger(self, 'stopped', [stopInfo]);
            }

            currentSrc = null;
        }

        self.notifyVolumeChange = function (volume) {
            self._volume = volume;
            events.trigger(self, 'volumechange');
        };

        self.notifyPlay = function () {
            events.trigger(self, 'unpause');
        };

        self.notifyPlaying = function () {
            self._paused = false;
            events.trigger(self, 'playing');
        };

        self.notifyEnded = function () {
            events.trigger(self, 'stopped');
        };

        self.notifyPause = function () {
            self._paused = true;
            events.trigger(self, 'pause');
        };

        self.notifyTimeUpdate = function (currentTime) {
            currentTime = currentTime / 1000;
            self._timeUpdated = self._currentTime != currentTime;
            self._currentTime = currentTime;
            events.trigger(self, 'timeupdate');
        }

        self.currentTime = function () {
            return (self._currentTime || 0) * 1000;
        }

        self.getDeviceProfile = function (item, options) {
            // using native player implementations, check if item can be played. Also check if direct play is supported, as audio is supported.

            return new Promise(function (resolve, reject) {
                require(['browserdeviceprofile'], function (profileBuilder) {
                    var bitrateSetting = appSettings.maxStreamingBitrate();

                    var profile = {};
                    profile.MaxStreamingBitrate = bitrateSetting;
                    profile.MaxStaticBitrate = 100000000;
                    profile.MusicStreamingTranscodingBitrate = 192000;

                    profile.DirectPlayProfiles = [];

                    profile.DirectPlayProfiles.push({
                        Container: 'm4v,3gp,ts,mpegts,mov,xvid,vob,mkv,wmv,asf,ogm,ogv,m2v,avi,mpg,mpeg,mp4,webm,wtv,dvr-ms,iso',
                        Type: 'Video'
                    });

                    profile.DirectPlayProfiles.push({
                        Container: 'aac,mp3,mpa,wav,wma,mp2,ogg,oga,webma,ape,opus,flac',
                        Type: 'Audio'
                    });

                    profile.TranscodingProfiles = [];

                    profile.TranscodingProfiles.push({
                        Container: 'mkv',
                        Type: 'Video',
                        AudioCodec: 'aac,mp3,ac3',
                        VideoCodec: 'h264',
                        Context: 'Streaming'
                    });

                    profile.TranscodingProfiles.push({
                        Container: 'mp3',
                        Type: 'Audio',
                        AudioCodec: 'mp3',
                        Context: 'Streaming',
                        Protocol: 'http'
                    });

                    profile.ContainerProfiles = [];

                    profile.CodecProfiles = [];

                    // Subtitle profiles
                    // External vtt or burn in
                    profile.SubtitleProfiles = [];
                    profile.SubtitleProfiles.push({
                        Format: 'srt',
                        Method: 'Embed'
                    });
                    profile.SubtitleProfiles.push({
                        Format: 'subrip',
                        Method: 'Embed'
                    });
                    profile.SubtitleProfiles.push({
                        Format: 'ass',
                        Method: 'Embed'
                    });
                    profile.SubtitleProfiles.push({
                        Format: 'ssa',
                        Method: 'Embed'
                    });
                    profile.SubtitleProfiles.push({
                        Format: 'pgs',
                        Method: 'Embed'
                    });
                    profile.SubtitleProfiles.push({
                        Format: 'pgssub',
                        Method: 'Embed'
                    });
                    profile.SubtitleProfiles.push({
                        Format: 'dvdsub',
                        Method: 'Embed'
                    });
                    profile.SubtitleProfiles.push({
                        Format: 'vtt',
                        Method: 'Embed'
                    });
                    profile.SubtitleProfiles.push({
                        Format: 'sub',
                        Method: 'Embed'
                    });
                    profile.SubtitleProfiles.push({
                        Format: 'idx',
                        Method: 'Embed'
                    });
                    profile.SubtitleProfiles.push({
                        Format: 'smi',
                        Method: 'Embed'
                    });

                    profile.ResponseProfiles = [];
                    resolve(profile);
                });
            });
        };
    };
});
