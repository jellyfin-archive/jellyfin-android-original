
exports.defineAutoTests = function() {

  var appId = "CC1AD845"; // kGCKMediaDefaultReceiverApplicationID app id
  var GCK = cordova.require('fw-cordova-chromecast.FWChromecast');

  describe('Test loading chromecast plugin', function() {

    it('plugin object should not be undefined', function() {
      expect(typeof(GCK)).toBe("object");
      expect(Object.keys(GCK).length > 0).toBe(true);
    });
  });

  describe('Test launching app', function() {
    it('Should find a devie, connect to it and launch app', function(done) {
      $(GCK).on('GCK.deviceDidComeOnline', function(e, device){
        expect(Object.keys(GCK.devices).length > 0).toBe(true);
        expect(typeof(device)).toBe("object");
        expect(typeof(device.friendlyName)).toBe("string");
        expect(typeof(device.id)).toBe("string");
        expect(typeof(device.ipAddress)).toBe("string");
        expect(typeof(device.servicePort)).toBe("number");

        GCK.selectDevice(device.id).then(function(device){
          expect(typeof(device)).toBe("object");
          expect(typeof(GCK.connected)).toBe("object");
          expect(JSON.stringify(device)).toBe(JSON.stringify(GCK.connected));
          expect(GCK.connected).toBe(GCK.devices[Object.keys(GCK.devices)[0]]);

          GCK.launchApplication().then(function(metadata){
            expect(typeof(metadata)).toBe("object");
            expect(typeof(metadata.applicationID)).toBe("string");
            expect(typeof(metadata.applicationName)).toBe("string");

            GCK.loadMedia("My title", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "video/mp4", "My subtitle");
            setTimeout(function(){
              GCK.pauseMedia();
              setTimeout(function(){
                GCK.playMedia();
                setTimeout(function(){
                  GCK.muteMedia(true);
                  setTimeout(function(){
                    GCK.muteMedia(false);
                    GCK.seekMedia(1);
                    setTimeout(function(){
                      GCK.setVolumeForMedia(0.2);
                      setTimeout(function(){
                        GCK.stopMedia();
                        done();
                      }, 2000);
                    }, 2000);
                  }, 2000);
                }, 2000);
              }, 2000);
            }, 5000);
          });
        });
      });
      GCK.scanForDevices(appId);
    });
  });
};
