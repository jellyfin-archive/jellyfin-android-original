(function () {

    window.FileSystemBridge = {

        fileExists: function (path) {
            var deferred = DeferredBuilder.Deferred();
            deferred.resolveWith(null, [NativeFileSystem.fileExists(path)]);
            return deferred.promise();
        }
    };

})();