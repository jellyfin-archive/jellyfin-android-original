define(['cryptojs-md5'], function () {

    function setImageIntoElement(elem, url) {

        if (elem.tagName !== "IMG") {

            elem.style.backgroundImage = "url('" + url + "')";

        } else {
            elem.setAttribute("src", url);
        }
    }

    var fileSystem;
    function getFileSystem() {

        if (fileSystem) {
            return Promise.resolve(fileSystem);
        }

        return new Promise(function (resolve, reject) {

            requestFileSystem(PERSISTENT, 0, function (fs) {
                fileSystem = fs;
                resolve(fileSystem);
            });
        });
    }

    function getCacheKey(url) {

        // Try to strip off the domain to share the cache between local and remote connections
        var index = url.indexOf('://');

        if (index != -1) {
            url = url.substring(index + 3);

            index = url.indexOf('/');

            if (index != -1) {
                url = url.substring(index + 1);
            }

        }

        return CryptoJS.MD5(url).toString();
    }

    function normalizeReturnUrl(url) {
        if (browserInfo.safari) {

            // Use the embedded server for iOS8, and also if we don't know the iOS version, just to be safe
            var index = url.indexOf('/Documents');
            if (index != -1) {
                return url.substring(index);
            }
            else {
                return url.replace('file://', '');
            }
        }
        return url;
    }

    function getImageUrl(originalUrl) {

        if (browserInfo.android && originalUrl.indexOf('tag=') != -1) {
            originalUrl += "&accept=webp";
        }

        return new Promise(function (resolve, reject) {

            var key = getCacheKey(originalUrl);

            //console.log('getImageUrl:' + originalUrl);

            getFileSystem().then(function (fileSystem) {
                var path = fileSystem.root.toURL() + "/emby/cache/" + key;

                resolveLocalFileSystemURL(path, function (fileEntry) {
                    var localUrl = normalizeReturnUrl(fileEntry.toURL());
                    //console.log('returning cached file: ' + localUrl);
                    resolve(localUrl);

                }, function () {

                    //console.log('downloading: ' + originalUrl);
                    var ft = new FileTransfer();
                    ft.download(originalUrl, path, function (entry) {

                        var localUrl = normalizeReturnUrl(entry.toURL());

                        //console.log(localUrl);
                        resolve(localUrl);
                    });
                });
            });
        });
    }

    return {
        loadImage: function (elem, url) {

            return getImageUrl(url).then(function (localUrl) {

                setImageIntoElement(elem, localUrl);

            }, function () {
                setImageIntoElement(elem, url);
            });
        }
    };

});