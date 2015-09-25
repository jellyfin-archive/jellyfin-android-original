var exec = require('cordova/exec');

var BackgroundDownloader = function () {

};

BackgroundDownloader.prototype.createDownload = function (successCallback, errorCallback, uri, resultFile) {
    console.log("before call exec");
    exec(successCallback, errorCallback, "BackgroundDownloader", "CreateDownload", [uri, resultFile]);
    console.log("after call exec");
};

module.exports = BackgroundDownloader;