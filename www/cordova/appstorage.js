define([], function () {
    'use strict';

    var myStore = {};

    myStore.setItem = function (name, value) {
        localStorage.setItem(name, value);
        AndroidSharedPreferences.set(name, value);
    };

    var isCrosswalk = navigator.userAgent.toLowerCase().indexOf('crosswalk') !== -1;

    if (isCrosswalk) {
        myStore.getItem = function (name) {
            return AndroidSharedPreferences.get(name);
        };
    } else {
        myStore.getItem = function (name) {
            return localStorage.getItem(name);
        };
    }

    myStore.removeItem = function (name) {
        localStorage.removeItem(name);
        AndroidSharedPreferences.remove(name);
    };

    return myStore;
});