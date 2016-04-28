define([], function () {

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