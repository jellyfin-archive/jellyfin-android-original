define([], function () {
    'use strict';

    var myStore = {};

    myStore.setItem = function (name, value) {
        localStorage.setItem(name, value);
        AndroidSharedPreferences.set(name, value);
    };

    myStore.getItem = function (name) {
        //return localStorage.getItem(name);
        return AndroidSharedPreferences.get(name);
    };

    myStore.removeItem = function (name) {
        localStorage.removeItem(name);
        AndroidSharedPreferences.remove(name);
    };

    return myStore;
});