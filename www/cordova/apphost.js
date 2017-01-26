define(['appStorage', 'browser'], function (appStorage, browser) {

    console.log = function () { };

    function supportsFullscreen() {

        if (browser.tv) {
            return false;
        };

        var element = document.documentElement;

        return element.requestFullscreen ||
            element.mozRequestFullScreen ||
            element.webkitRequestFullscreen ||
            element.msRequestFullscreen;
    }

    function getDeviceProfile() {

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

                if (self.VlcAudio) {
                    profile.DirectPlayProfiles.push({
                        Container: "aac,mp3,mpa,wav,wma,mp2,ogg,oga,webma,ape,opus,flac,m4a",
                        Type: 'Audio'
                    });
                }

                resolve(profile);
            });
        });
    };

    var appInfo;

    return {
        getWindowState: function () {
            return document.windowState || 'Normal';
        },
        setWindowState: function (state) {
            alert('setWindowState is not supported and should not be called');
        },
        exit: function () {

            if (navigator.app && navigator.app.exitApp) {
                navigator.app.exitApp();
            } else {
                window.close();
            }
        },
        supports: function (command) {

            var features = [];

            //features.push('filedownload');
            features.push('sync');
            features.push('customsyncpath');
            features.push('cameraupload');
            features.push('sharing');
            features.push('exit');
            features.push('htmlaudioautoplay');
            features.push('htmlvideoautoplay');
            features.push('externallinks');
            features.push('multiserver');

            if (MainActivity.getChromeVersion() >= 53) {
                features.push('imageanalysis');
            }

            if (supportsFullscreen()) {
                //features.push('fullscreenchange');
            }

            if (browser.tv || browser.xboxOne || browser.ps4 || browser.mobile) {
                features.push('physicalvolumecontrol');
            }

            if (!browser.tv && !browser.xboxOne && !browser.ps4) {
                features.push('remotecontrol');
            }

            features.push('castmenuhashchange');

            return features.indexOf(command.toLowerCase()) != -1;
        },
        appInfo: function () {

            if (appInfo) {
                return Promise.resolve(appInfo);
            }

            return new Promise(function (resolve, reject) {

                document.addEventListener("deviceready", function () {

                    var name = "Emby for Android Mobile";

                    // Remove special characters
                    var cleanDeviceName = MainActivity.getDeviceModel().replace(/[^\w\s]/gi, '');

                    var deviceId = null;

                    if (window.MainActivity) {

                        deviceId = appStorage.getItem('legacyDeviceId');
                    }

                    appInfo = {
                        deviceId: deviceId || MainActivity.getDeviceId(),
                        deviceName: cleanDeviceName,
                        appName: name,
                        appVersion: MainActivity.getAppVersion()
                    };

                    resolve(appInfo);

                }, false);
            });
        },
        preferVisualCards: true,
        moreIcon: 'dots-vert',
        getSyncProfile: getDeviceProfile
    };
});