define(['appStorage'], function (appStorage) {

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

        if (feature == 'playback' || feature == 'livetv') {
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

            console.log('testing play access for device id: ' + deviceId);

            return fetch('http://mb3admin.com/admin/service/statistics/appAccess?application=AndroidV1&deviceId=' + deviceId, {
                method: 'GET'

            }).then(function (response) {

                console.log('Play access test for device id: ' + deviceId + '. Response: ' + response.status);

                if (response.status < 400) {
                    appStorage.setItem(cacheKey, 'true');
                    return true;
                }

                return false;

            }, function (e) {

                console.log('Play access test for device id: ' + deviceId + ' failed.');

                return false;
            });
        }
    }

    function enableRestore(feature, subscriptionOptions, unlockableProductInfo) {
        return feature == 'playback' || feature == 'livetv';
    }

    function restorePurchase() {

        var msg = Globalize.translate('AlreadyPaidHelp1', 'apps@emby.media');

        msg += '<br/><br/>' + Globalize.translate('AlreadyPaidHelp2');

        require(['confirm'], function (confirm) {

            confirm(msg, Globalize.translate('AlreadyPaid')).then(launchEmail);

        });
    }

    function launchEmail() {

        var serverInfo = ApiClient.serverInfo() || {};
        var serverId = serverInfo.Id || 'Unknown';

        var body = 'Please assist in restoring my previous purchase. ' + serverId + '|' + ConnectionManager.deviceId();

        MainActivity.sendEmail('apps@emby.media', 'Android Activation', body);
    }

    window.IapManager = {
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

});