(function () {

    var currentResolve;
    var currentReject;

    function chooseDirectory() {
        return new Promise(function (resolve, reject) {

            currentResolve = resolve;
            currentReject = reject;

            AndroidDirectoryChooser.chooseDirectory();
        });
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