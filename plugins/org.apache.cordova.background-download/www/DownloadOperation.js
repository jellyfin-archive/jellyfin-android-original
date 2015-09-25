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

var exec = require('cordova/exec'),
    Promise = require('./Promise');

/**
 * Performs an asynchronous download operation in the background.
 *
 * @param {string} uri The location of the resource.
 * @param {File} resultFile The file that the response will be written to.
 */
var DownloadOperation = function (uri, resultFile) {

    if (uri == null || resultFile == null) {
        throw new Error("missing or invalid argument");
    }
    
    this.uri = uri;
    this.resultFile = resultFile;
};

/**
 * Starts an asynchronous download operation.
 */
DownloadOperation.prototype.startAsync = function() {

    var deferral = new Promise.Deferral(),
        me = this,
        successCallback = function(result) {

            // success callback is used to both report operation progress and 
            // as operation completeness handler
            
            if (result && typeof result.progress != 'undefined') {
                deferral.notify(result.progress);
            } else {
                deferral.resolve(result);
            }
        },
        errorCallback = function(err) {
            deferral.reject(err);
        };

    exec(successCallback, errorCallback, "BackgroundDownload", "startAsync", [this.uri, this.resultFile.toURL()]);

    // custom mechanism to trigger stop when user cancels pending operation
    deferral.promise.onCancelled = function () {
        me.stop();
    };

    return deferral.promise;
};

/**
 * Stops a download operation.
 */
DownloadOperation.prototype.stop = function() {
    // TODO return promise
    exec(null, null, "BackgroundDownload", "stop", [this.uri]);

};

module.exports = DownloadOperation;