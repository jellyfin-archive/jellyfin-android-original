window.RegistrationServices = {

    renderPluginInfo: function (page, pkg, pluginSecurityInfo) {


    },

    addRecurringFields: function (page, period) {

    },

    initSupporterForm: function (page) {

        $('.recurringSubscriptionCancellationHelp', page).html('');
    },

    validatePlayback: function () {
        var deferred = DeferredBuilder.Deferred();
        deferred.resolve();
        return deferred.promise();
    }
};