(function () {

    function fileExists(path) {

        var deferred = DeferredBuilder.Deferred();

        resolveLocalFileSystemURL(path, function (fileEntry) {

            deferred.resolveWith(null, [true]);

        }, function () {

            deferred.resolveWith(null, [false]);
        });

        return deferred.promise();
    }

    window.FileSystemBridge = {

        fileExists: fileExists
    };

})();