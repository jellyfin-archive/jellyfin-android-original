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
        self._currentSrc = null;

        var currentSrc;

        self.invokeNativeMethod = function (method, args, successCallback, errorCallback) {
            return window.NativeShell.invokeMethod('exoplayer.' + method, args, successCallback, errorCallback);
        }

        self.canPlayMediaType = function (mediaType) {
            return mediaType === 'Video';
        };

        self.checkTracksSupport = function (videoTracks, audioTracks, subtitleTracks) {
            return new Promise(function (resolve) {
                var successCallback = function (result) {
                    resolve({
                        videoTracks: result.videoTracks,
                        audioTracks: result.audioTracks,
                        subtitleTracks: result.subtitleTracks
                    });
                };

                var errorCallback = function () {
                    resolve(false);
                };

                invokeMethod('checkTracksSupport', [videoTracks, audioTracks, subtitleTracks], successCallback, errorCallback);
            });
        };

        self.canPlayItem = function (item, playOptions) {
            return true;
        };

        self.currentSrc = function () {
            return self._currentSrc;
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
                self._paused = false;
                self._currentSrc = options.url;
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

        self.destroy = function () {
            self.invokeNativeMethod('destroyPlayer');
        };

        self.pause = function () {
            self._paused = true;
            self.invokeNativeMethod('pausePlayer');
        };

        self.unpause = function () {
            self._paused = false;
            self.invokeNativeMethod('resumePlayer');
        };

        self.paused = function () {
            return self._paused;
        };

        self.stop = function (destroyPlayer) {
            return new Promise(function (resolve) {
                self.invokeNativeMethod('stopPlayer');

                if (destroyPlayer) {
                    self.destroy();
                }

                resolve();
            });
        };

        self.volume = function (val) {
            // should not be necessary to implement
        };

        self.setMute = function (mute) {
            // if volume is set to zero, then assume half as default when unmuting
            var unmuted = Number(self._volume) ? self._volume : '0.5';
            self.invokeNativeMethod('setVolume', [mute ? '0' : unmuted]);
        };

        self.isMuted = function () {
            return Number(self._volume) == 0;
        };

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
            var stopInfo = {
                src: self._currentSrc
            };

            events.trigger(self, 'stopped', [stopInfo]);
            self._currentSrc = self._currentTime = null;
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
            // using native player implementations, check if item can be played
            // also check if direct play is supported, as audio is supported
            return new Promise(function (resolve, reject) {
                require(['browserdeviceprofile'], function (profileBuilder) {
                    var bitrateSetting = appSettings.maxStreamingBitrate();

                    var profile = {};
                    profile.MaxStreamingBitrate = bitrateSetting;
                    profile.MaxStaticBitrate = 100000000;
                    profile.MusicStreamingTranscodingBitrate = 192000;

                    profile.SubtitleProfiles = [];
                    profile.DirectPlayProfiles = [];
                    profile.CodecProfiles = [];

                    var videoProfiles = {
                        '3gp': ['h263', 'h264', 'mpeg4', 'hevc'],
                        'mp4': ['h263', 'h264', 'mpeg4', 'hevc', 'mpeg2video', 'av1', 'mpeg1video'],
                        'ts': ['h264', 'mpeg4'],
                        'webvm': ['vp8', 'vp9'],
                        'mkv': ['h264', 'mpeg4', 'hevc', 'vp8', 'vp9', 'mpeg2video', 'mpeg1video'],
                        'avi': ['h263', 'h264', 'mpeg4', 'hevc', 'vp8', 'vp9', 'mpeg2video', 'mpeg1video'],
                        'flv': ['h264', 'mpeg4'],
                        'asf': ['mpeg2video', 'mpeg4', 'h263', 'h264', 'hevc', 'vp8', 'vp9', 'mpeg1video'],
                        'wmv': ['mpeg2video', 'mpeg4', 'h263', 'h264', 'hevc', 'vp8', 'vp9'],
                        'm2ts': ['mp2g2video', 'mpeg4', 'h264', 'mpeg1video'],
                        'vob': ['mpeg1video', 'mpeg2video'],
                        'mov': ['mpeg1video', 'mpeg2video', 'mpeg4', 'h263', 'h264', 'hevc']
                    };

                    var audioProfiles = {
                        '3gp': ['aac', '3gpp', 'flac'],
                        'mp4': ['aac', 'mp1', 'mp2', 'mp3'],
                        'ts': ['aac', 'mp1', 'mp2', 'mp3', 'ac3', 'dts'],
                        'flac': ['flac'],
                        'aac': ['aac'],
                        'mkv': ['aac', 'dts', 'flac', 'vorbis', 'ac3', 'wma', 'mp1', 'mp2', 'mp3'],
                        'mp3': ['mp3'],
                        'ogg': ['ogg', 'opus', 'vorbis'],
                        'webvm': ['vorbis', 'opus'],
                        'avi': ['flac', 'aac', 'dts', 'ac3', 'wma', 'pcm', 'mp1', 'mp2', 'mp3'],
                        'flv': ['aac', 'mp3'],
                        'asf': ['aac', 'ac3', 'dts', 'wma', 'flac', 'pcm'],
                        'wmv': ['aac', 'ac3', 'dts', 'wma', 'flac', 'pcm'],
                        'm2ts': ['aac', 'ac3', 'dts', 'pcm'],
                        'vob': ['mp1'],
                        'mov': ['mp3', 'aac', 'ac3', 'dts-hd', 'pcm']
                    };

                    // dvdsub is not supported yet
                    var subtitleProfiles = ['srt', 'subrip', 'ass', 'ssa', 'pgs', 'pgssub', 'vtt', 'sub', 'idx', 'smi'];

                    subtitleProfiles.forEach(function (format) {
                        profile.SubtitleProfiles.push({
                            Format: format,
                            Method: 'Embed'
                        });
                    });

                    profile.SubtitleProfiles.push({
                        Format: 'dvdsub',
                        Method: 'Encode'
                    });

                    self.invokeNativeMethod('getSupportedFormats', null, function (codecs) {
                        var videoCodecs = [];
                        var audioCodecs = [];

                        for (var index in codecs.audioCodecs) {
                            if (codecs.audioCodecs.hasOwnProperty(index)) {
                                var audioCodec = codecs.audioCodecs[index];
                                audioCodecs.push(audioCodec.codec);
                            }
                        }

                        for (var index in codecs.videoCodecs) {
                            if (codecs.videoCodecs.hasOwnProperty(index)) {
                                var videoCodec = codecs.videoCodecs[index];
                                videoCodecs.push(videoCodec.codec);

                                var profiles = videoCodec.profiles.join('|');
                                var maxLevel = videoCodec.levels.length && Math.max.apply(null, videoCodec.levels);
                                var conditions = [];

                                if (profiles) {
                                    conditions.push({
                                        Condition: 'EqualsAny',
                                        Property: 'VideoProfile',
                                        Value: profiles
                                    });
                                }

                                if (maxLevel) {
                                    conditions.push({
                                        Condition: 'LessThanEqual',
                                        Property: 'VideoLevel',
                                        Value: maxLevel
                                    });
                                }

                                if (conditions.length) {
                                    profile.CodecProfiles.push({
                                        Type: 'Video',
                                        Codec: videoCodec.codec,
                                        Conditions: conditions
                                    });
                                }
                            }
                        }

                        for (var container in videoProfiles) {
                            if (videoProfiles.hasOwnProperty(container)) {
                                profile.DirectPlayProfiles.push({
                                    Container: container,
                                    Type: 'Video',
                                    AudioCodec: audioProfiles[container].filter(function (codec) {
                                        return audioCodecs.indexOf(codec) !== -1;
                                    }).join(','),
                                    VideoCodec: videoProfiles[container].filter(function (codec) {
                                        return videoCodecs.indexOf(codec) !== -1;
                                    }).join(',')
                                });
                            }
                        }

                        for (var container in audioProfiles) {
                            if (audioProfiles.hasOwnProperty(container)) {
                                profile.DirectPlayProfiles.push({
                                    Container: container,
                                    Type: 'Audio',
                                    VideoCodec: audioProfiles[container].filter(function (codec) {
                                        return audioCodecs.indexOf(codec) !== -1;
                                    }).join(',')
                                });
                            }
                        }

                        profile.TranscodingProfiles = [
                            {
                                Container: 'ts',
                                Type: 'Video',
                                AudioCodec: audioProfiles['ts'].filter(function (codec) {
                                    return audioCodecs.indexOf(codec) !== -1;
                                }).join(','),
                                VideoCodec: 'h264',
                                Context: 'Streaming',
                                Protocol: 'hls',
                                MinSegments: 1
                            },
                            {
                                Container: 'mkv',
                                Type: 'Video',
                                AudioCodec: audioProfiles['mkv'].filter(function (codec) {
                                    return audioCodecs.indexOf(codec) !== -1;
                                }).join(','),
                                VideoCodec: 'h264',
                                Context: 'Streaming'
                            },
                            {
                                Container: 'mp3',
                                Type: 'Audio',
                                AudioCodec: 'mp3',
                                Context: 'Streaming',
                                Protocol: 'http'
                            }
                        ];

                        resolve(profile);
                    });
                });
            });
        };
    };
});
