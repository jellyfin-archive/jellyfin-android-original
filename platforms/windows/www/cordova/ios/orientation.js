define([], function () {

    function forceScroll() {

        // Try to make it react quicker to the orientation change
        window.scrollTo(0, window.pageYOffset + 1);
    }

    function onOrientationChange() {

        forceScroll();
        for (var i = 0; i <= 500; i += 100) {
            setTimeout(forceScroll, i);
        }
    }

    window.addEventListener('orientationchange', onOrientationChange);

});