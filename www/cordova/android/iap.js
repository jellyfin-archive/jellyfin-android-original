(function () {

    var updatedProducts = [];

    function updateProductInfo(id, owned, price) {

        updatedProducts = updatedProducts.filter(function (r) {
            return r.id != id;
        });

        var product = {
            id: id,
            owned: owned,
            price: price
        };

        updatedProducts.push(product);

        Events.trigger(IapManager, 'productupdated', [product]);
    }

    function getProduct(feature) {

        var id;
        if (feature == 'embypremieremonthly') {
            id = NativeIapManager.getPremiereMonthlySku();
        } else {
            id = NativeIapManager.getUnlockProductSku();
        }

        var products = updatedProducts.filter(function (r) {
            return r.id == id;
        });

        return products.length ? products[0] : null;
    }

    var storeReady = false;
    function onStoreReady() {
        storeReady = true;
        refreshPurchases();
    }

    function isPurchaseAvailable() {

        return storeReady;
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

            ApiClient.ajax({
                type: "POST",
                url: ApiClient.getUrl("Appstore/Register"),
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

    function getSubscriptionOptions() {

        return new Promise(function (resolve, reject) {

            var options = [];

            options.push({
                feature: 'embypremieremonthly',
                buttonText: 'EmbyPremiereMonthly'
            });

            options = options.filter(function (o) {
                return getProduct(o.feature) != null;

            }).map(function (o) {

                var prod = getProduct(o.feature);
                o.buttonText = Globalize.translate(o.buttonText, prod.price);
                o.owned = prod.owned;
                return o;
            });

            resolve(options);
        });
    }

    function isUnlockedOverride(feature) {

        if (feature == 'playback') {
            return isPlaybackUnlockedViaOldApp();
        } else {
            return new Promise(function (resolve, reject) {

                resolve(false);
            });
        }
    }

    function isPlaybackUnlockedViaOldApp() {

        return testDeviceId(ConnectionManager.deviceId()).then(function (isUnlocked) {

            if (isUnlocked) {
                return true;
            }

            return testDeviceId(device.uuid).then(function (isUnlocked) {

                if (isUnlocked) {
                    return true;
                }

                return testDeviceId(MainActivity.getAndroidDeviceId()).then(function (isUnlocked) {

                    if (isUnlocked) {
                        return true;
                    }

                    var legacyDeviceId = MainActivity.getLegacyDeviceId();
                    if (legacyDeviceId) {
                        return testDeviceId(legacyDeviceId);
                    }

                    return false;
                })
            });
        });
    }

    function testDeviceId(deviceId) {

        var cacheKey = 'oldapp3-' + deviceId;
        var cacheValue = appStorage.getItem(cacheKey);
        if (cacheValue) {

            return new Promise(function (resolve, reject) {

                resolve(cacheValue == 'true');
            });

        } else {

            Logger.log('testing play access for device id: ' + deviceId);

            return fetch('http://mb3admin.com/admin/service/statistics/appAccess?application=AndroidV1&deviceId=' + deviceId, {
                method: 'GET'

            }).then(function (response) {

                Logger.log('Play access test for device id: ' + deviceId + '. Response: ' + response.status);

                if (response.status < 400) {
                    appStorage.setItem(cacheKey, 'true');
                    return true;
                }

                return false;

            }, function (e) {

                Logger.log('Play access test for device id: ' + deviceId + ' failed.');

                return false;
            });
        }
    }

    function enableRestore(subscriptionOptions, unlockableProductInfo) {
        return unlockableProductInfo != null && unlockableProductInfo.feature == 'playback';
    }

    function restorePurchase() {

        Dashboard.alert({
            message: "We're unable to automatically restore your previous purchase. Please send an email to apps@emby.media. Thank you for your patience.",
            title: Globalize.translate('ButtonRestorePreviousPurchase')
        });
    }

    window.IapManager = {
        isPurchaseAvailable: isPurchaseAvailable,
        getProductInfo: getProduct,
        updateProduct: updateProductInfo,
        beginPurchase: beginPurchase,
        onPurchaseComplete: onPurchaseComplete,
        getSubscriptionOptions: getSubscriptionOptions,
        onStoreReady: onStoreReady,
        isUnlockedOverride: isUnlockedOverride,
        restorePurchase: restorePurchase,
        enableRestore: enableRestore
    };

    NativeIapManager.initStore();

})();