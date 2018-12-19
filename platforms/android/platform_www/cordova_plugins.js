cordova.define('cordova/plugin_list', function(require, exports, module) {
module.exports = [
    {
        "file": "plugins/acidhax.cordova.chromecast/chrome.cast.js",
        "id": "acidhax.cordova.chromecast.ChromecastApi",
        "clobbers": [
            "chrome.cast"
        ]
    },
    {
        "file": "plugins/acidhax.cordova.chromecast/EventEmitter.js",
        "id": "acidhax.cordova.chromecast.EventEmitter"
    },
    {
        "file": "plugins/acidhax.cordova.chromecast/tests/tests.js",
        "id": "acidhax.cordova.chromecast.tests"
    }
];
module.exports.metadata = 
// TOP OF METADATA
{
};
// BOTTOM OF METADATA
});
