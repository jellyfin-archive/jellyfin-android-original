
function invokeMethod(method, args, successCallback, errorCallback) {
    return window.NativeShell.invokeMethod('nativePlayer.' + method, args, successCallback, errorCallback);
}

module.exports = {
    loadPlayer: function () {
        invokeMethod('loadPlayer');
    }
};