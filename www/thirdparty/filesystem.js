(function () {

    window.FileSystemBridge = {

        fileExists: function (path) {
            var deferred = DeferredBuilder.Deferred();
            deferred.resolveWith(null, [false]);
            return deferred.promise();
        }
    };
})();