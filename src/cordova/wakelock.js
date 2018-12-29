define([], function () {
    'use strict';

    function ResourceLockInstance() {
    }

    ResourceLockInstance.prototype.acquire = function () {
        MainActivity.acquireWakeLock();
    };

    ResourceLockInstance.prototype.isHeld = function () {
        return MainActivity.isWakeLockHeld();
    };

    ResourceLockInstance.prototype.release = function () {
        MainActivity.releaseWakeLock();
    };

    return ResourceLockInstance;
});