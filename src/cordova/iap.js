define(['appStorage', 'globalize', 'events', 'connectionManager'], function (appStorage, globalize, events, connectionManager) {

    var updatedProducts = [];
    var iapManager = {};

    function updateProductInfo(id, owned, price) {
        var currentProduct = updatedProducts.filter(function (r) {
            return r.id == id;
        })[0];

        if (!currentProduct) {
            currentProduct = {
                id: id,
                owned: owned,
                price: price
            };
            updatedProducts.push(currentProduct);
        }

        currentProduct.price = price;
        currentProduct.owned = currentProduct.owned || owned;
        currentProduct.id = id;

        console.log('Product updated: ' + JSON.stringify(currentProduct));
        events.trigger(iapManager, 'productupdated', [currentProduct]);
    }

    function getProduct(feature) {

        var id;
        if (feature == 'embypremieremonthly') {
            id = NativeIapManager.getPremiereMonthlySku();
        } else if (feature == 'playback' || feature == 'livetv') {
            id = NativeIapManager.getUnlockProductSku();
        } else {
            return null;
        }

        var products = updatedProducts.filter(function (r) {
            return r.id == id;
        });

        return products.length ? products[0] : null;
    }

    function onStoreReady() {
        refreshPurchases();
    }

    function beginPurchase(feature, email) {
        if (feature == 'embypremieremonthly') {
            return NativeIapManager.purchasePremiereMonthly(email);
        }
        return NativeIapManager.purchaseUnlock();
    }

    function onPurchaseComplete(result) {
        if (result === true) {
            refreshPurchases();
        }
        else if (result) {
            var apiClient = connectionManager.currentApiClient();
            apiClient.ajax({
                type: "POST",
                url: apiClient.getUrl("Appstore/Register"),
                data: {
                    Parameters: JSON.stringify(result)
                }
            }).then(function () {
                refreshPurchases();
            }, function (e) {
                refreshPurchases();
            });
        }
    }

    function refreshPurchases() {
        NativeIapManager.getPurchaseInfos("window.IapManager.updateProduct");
    }

    function getStoreFeatureId(feature) {
        // the mapping is handled internally in java
        return feature;
    }

    function getSubscriptionOptions() {
        var options = [];
        options.push({
            feature: 'embypremieremonthly',
            title: 'sharedcomponents#EmbyPremiereMonthlyWithPrice'
        });

        options = options.filter(function (o) {
            var storeProduct = getProduct(o.feature);
            return storeProduct != null;
        }).map(function (o) {
            var storeProduct = getProduct(o.feature);
            o.id = getStoreFeatureId(o.feature);
            o.title = globalize.translate(o.title, storeProduct.price);
            o.owned = storeProduct.owned;
            return o;
        });

        return Promise.resolve(options);
    }

    function isUnlockedByDefault(feature) {
        if (feature == 'playback' || feature == 'livetv') {
            return isPlaybackUnlockedViaOldApp();
        } else {
            return Promise.reject();
        }
    }

    function isPlaybackUnlockedViaOldApp() {
        return Promise.resolve();
    }

    function restorePurchase() {
        var msg = globalize.translate('AlreadyPaidHelp1', 'apps@emby.media');
        msg += '<br/><br/>' + globalize.translate('AlreadyPaidHelp2');

        require(['confirm'], function (confirm) {
            confirm(msg, globalize.translate('sharedcomponents#HeaderAlreadyPaid')).then(launchEmail);
        });
    }

    function launchEmail() {
        // TODO remove
    }

    function getAdminFeatureName(feature) {
        if (feature == 'playback') {
            return 'androidappunlock';
        }
        return feature;
    }

    function getPeriodicMessageIntervalMs(feature) {
        return 0;
    }

    function getRestoreButtonText() {
        return globalize.translate('sharedcomponents#HeaderAlreadyPaid');
    }

    iapManager.getProductInfo = getProduct;
    iapManager.beginPurchase = beginPurchase;
    iapManager.onPurchaseComplete = onPurchaseComplete;
    iapManager.getSubscriptionOptions = getSubscriptionOptions;
    iapManager.isUnlockedByDefault = isUnlockedByDefault;
    iapManager.restorePurchase = restorePurchase;
    iapManager.getAdminFeatureName = getAdminFeatureName;
    iapManager.getPeriodicMessageIntervalMs = getPeriodicMessageIntervalMs;
    iapManager.getRestoreButtonText = getRestoreButtonText;

    // not part of the iap interface, but called by java
    iapManager.onStoreReady = onStoreReady;
    iapManager.updateProduct = updateProductInfo;

    // Called from android
    window.IapManager = iapManager;
    NativeIapManager.initStore();

    return iapManager;
});
