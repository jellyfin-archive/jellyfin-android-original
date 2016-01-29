(function () {

    function getRegistrationInfo(feature) {

        return ConnectionManager.getRegistrationInfo(feature, ApiClient);
    }

    function validateFeature(feature, resolve, reject) {

        if (isConnectUserSupporter()) {
            resolve();
            return;
        }

        var unlockableProduct = IapManager.getProductInfo(feature);

        var unlockableProductInfo = unlockableProduct ? {
            enableAppUnlock: true,
            id: unlockableProduct.id,
            price: unlockableProduct.price,
            feature: feature

        } : null;

        if (unlockableProduct && unlockableProduct.owned) {
            resolve();
            return;
        }

        var prefix = browserInfo.android ? 'android' : 'ios';

        IapManager.isUnlockedOverride(feature).then(function (isUnlocked) {

            if (isUnlocked) {
                resolve();
                return;
            }

            function onRegistrationInfoResponse(registrationInfo) {

                if (registrationInfo.IsRegistered) {
                    resolve();
                    return;
                }

                IapManager.getSubscriptionOptions().then(function (subscriptionOptions) {

                    if (subscriptionOptions.filter(function (p) {
                        return p.owned;
                    }).length > 0) {
                        resolve();
                        return;
                    }

                    var dialogOptions = {
                        title: Globalize.translate('HeaderUnlockApp'),
                        enablePlayMinute: feature == 'playback',
                        feature: feature
                    };

                    showInAppPurchaseInfo(subscriptionOptions, unlockableProductInfo, dialogOptions, resolve, reject);
                });
            }

            // Get supporter status
            getRegistrationInfo(prefix + 'appunlock').then(onRegistrationInfoResponse, function () {
                onRegistrationInfoResponse({});
            });
        });
    }

    function isConnectUserSupporter() {

        if (ConnectionManager.isLoggedIntoConnect()) {

            var connectUser = ConnectionManager.connectUser();

            if (connectUser && connectUser.IsSupporter) {
                return true;
            }
        }
        return false;
    }

    function cancelInAppPurchase() {

        var elem = document.querySelector('.inAppPurchaseOverlay');
        if (elem) {
            PaperDialogHelper.close(elem);
        }
    }

    var isCancelled = true;
    var currentDisplayingProductInfos = [];
    var currentDisplayingResolve = null;
    var currentDisplayingReject = null;

    function clearCurrentDisplayingInfo() {
        currentDisplayingProductInfos = [];
        currentDisplayingResolve = null;
        currentDisplayingReject = null;
    }

    function showInAppPurchaseElement(subscriptionOptions, unlockableProductInfo, dialogOptions, resolve, reject) {

        cancelInAppPurchase();

        // clone
        currentDisplayingProductInfos = subscriptionOptions.slice(0);

        if (unlockableProductInfo) {
            currentDisplayingProductInfos.push(unlockableProductInfo);
        }

        var dlg = PaperDialogHelper.createDialog();

        var html = '';
        html += '<h2 class="dialogHeader">';
        html += '<paper-fab icon="arrow-back" mini class="btnCloseDialog"></paper-fab>';
        html += '<div style="display:inline-block;margin-left:.6em;vertical-align:middle;">' + dialogOptions.title + '</div>';
        html += '</h2>';

        html += '<div class="editorContent">';

        html += '<form style="max-width: 800px;margin:auto;">';
        html += '<p style="margin:2em 0;">';

        if (unlockableProductInfo) {
            html += Globalize.translate('MessageUnlockAppWithPurchaseOrSupporter');
        }
        else {
            html += Globalize.translate('MessageUnlockAppWithSupporter');
        }
        html += '</p>';

        html += '<p style="margin:2em 0;">';
        html += Globalize.translate('MessageToValidateSupporter');
        html += '</p>';

        var hasProduct = false;

        for (var i = 0, length = subscriptionOptions.length; i < length; i++) {

            hasProduct = true;
            html += '<p>';
            html += '<paper-button raised class="submit block btnPurchase" data-email="true" data-feature="' + subscriptionOptions[i].feature + '"><iron-icon icon="check"></iron-icon><span>';
            html += subscriptionOptions[i].buttonText;
            html += '</span></paper-button>';
            html += '</p>';
        }

        if (unlockableProductInfo) {

            hasProduct = true;
            var unlockText = Globalize.translate('ButtonUnlockWithPurchase');
            if (unlockableProductInfo.price) {
                unlockText = Globalize.translate('ButtonUnlockPrice', unlockableProductInfo.price);
            }
            html += '<p>';
            html += '<paper-button raised class="secondary block btnPurchase" data-feature="' + unlockableProductInfo.feature + '"><iron-icon icon="check"></iron-icon><span>' + unlockText + '</span></paper-button>';
            html += '</p>';
        }

        if (hasProduct && IapManager.enableRestore(dialogOptions.feature, subscriptionOptions, unlockableProductInfo)) {
            html += '<p>';
            if (browserInfo.safari) {
                html += '<paper-button raised class="secondary block btnRestorePurchase subdued"><iron-icon icon="check"></iron-icon><span>' + Globalize.translate('ButtonRestorePreviousPurchase') + '</span></paper-button>';
            } else {
                html += '<paper-button raised class="secondary block btnRestorePurchase subdued"><span>' + Globalize.translate('AlreadyPaid') + '</span></paper-button>';
            }
            html += '</p>';
        }

        if (subscriptionOptions.length) {
            html += '<br/>';
            html += '<h1>' + Globalize.translate('HeaderBenefitsEmbyPremiere') + '</h1>';

            html += '<div class="paperList" style="margin-bottom:1em;">';
            html += getSubscriptionBenefits().map(getSubscriptionBenefitHtml).join('');
            html += '</div>';
        }

        if (dialogOptions.enablePlayMinute) {
            html += '<p>';
            html += '<paper-button raised class="secondary block btnCloseDialog subdued"><iron-icon icon="play-arrow"></iron-icon><span>' + Globalize.translate('ButtonPlayOneMinute') + '</span></paper-button>';
            html += '</p>';
        }

        html += '</form>';
        html += '</div>';

        dlg.innerHTML = html;
        document.body.appendChild(dlg);

        initInAppPurchaseElementEvents(dlg, dialogOptions.feature, resolve, reject);

        PaperDialogHelper.open(dlg);

        $('.btnCloseDialog', dlg).on('click', function () {

            PaperDialogHelper.close(dlg);
        });

        $(dlg).on('iron-overlay-closed', function () {

            if (window.TabBar) {
                TabBar.show();
            }
        });

        dlg.classList.add('inAppPurchaseOverlay');
    }

    function getSubscriptionBenefits() {

        var list = [];

        list.push({
            name: Globalize.translate('CoverArt'),
            icon: 'photo',
            text: Globalize.translate('CoverArtFeatureDescription')
        });

        list.push({
            name: Globalize.translate('HeaderFreeApps'),
            icon: 'check',
            text: Globalize.translate('FreeAppsFeatureDescription')
        });

        if (Dashboard.capabilities().SupportsSync) {
            list.push({
                name: Globalize.translate('HeaderMobileSync'),
                icon: 'sync',
                text: Globalize.translate('MobileSyncFeatureDescription')
            });
        }
        else if (AppInfo.isNativeApp) {
            list.push({
                name: Globalize.translate('HeaderCloudSync'),
                icon: 'sync',
                text: Globalize.translate('CloudSyncFeatureDescription')
            });
        }
        else {
            list.push({
                name: Globalize.translate('HeaderCinemaMode'),
                icon: 'movie',
                text: Globalize.translate('CinemaModeFeatureDescription')
            });
        }

        return list;
    }

    function getSubscriptionBenefitHtml(item) {

        var html = '';
        html += '<paper-icon-item>';

        html += '<paper-fab mini style="background-color:#52B54B;" icon="' + item.icon + '" item-icon></paper-fab>';

        html += '<paper-item-body three-line>';
        html += '<a class="clearLink" href="https://emby.media/premiere" target="_blank">';

        html += '<div>';
        html += item.name;
        html += '</div>';

        html += '<div secondary style="white-space:normal;">';
        html += item.text;
        html += '</div>';

        html += '</a>';
        html += '</paper-item-body>';

        html += '</paper-icon-item>';

        return html;
    }

    function initInAppPurchaseElementEvents(elem, feature, resolve, reject) {

        isCancelled = true;

        $('.btnPurchase', elem).on('click', function () {

            isCancelled = false;

            if (this.getAttribute('data-email') == 'true') {
                acquireEmail(this.getAttribute('data-feature'));
            } else {
                IapManager.beginPurchase(this.getAttribute('data-feature'));
            }
        });

        $('.btnRestorePurchase', elem).on('click', function () {

            isCancelled = false;
            IapManager.restorePurchase();
        });

        $(elem).on('iron-overlay-closed', function () {

            clearCurrentDisplayingInfo();

            var overlay = this;

            if (isCancelled) {

                if (feature == 'playback') {
                    Dashboard.alert({
                        message: Globalize.translate('ThankYouForTryingEnjoyOneMinute'),
                        title: Globalize.translate('HeaderTryPlayback'),
                        callback: function () {
                            reject();
                            $(overlay).remove();
                        }
                    });
                } else {
                    reject();
                    $(overlay).remove();
                }

            } else {
                $(this).remove();
            }
        });
    }

    function showInAppPurchaseInfo(subscriptionOptions, unlockableProductInfo, dialogOptions, resolve, reject) {

        require(['components/paperdialoghelper', 'paper-fab', 'paper-icon-item', 'paper-item-body'], function () {

            if (window.TabBar) {
                TabBar.hide();
            }

            showInAppPurchaseElement(subscriptionOptions, unlockableProductInfo, dialogOptions, resolve, reject);

            currentDisplayingResolve = resolve;
            currentDisplayingReject = reject;
        });
    }

    function acquireEmail(feature) {

        if (ConnectionManager.isLoggedIntoConnect()) {

            var connectUser = ConnectionManager.connectUser();

            if (connectUser && connectUser.Email) {
                IapManager.beginPurchase(feature, connectUser.Email);
                return;
            }
        }

        promptForEmail(feature);
    }

    function promptForEmail(feature) {

        require(['prompt'], function (prompt) {

            prompt({
                text: Globalize.translate('TextPleaseEnterYourEmailAddressForSubscription'),
                title: Globalize.translate('HeaderEmailAddress'),
                callback: function (email) {

                    if (email) {
                        IapManager.beginPurchase(feature, email);
                    }
                }
            });
        });
    }

    function onProductUpdated(e, product) {

        if (product.owned) {

            var resolve = currentDisplayingResolve;

            if (resolve && currentDisplayingProductInfos.filter(function (p) {

                return product.id == p.id;

            }).length) {

                isCancelled = false;

                cancelInAppPurchase();
                resolve.resolve();
            }
        }
    }

    function validateSync(resolve, reject) {

        Dashboard.getPluginSecurityInfo().then(function (pluginSecurityInfo) {

            if (pluginSecurityInfo.IsMBSupporter) {
                resolve();
                return;
            }

            function onRegistrationInfoResponse(registrationInfo) {
                if (registrationInfo.IsRegistered) {
                    resolve();
                    return;
                }

                IapManager.getSubscriptionOptions().then(function (subscriptionOptions) {

                    var dialogOptions = {
                        title: Globalize.translate('HeaderUnlockSync'),
                        feature: 'sync'
                    };

                    showInAppPurchaseInfo(subscriptionOptions, null, dialogOptions, resolve, reject);
                });
            }

            // Get supporter status
            getRegistrationInfo('Sync').then(onRegistrationInfoResponse, function () {
                onRegistrationInfoResponse({});
            });
        });
    }

    window.RegistrationServices = {

        renderPluginInfo: function (page, pkg, pluginSecurityInfo) {


        },

        validateFeature: function (name) {

            return new Promise(function (resolve, reject) {

                if (name == 'playback') {
                    validateFeature(name, resolve, reject);
                } else if (name == 'livetv') {
                    validateFeature(name, resolve, reject);
                } else if (name == 'sync') {
                    validateSync(resolve, reject);
                } else {
                    resolve();
                }
            });
        }
    };

    function onIapManagerLoaded() {
        Events.on(IapManager, 'productupdated', onProductUpdated);
    }

    if (browserInfo.android) {
        requirejs(['cordova/android/iap'], onIapManagerLoaded);
    } else {
        requirejs(['cordova/iap'], onIapManagerLoaded);
    }

})();