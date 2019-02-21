define(['appStorage', 'browser'], function (appStorage, browser) {
    console.log = function () { };

    var deviceId;
    var deviceName;
    var appName;
    var appVersion;

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

                var profile = profileBuilder();

                profile.DirectPlayProfiles.push({
                    // TODO investigate ac3 support
                    Container: "m4v,3gp,ts,mpegts,mov,xvid,vob,mkv,wmv,asf,ogm,ogv,m2v,avi,mpg,mpeg,mp4,webm,wtv",
                    Type: 'Video',
                    AudioCodec: 'aac,aac_latm,mp2,mp3,wma,dca,dts,pcm,PCM_S16LE,PCM_S24LE,opus,flac'
                });

                profile.CodecProfiles = profile.CodecProfiles.filter(function (i) {
                    return i.Type == 'Audio';
                });

                profile.SubtitleProfiles.push(
                    {
                        Format: 'ssa',
                        Method: 'External'
                    },
                    {
                        Format: 'ass',
                        Method: 'External'
                    }
                );

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
            features.push('displaylanguage');
            features.push('subtitleappearancesettings');
            features.push('cameraupload');
            features.push('sharing');
            features.push('exit');
            features.push('htmlaudioautoplay');
            features.push('htmlvideoautoplay');
            features.push('externallinks');
            features.push('multiserver');

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
        preferVisualCards: true,
        moreIcon: 'dots-vert',
        getSyncProfile: getDeviceProfile,
        getDefaultLayout: function() {
            return 'mobile';
        },
        getDeviceProfile: function() {
            return getDeviceProfile();
        },
        init: function() {
            return new Promise(function(resolve) {
                document.addEventListener("deviceready", function () {
                    window.NativeShell.getDeviceInformation(function(result) {
                        // set globally so they can be used elsewhere
                        deviceId = result.deviceId;
                        deviceName = result.deviceName;
                        appName = result.appName;
                        appVersion = result.appVersion;
                        appInfo = {
                            deviceId: deviceId,
                            deviceName: deviceName,
                            appName: appName,
                            appVersion: appVersion
                        };
                        resolve(appInfo);
                    }, function(err) {
                        console.log(err);
                        reject();
                    });
                }, false);
            });
        },
        deviceName: function() {
            return deviceName;
        },
        deviceId: function() {
            return deviceId;
        },
        appName: function() {
            return appName;
        },
        appVersion: function() {
            return appVersion;
        },
        getPushTokenInfo: function() {
            return {};
        },
        setThemeColor: function(color) {
            var metaThemeColor = document.querySelector("meta[name=theme-color]");
            if (metaThemeColor) metaThemeColor.setAttribute("content", color);
        },
        setUserScalable: function(scalable) {
            if (!browser.tv) {
                var att = "width=device-width, initial-scale=1, minimum-scale=1";
                att += scalable ? ", user-scalable=yes" : ", maximum-scale=1, user-scalable=no";
                document.querySelector("meta[name=viewport]").setAttribute("content", att);
            }
        },
        deviceIconUrl: function() {
            // TODO: Need static hosted icons for devices?
            return '';
        }
    };
});
