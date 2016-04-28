(function () {

    document.addEventListener('click', function (e) {

        var target = e.target;
        if (target.tagName == 'A' && target.getAttribute('target') == '_blank') {
            var url = target.href || target.getAttribute('href');

            window.plugins.launcher.launch({
                uri: url
            });

            //window.open(url, '_system');
            e.preventDefault();
            return false;
        }
    });

})();