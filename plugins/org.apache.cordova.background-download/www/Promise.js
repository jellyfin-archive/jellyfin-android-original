/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
(function (global) {
    /**
     * Provides a mechanism for managing interactions with asynchronous APIs based on Promises.
     * See 'Asynchronous programming in JavaScript' for more details
     * http://msdn.microsoft.com/en-us/library/windows/apps/hh700330.aspx
     * http://msdn.microsoft.com/en-us/library/windows/apps/br211867.aspx
     * Note. Functionality is limited, support of .then and .cancel methods only, other methods are in progress.
     */
    function Promise() {
        this._completeCallbacks = [];
        this._errorCallbacks = [];
        this._progressCallbacks = [];
        this._chainedDefer = null;
        this._state = 'pending'; // -> 'resolved' or 'rejected' or 'cancelled'
        this.result = null; // represents error or result depending on _state
    };
    
    function Deferral() {
        this.promise = new Promise();
    };

    Promise.Deferral = Deferral;

    Promise.prototype.then = function (onComplete, onError, onProgress) {

        if (onComplete) { this._completeCallbacks.push(onComplete); }
        if (onError) { this._errorCallbacks.push(onError); }
        if (onProgress) { this._progressCallbacks.push(onProgress); }

        // TODO: add support of several chained defers - pass defer object along with callback to _successCallbacks/_errorCallbacks etc..
        // Example: var a = new Promise(); var b = a.then(..); var c = a.then(..).
        // both b and c promises should work

        // deferred object for chained promise
        this._chainedDefer = new Promise.Deferral();
        this._chainedDefer.promise.parent = this;

        // if operation already completed/failed
        if (this._state === 'resolved') {
            this.executeCallback(onComplete, this.result);
        } else if (this._state === 'rejected') {
            this.executeCallback(onError, this.result);
        }

        return this._chainedDefer.promise;
    };

    // Attempts to cancel the fulfillment of a promised value. If the promise hasn't already been fulfilled and
    // cancellation is supported, the promise enters the error state with a value of Error("Canceled").
    // http://msdn.microsoft.com/en-us/library/windows/apps/br211667.aspx
    Promise.prototype.cancel = function (flag) {
        // start cancellation from the root promise and then go down the way
        // TODO find official/correct implementation way
        if (this.parent != null && !flag) {
            var root = this;
            while (root.parent) {
                root = root.parent;
            }
            window.setTimeout(function () {
                root.cancel(true);
            });
            return;
        }

        // we can cancel only pending promise
        if (this._state !== 'pending') {
            return;
        }

        this._state = 'cancelled';
        
        var me = this,
            error = new Error('Cancelled');

        this.result = error;
        
        // trigger cancellation error
        this._errorCallbacks.forEach(function (callback) {
            me.executeCallback(callback, error);
        });

        // special way to implement any custom logic when user requested async operation cancel
        if (this.onCancelled) {
            this.onCancelled();
        }

        if (this._chainedDefer) {
            this._chainedDefer.promise.cancel(true);
        }

    };

    Promise.prototype.executeCallback = function (callback, args) {
        var chainedDefer = this._chainedDefer;
        state = this._state;
        
        window.setTimeout(function () {

            var fnResult;
            try {
                fnResult = callback(args);
            } catch (ex) {
                // pass error to chained promise
                chainedDefer && chainedDefer.reject(ex);
                return;
            }

            // if there is chained promise we should resolve it after callback function is completed
            // for promise cancellation there is different  propagation mechanism
            if (chainedDefer == null || state === 'cancelled') return;

            if (fnResult instanceof Promise) {
                chainedDefer.bind(fnResult);
            } else {
                // TODO promise is rejected when it's resolved with an instance of error; add check for fnResult instance of Error
                chainedDefer.resolve(fnResult);
            }
        });
    };


    Deferral.prototype.resolve = function (data) {

        if (this.promise._state !== 'pending') {
            return;
        }

        var promise = this.promise;
        promise._state = 'resolved';
        promise.result = data;

        promise._completeCallbacks.forEach(function (callback) {
            promise.executeCallback(callback, data);
        });
    };

    Deferral.prototype.reject = function (error) {

        if (this.promise._state !== 'pending') {
            return;
        }

        var promise = this.promise;
        promise._state = 'rejected';
        promise.result = error;

        promise._errorCallbacks.forEach(function (callback) {
            promise.executeCallback(callback, error);
        });
    };

    Deferral.prototype.notify = function (value) {
        
        if (this.promise._state === 'cancelled') {
            return;
        }
        
        this.promise._progressCallbacks.forEach(function (callback) {
            window.setTimeout(function () {
                callback(value);
            });
        });
    };

    Deferral.prototype.bind = function (promise) {
        var that = this;
        promise.then(function (res) {
            that.resolve(res);
        }, function (err) {
            that.reject(err);
        });
    };
    
    if (typeof exports == 'object') {
        module.exports = Promise;
    } else if (typeof define === 'function' && define.amd) {
        /* AMD support */
        define(function () {
            return Promise;
        });
    } else {
        global.Promise = Promise;
    }

})(this);
