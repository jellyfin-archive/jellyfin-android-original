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

var DownloadOperation = require('./DownloadOperation');

/**
 * Initializes a new instance of BackgroundDownloader object.
 * Used to configure downloads prior to the actual creation of the download operation using CreateDownload.
 */
var BackgroundDownloader = function() {

};

/**
 * Initializes a DownloadOperation object that contains the specified Uri and the file that the response is written to.
 *
 * @param {string} uri The location of the resource.
 * @param {File} resultFile The file that the response will be written to.
 */
BackgroundDownloader.prototype.createDownload = function(uri, resultFile) {
    return new DownloadOperation(uri, resultFile);
};

module.exports = BackgroundDownloader;