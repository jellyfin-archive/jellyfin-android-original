define([], function () {
    'use strict';

    var myStore = {};

    myStore.setItem = function (name, value) {
        localStorage.setItem(name, value);
    };

    myStore.getItem = function (name) {
        return localStorage.getItem(name);
    };

    myStore.removeItem = function (name) {
        localStorage.removeItem(name);
    };

    return myStore;
});