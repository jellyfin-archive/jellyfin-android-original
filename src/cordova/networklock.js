define([], function () {
    'use strict';

    function ResourceLockInstance() {
    }

    ResourceLockInstance.prototype.acquire = function () {
        MainActivity.acquireNetworkLock();
    };

    ResourceLockInstance.prototype.isHeld = function () {
        return MainActivity.isNetworkLockHeld();
    };

    ResourceLockInstance.prototype.release = function () {
        MainActivity.releaseNetworkLock();
    };

    return ResourceLockInstance;
});