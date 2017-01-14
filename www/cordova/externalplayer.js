define(['events', 'appSettings', 'filesystem'], function (events, appSettings, fileSystem) {
    "use strict";

    return function () {

        var self = this;

        self.name = 'External Player';
        self.type = 'mediaplayer';
        self.id = 'externalplayer';

        // Prioritize first
        self.priority = -1;
        self.supportsProgress = false;
        self.isLocalPlayer = true;

        var currentProcess;

        var currentSrc;

        self.canPlayMediaType = function (mediaType) {

            if (mediaType === 'Video') {

                return appSettings.enableExternalPlayers();
            }

            return false;
        };

        self.canPlayItem = function (item, playOptions) {

            if (!playOptions.fullscreen) {
                return false;
            }

            return true;
        };

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

            return modifyStreamUrl(options).then(function (streamUrl) {

                MainActivity.launchIntent(streamUrl, options.mimeType);
                return Promise.resolve();
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
            closePlayer();
        };

        function closePlayer() {

            return Promise.resolve();
        }

        self.pause = function () {
        };

        self.unpause = function () {
        };

        self.paused = function () {
            return false;
        };

        self.volume = function (val) {
        };

        self.setMute = function (mute) {
        };

        self.isMuted = function () {
        };

        function onEnded() {

            currentProcess = null;
            onEndedInternal(true);
        }

        function onError() {

            currentProcess = null;
            events.trigger(self, 'error');
        }

        function onEndedInternal(triggerEnded) {

            if (triggerEnded) {
                var stopInfo = {
                    src: currentSrc
                };

                events.trigger(self, 'stopped', [stopInfo]);
            }

            currentSrc = null;
        }

        self.getDeviceProfile = function () {

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