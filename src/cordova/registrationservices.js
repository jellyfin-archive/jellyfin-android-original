define(['appSettings', 'loading', 'emby-button'], function (appSettings, loading) {

    function getRegistrationInfo(feature) {
        return ConnectionManager.getRegistrationInfo(feature, ApiClient);
    }

    function validateFeature(feature) {

        console.log('validateFeature: ' + feature);
        var unlockableFeatureCacheKey = 'featurepurchased-' + feature;
        if (appSettings.get(unlockableFeatureCacheKey) == '1') {
            return Promise.resolve();
        }

        var unlockableProduct = IapManager.getProductInfo(feature);

        if (unlockableProduct) {
            console.log('unlockableProduct: ' + JSON.stringify(unlockableProduct));
            var unlockableCacheKey = 'productpurchased-' + unlockableProduct.id;
            if (unlockableProduct.owned) {

                // Cache this to eliminate the store as a possible point of failure in the future
                appSettings.set(unlockableFeatureCacheKey, '1');
                appSettings.set(unlockableCacheKey, '1');
                return Promise.resolve();
            }

            if (appSettings.get(unlockableCacheKey) == '1') {
                return Promise.resolve();
            }
        } else {
            console.log('unlockableProduct not found for this feature.');
        }

        var unlockableProductInfo = unlockableProduct ? {
            enableAppUnlock: true,
            id: unlockableProduct.id,
            price: unlockableProduct.price,
            feature: feature

        } : null;

        var prefix = browserInfo.android ? 'android' : 'ios';

        return IapManager.isUnlockedOverride(feature).then(function (isUnlocked) {

            if (isUnlocked) {
                return Promise.resolve();
            }

            return IapManager.getSubscriptionOptions().then(function (subscriptionOptions) {

                if (subscriptionOptions.filter(function (p) {
                    return p.owned;
                }).length > 0) {
                    return Promise.resolve();
                }

                // Get supporter status
                return getRegistrationInfo(prefix + 'appunlock').catch(function () {

                    var dialogOptions = {
                        title: Globalize.translate('HeaderUnlockApp'),
                        enablePlayMinute: feature == 'playback',
                        feature: feature
                    };

                    return showInAppPurchaseInfo(subscriptionOptions, unlockableProductInfo, dialogOptions);
                });
            });
        });
    }

    function cancelInAppPurchase(dialogHelper) {

        var elem = document.querySelector('.inAppPurchaseOverlay');
        if (elem) {
            dialogHelper.close(elem);
        }
    }

    var currentDisplayingProductInfos = [];
    var currentDisplayingResolve = null;

    function clearCurrentDisplayingInfo() {
        currentDisplayingProductInfos = [];
        currentDisplayingResolve = null;
    }

    function showInAppPurchaseElement(dialogHelper, subscriptionOptions, unlockableProductInfo, dialogOptions, resolve, reject) {

        cancelInAppPurchase();

        // clone
        currentDisplayingProductInfos = subscriptionOptions.slice(0);

        if (unlockableProductInfo) {
            currentDisplayingProductInfos.push(unlockableProductInfo);
        }

        var dlg = dialogHelper.createDialog({
            size: 'fullscreen-border',
            removeOnClose: true
        });

        dlg.classList.add('ui-body-b');
        dlg.classList.add('background-theme-b');

        var html = '';
        html += '<h2 class="dialogHeader">';
        html += '<button is="paper-icon-button-light" class="btnCloseDialog autoSize" tabindex="-1"><i class="md-icon">&#xE5C4;</i></button>';
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
            html += '<button is="emby-button" type="button" class="raised button-submit block btnPurchase" data-email="true" data-feature="' + subscriptionOptions[i].feature + '"><iron-icon icon="check"></iron-icon><span>';
            html += subscriptionOptions[i].buttonText;
            html += '</span></button>';
            html += '</p>';
        }

        if (unlockableProductInfo) {

            hasProduct = true;
            var unlockText = Globalize.translate('ButtonUnlockWithPurchase');
            if (unlockableProductInfo.price) {
                unlockText = Globalize.translate('ButtonUnlockPrice', unlockableProductInfo.price);
            }
            html += '<p>';
            html += '<button is="emby-button" type="button" class="raised secondary block btnPurchase" data-feature="' + unlockableProductInfo.feature + '"><iron-icon icon="check"></iron-icon><span>' + unlockText + '</span></button>';
            html += '</p>';
        }

        if (hasProduct && IapManager.enableRestore(dialogOptions.feature, subscriptionOptions, unlockableProductInfo)) {
            html += '<p>';
            if (browserInfo.safari) {
                html += '<button is="emby-button" type="button" class="raised secondary block btnRestorePurchase subdued"><iron-icon icon="check"></iron-icon><span>' + Globalize.translate('ButtonRestorePreviousPurchase') + '</span></button>';
            } else {
                html += '<button is="emby-button" type="button" class="raised secondary block btnRestorePurchase subdued"><span>' + Globalize.translate('AlreadyPaid') + '</span></button>';
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
            html += '<button is="emby-button" type="button" class="raised secondary block btnCloseDialog subdued"><iron-icon icon="play-arrow"></iron-icon><span>' + Globalize.translate('ButtonPlayOneMinute') + '</span></button>';
            html += '</p>';
        }

        html += '</form>';
        html += '</div>';

        dlg.innerHTML = html;
        document.body.appendChild(dlg);

        var btnPurchases = dlg.querySelectorAll('.btnPurchase');
        for (var i = 0, length = btnPurchases.length; i < length; i++) {
            btnPurchases[i].addEventListener('click', onPurchaseButtonClick);
        }

        var btnRestorePurchase = dlg.querySelector('.btnRestorePurchase');
        if (btnRestorePurchase) {
            btnRestorePurchase.addEventListener('click', function () {

                restorePurchase();
            });
        }

        loading.hide();

        function onCloseButtonClick() {

            var onConfirmed = function () {
                dialogHelper.close(dlg);
                reject();
            };

            if (dialogOptions.feature == 'playback') {
                Dashboard.alert({
                    message: Globalize.translate('ThankYouForTryingEnjoyOneMinute'),
                    title: Globalize.translate('HeaderTryPlayback'),
                    callback: onConfirmed
                });
            } else {
                onConfirmed();
            }
        }

        var btnCloseDialogs = dlg.querySelectorAll('.btnCloseDialog');
        for (var i = 0, length = btnCloseDialogs.length; i < length; i++) {
            btnCloseDialogs[i].addEventListener('click', onCloseButtonClick);
        }

        dlg.addEventListener('close', function () {

            clearCurrentDisplayingInfo();

            if (window.TabBar) {
                TabBar.show();
            }
        });

        dlg.classList.add('inAppPurchaseOverlay');

        dialogHelper.open(dlg);
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

        var enableLink = !browserInfo.safari;

        var html = '';
        html += '<div class="listItem">';

        html += '<i class="listItemIcon md-icon">' + item.icon + '</i>';

        html += '<div class="listItemBody three-line">';

        if (enableLink) {
            html += '<a class="clearLink" href="https://emby.media/premiere" target="_blank">';
        }

        html += '<div class="listItemBodyText">';
        html += item.name;
        html += '</div>';

        html += '<div class="listItemBodyText secondary" style="white-space:normal;">';
        html += item.text;
        html += '</div>';

        if (enableLink) {
            html += '</a>';
        }

        html += '</div>';

        html += '</div>';

        return html;
    }

    function onPurchaseButtonClick() {

        if (this.getAttribute('data-email') == 'true') {
            acquireEmail(this.getAttribute('data-feature'));
        } else {
            IapManager.beginPurchase(this.getAttribute('data-feature'));
        }
    }

    function restorePurchase() {

        require(['dialogHelper'], function (dialogHelper) {

            var dlg = dialogHelper.createDialog({
                size: 'fullscreen-border',
                removeOnClose: true
            });

            dlg.classList.add('ui-body-b');
            dlg.classList.add('background-theme-b');

            var html = '';
            html += '<h2 class="dialogHeader">';
            html += '<button is="paper-icon-button-light" class="btnCloseDialog autoSize" tabindex="-1"><i class="md-icon">&#xE5C4;</i></button>';
            html += '<div style="display:inline-block;margin-left:.6em;vertical-align:middle;">' + Globalize.translate('ButtonRestorePreviousPurchase') + '</div>';
            html += '</h2>';

            html += '<div class="editorContent">';

            html += '<p style="margin:2em 0;">';
            html += Globalize.translate('HowDidYouPay');
            html += '</p>';

            html += '<p>';
            html += '<button is="emby-button" type="button" class="raised secondary block btnRestoreSub subdued"><span>' + Globalize.translate('IHaveEmbyPremiere') + '</span></button>';
            html += '</p>';
            html += '<p>';
            html += '<button is="emby-button" type="button" class="raised secondary block btnRestoreUnlock subdued"><span>' + Globalize.translate('IPurchasedThisApp') + '</span></button>';
            html += '</p>';

            html += '</div>';

            dlg.innerHTML = html;
            document.body.appendChild(dlg);

            loading.hide();

            dialogHelper.open(dlg);

            dlg.querySelector('.btnCloseDialog').addEventListener('click', function () {

                dialogHelper.close(dlg);
            });

            dlg.querySelector('.btnRestoreSub').addEventListener('click', function () {

                dialogHelper.close(dlg);
                Dashboard.alert({
                    message: Globalize.translate('MessageToValidateSupporter'),
                    title: Globalize.translate('TabEmbyPremiere')
                });

            });

            dlg.querySelector('.btnRestoreUnlock').addEventListener('click', function () {

                dialogHelper.close(dlg);
                IapManager.restorePurchase();
            });
        });
    }

    function showInAppPurchaseInfo(subscriptionOptions, unlockableProductInfo, dialogOptions) {

        return new Promise(function (resolve, reject) {

            require(['dialogHelper', 'listViewStyle'], function (dialogHelper) {

                if (window.TabBar) {
                    TabBar.hide();
                }

                showInAppPurchaseElement(dialogHelper, subscriptionOptions, unlockableProductInfo, dialogOptions, resolve, reject);

                currentDisplayingResolve = resolve;
            });
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
                label: Globalize.translate('TextPleaseEnterYourEmailAddressForSubscription')
            }).then(function (email) {
                if (email) {
                    IapManager.beginPurchase(feature, email);
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

                require(['dialogHelper'], function (dialogHelper) {

                    cancelInAppPurchase(dialogHelper);
                    resolve();
                });
            }
        }
    }

    function validateSync() {

        // Get supporter status
        return getRegistrationInfo('Sync').catch(function () {

            return IapManager.getSubscriptionOptions().then(function (subscriptionOptions) {

                var dialogOptions = {
                    title: Globalize.translate('HeaderUnlockSync'),
                    feature: 'sync'
                };

                return showInAppPurchaseInfo(subscriptionOptions, null, dialogOptions);
            });
        });
    }

    function showPremiereInfo() {

        return IapManager.getSubscriptionOptions().then(function (subscriptionOptions) {

            var dialogOptions = {
                title: 'Emby Premiere',
                feature: 'sync'
            };

            return showInAppPurchaseInfo(subscriptionOptions, null, dialogOptions);
        });
    }

    window.RegistrationServices = {

        validateFeature: function (name) {

            if (name == 'playback') {
                return validateFeature(name);
            } else if (name == 'livetv') {
                return validateFeature(name);
            } else if (name == 'sync') {
                return validateSync();
            } else {
                return Promise.resolve();
            }
        },

        showPremiereInfo: showPremiereInfo
    };

    function onIapManagerLoaded() {
        Events.on(IapManager, 'productupdated', onProductUpdated);
    }

    if (browserInfo.android) {
        requirejs(['cordova/android/iap'], onIapManagerLoaded);
    } else {
        requirejs(['cordova/iap'], onIapManagerLoaded);
    }

    return window.RegistrationServices;
});