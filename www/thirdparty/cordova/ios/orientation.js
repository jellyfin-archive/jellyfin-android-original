(function () {

    function onOrientationChange() {

        // Try to make it react quicker to the orientation change
        $(document).scrollTop($(document).scrollTop() + 1);
    }

    $(window).on('orientationchange', onOrientationChange);

})();