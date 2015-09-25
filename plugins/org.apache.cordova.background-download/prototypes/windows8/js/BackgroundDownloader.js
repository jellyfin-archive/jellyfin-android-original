"use strict";

var BackgroundDownloader = {
    
    CreateDownload: function(url, location, minInterval) {
        return new DownloadOperation(url, location, minInterval);
    }
};

window.BackgroundDownloader = BackgroundDownloader;