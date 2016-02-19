(function () {

    document.addEventListener('click', function (e) {

        var target = e.target;
        if (target.tagName == 'A' && target.getAttribute('target') == '_blank') {

            var url = target.href;
            window.open(url, "_system");
            e.preventDefault();
            return false;
        }
    });

})();