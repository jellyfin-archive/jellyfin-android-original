(function () {

    var currentResolve;
    var currentReject;

    function chooseDirectory() {
        return Promise().reject();
    }

    function onChosen(path) {

        var resolve = currentResolve;

        if (resolve) {
            if (path) {
                resolve(path);
            } else {
                reject();
            }

            currentResolve = null;
            currentReject = null;
        }
    }

    window.NativeDirectoryChooser = {
        chooseDirectory: chooseDirectory,
        onChosen: onChosen
    };

})();