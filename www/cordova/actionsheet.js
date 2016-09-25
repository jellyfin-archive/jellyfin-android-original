define(['browser', 'dom'], function (browser, dom) {

    function getOffsets(elems) {

        var doc = document;
        var results = [];

        if (!doc) {
            return results;
        }

        var box;
        var elem;

        for (var i = 0, length = elems.length; i < length; i++) {

            elem = elems[i];
            // Support: BlackBerry 5, iOS 3 (original iPhone)
            // If we don't have gBCR, just use 0,0 rather than error
            if (elem.getBoundingClientRect) {
                box = elem.getBoundingClientRect();
            } else {
                box = { top: 0, left: 0 };
            }

            results[i] = {
                top: box.top,
                left: box.left,
                width: box.width,
                height: box.height
            };
        }

        return results;
    }

    function getPosition(options) {

        var windowSize = dom.getWindowSize();
        var windowHeight = windowSize.innerHeight;
        var windowWidth = windowSize.innerWidth;

        if (windowHeight < 540) {
            return null;
        }

        var pos = getOffsets([options.positionTo])[0];

        if (options.positionY != 'top') {
            pos.top += (pos.height || 0) / 2;
        }

        pos.left += (pos.width || 0) / 2;

        //var height = dlg.offsetHeight || 300;
        //var width = dlg.offsetWidth || 160;

        //// Account for popup size 
        //pos.top -= height / 2;
        //pos.left -= width / 2;

        //// Avoid showing too close to the bottom
        //var overflowX = pos.left + width - windowWidth;
        //var overflowY = pos.top + height - windowHeight;

        //if (overflowX > 0) {
        //    pos.left -= (overflowX + 20);
        //}
        //if (overflowY > 0) {
        //    pos.top -= (overflowY + 20);
        //}

        //pos.top += (options.offsetTop || 0);
        //pos.left += (options.offsetLeft || 0);

        // Do some boundary checking
        pos.top = Math.max(pos.top, 10);
        pos.left = Math.max(pos.left, 10);

        return [pos.left, pos.top];
    }

    function show(options) {

        // items
        // positionTo
        // showCancel
        // title

        // If any items have an icon, give them all an icon just to make sure they're all lined up evenly
        var renderIcon = options.items.filter(function (o) {
            return o.selected;
        }).length;

        if (renderIcon) {

            for (var i = 0, length = options.items.length; i < length; i++) {

                var option = options.items[i];

                if (option.selected) {
                    option.name = '• ' + option.name;
                } else {
                    option.name = '  ' + option.name;
                }
            }
        }

        var innerOptions = {
            'title': options.title,
            'buttonLabels': options.items.map(function (i) {
                return i.name;
            })
        };

        if (options.positionTo && browser.safari) {
            innerOptions.position = getPosition(options);
        }

        // Show cancel unless the caller explicitly set it to false
        if (options.showCancel !== false) {
            innerOptions.addCancelButtonWithLabel = Globalize.translate('ButtonCancel');
        }

        return new Promise(function (resolve, reject) {

            // Depending on the buttonIndex, you can now call shareViaFacebook or shareViaTwitter
            // of the SocialSharing plugin (https://github.com/EddyVerbruggen/SocialSharing-PhoneGap-Plugin)
            window.plugins.actionsheet.show(innerOptions, function (index) {

                // Results are 1-based
                if (index >= 1 && options.items.length >= index) {

                    if (options.callback) {
                        // Results are 1-based
                        options.callback(options.items[index - 1].id);
                    }

                    resolve(options.items[index - 1].id);
                }
            });
        });
    }

    return {
        show: show
    };

});