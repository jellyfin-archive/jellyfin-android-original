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
            return MainActivity.purchasePremiereMonthly(email);
        }
        return MainActivity.purchaseUnlock();
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

        return testDeviceId(connectionManager.deviceId()).then(function () {

            return Promise.resolve();

        }, function () {

            return testDeviceId(MainActivity.getDeviceId());
        });
    }

    function testDeviceId(deviceId, alias) {

        var cacheKey = 'oldapp5-' + deviceId;
        var cacheValue = appStorage.getItem(cacheKey);
        if (cacheValue == 'true') {
            return Promise.resolve();
        }

        console.log('testing play access for device id: ' + deviceId);

        var fetchUrl = 'http://mb3admin.com/admin/service/statistics/appAccess?application=AndroidV1&deviceId=' + deviceId;
        if (alias) {
            fetchUrl += '&alias=' + alias;
        }

        return fetch(fetchUrl, {

            method: 'GET'

        }).then(function (response) {

            console.log('Play access test for device id: ' + deviceId + '. Response: ' + response.status);

            if (response.status < 400) {
                appStorage.setItem(cacheKey, 'true');
                return Promise.resolve();
            }

            return Promise.reject();

        }, function (e) {

            console.log('Play access test for device id: ' + deviceId + ' failed.');

            return Promise.reject();
        });
    }

    function restorePurchase() {

        var msg = globalize.translate('AlreadyPaidHelp1', 'apps@emby.media');

        msg += '<br/><br/>' + globalize.translate('AlreadyPaidHelp2');

        require(['confirm'], function (confirm) {

            confirm(msg, globalize.translate('sharedcomponents#HeaderAlreadyPaid')).then(launchEmail);

        });
    }

    function launchEmail() {

        var serverInfo = connectionManager.currentApiClient().serverInfo() || {};
        var serverId = serverInfo.Id || 'Unknown';

        var body = 'Order number: ';
        body += '\n\nPlease enter order number above or attach screenshot of order information.';
        body += '\n\n' + serverId + '|' + connectionManager.deviceId();

        MainActivity.sendEmail('apps@emby.media', 'Android Activation', body);
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