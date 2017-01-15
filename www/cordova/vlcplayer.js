define(['appSettings', 'events', 'playbackManager', 'connectionManager'], function (appSettings, events, playbackManager, connectionManager) {

    return function () {

        var self = this;

        self.name = 'Vlc Player';
        self.type = 'mediaplayer';
        self.id = 'vlcplayer';
        self.isLocalPlayer = true;

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

        function onPlay() {
            events.trigger(self, 'play');
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

        self.getDeviceProfile = function () {

            return new Promise(function (resolve, reject) {

                require(['browserdeviceprofile'], function (profileBuilder) {

                    var profile = profileBuilder({
                    });

                    profile.DirectPlayProfiles.push({
                        Container: "m4v,3gp,ts,mpegts,mov,xvid,vob,mkv,wmv,asf,ogm,ogv,m2v,avi,mpg,mpeg,mp4,webm,wtv",
                        Type: 'Video',
                        AudioCodec: 'aac,aac_latm,mp2,mp3,ac3,wma,dca,dts,pcm,PCM_S16LE,PCM_S24LE,opus,flac'
                    });

                    profile.CodecProfiles = profile.CodecProfiles.filter(function (i) {
                        return i.Type == 'Audio';
                    });

                    profile.SubtitleProfiles = [];
                    profile.SubtitleProfiles.push({
                        Format: 'srt',
                        Method: 'External'
                    });
                    profile.SubtitleProfiles.push({
                        Format: 'ssa',
                        Method: 'External'
                    });
                    profile.SubtitleProfiles.push({
                        Format: 'ass',
                        Method: 'External'
                    });
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
                        Format: 'dvb_teletext',
                        Method: 'Embed'
                    });
                    profile.SubtitleProfiles.push({
                        Format: 'dvb_subtitle',
                        Method: 'Embed'
                    });
                    profile.SubtitleProfiles.push({
                        Format: 'dvbsub',
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

                    profile.CodecProfiles.push({
                        Type: 'Video',
                        Container: 'avi',
                        Conditions: [
                            {
                                Condition: 'NotEqual',
                                Property: 'CodecTag',
                                Value: 'xvid'
                            }
                        ]
                    });

                    profile.CodecProfiles.push({
                        Type: 'Video',
                        Codec: 'h264',
                        Conditions: [
                        {
                            Condition: 'EqualsAny',
                            Property: 'VideoProfile',
                            Value: 'high|main|baseline|constrained baseline'
                        },
                        {
                            Condition: 'LessThanEqual',
                            Property: 'VideoLevel',
                            Value: '41'
                        }]
                    });

                    //profile.TranscodingProfiles.filter(function (p) {

                    //    return p.Type == 'Video' && p.Container == 'mkv';

                    //}).forEach(function (p) {

                    //    p.Container = 'ts';
                    //});

                    profile.TranscodingProfiles.filter(function (p) {

                        return p.Type == 'Video' && p.CopyTimestamps == true;

                    }).forEach(function (p) {

                        // Vlc doesn't seem to handle this well
                        p.CopyTimestamps = false;
                    });

                    profile.TranscodingProfiles.filter(function (p) {

                        return p.Type == 'Video' && p.VideoCodec == 'h264';

                    }).forEach(function (p) {

                        p.AudioCodec += ',ac3';
                    });

                    profile.DirectPlayProfiles.push({
                        Container: "aac,mp3,mpa,wav,wma,mp2,ogg,oga,webma,ape,opus,flac,m4a",
                        Type: 'Audio'
                    });

                    profile.CodecProfiles = profile.CodecProfiles.filter(function (i) {
                        return i.Type != 'Audio';
                    });

                    profile.CodecProfiles.push({
                        Type: 'Audio',
                        Conditions: [{
                            Condition: 'LessThanEqual',
                            Property: 'AudioChannels',
                            Value: '2'
                        }]
                    });

                    resolve(profile);
                });
            });
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

        self.stop = function () {
            AndroidVlcPlayer.sendVlcCommand("stop", null);
            return Promise.resolve();
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
                var startPosMs = (options.startPositionInSeekParam || 0) * 1000;

                if (tIndex != -1) {
                    val = val.split('#')[0];
                }

                var apiClient = connectionManager.getApiClient(item.ServerId);

                if (options.type == 'audio') {

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

                        var userStartPos = playbackStartInfo.PlayMethod == 'Transcode' ? ((options.startTimeTicksOffset || 0) / 10000) : startPosMs;

                        Dashboard.getDeviceProfile().then(function (deviceProfile) {

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

        self.cleanup = function (destroyRenderer) {

            if (destroyRenderer !== false) {
                AndroidVlcPlayer.destroyVlc();
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

        self.init = function () {

            return Promise.resolve();
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