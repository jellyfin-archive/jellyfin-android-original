(function () {

    function onSuccess() {
        console.log('Immersive mode succeeded');
    }

    function onError() {
        console.log('Immersive mode failed');
    }

    //// Is this plugin supported?
    //AndroidFullScreen.isSupported();

    //// Is immersive mode supported?
    //AndroidFullScreen.isImmersiveModeSupported(onSuccess, onError);

    //// The width of the screen in immersive mode
    //AndroidFullScreen.immersiveWidth(trace, onError);

    //// The height of the screen in immersive mode
    //AndroidFullScreen.immersiveHeight(trace, onError);

    //// Hide system UI until user interacts
    //AndroidFullScreen.leanMode(onSuccess, onError);

    //// Show system UI
    //AndroidFullScreen.showSystemUI(onSuccess, onError);

    //// Extend your app underneath the system UI (Android 4.4+ only)
    //AndroidFullScreen.showUnderSystemUI(onSuccess, onError);

    //// Hide system UI and keep it hidden (Android 4.4+ only)
    AndroidFullScreen.immersiveMode(onSuccess, onError);
})();