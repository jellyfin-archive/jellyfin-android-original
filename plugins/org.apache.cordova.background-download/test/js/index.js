/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var app = {

    fileName: "PointerEventsCordovaPlugin.wmv",
    uriString: "http://media.ch9.ms/ch9/8c03/f4fe2512-59e5-4a07-bded-124b06ac8c03/PointerEventsCordovaPlugin.wmv",  // 38.3 MB
    // Application Constructor
    initialize: function() {
        this.bindEvents();
    },

    downloadFile: function(uriString, targetFile) {

        var lblProgress = document.getElementById('lblProgress');

        var complete = function() {
            lblProgress.innerHTML = 'Done';
        };
        var error = function (err) {
            console.log('Error: ' + err);
            lblProgress.innerHTML = 'Error: ' + err;
        };
        var progress = function(progress) {
            lblProgress.innerHTML = (100 * progress.bytesReceived / progress.totalBytesToReceive) + '%';
        };

        try {

            var downloader = new BackgroundTransfer.BackgroundDownloader();
            // Create a new download operation.
            var download = downloader.createDownload(uriString, targetFile);
            // Start the download and persist the promise to be able to cancel the download.
            app.downloadPromise = download.startAsync().then(complete, error, progress);

        } catch(err) {
            console.log('Error: ' + err);
        }
    },

    // Bind Event Listeners
    //
    // Bind any events that are required on startup. Common events are:
    // 'load', 'deviceready', 'offline', and 'online'.
    bindEvents: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);

        document.getElementById('btnStart').addEventListener('click', this.startDownload);
        document.getElementById('btnStop').addEventListener('click', this.stopDownload);
        document.getElementById('btnFileInfo').addEventListener('click', this.getFileInfo);
    },
    // deviceready Event Handler
    //
    // The scope of 'this' is the event. In order to call the 'receivedEvent'
    // function, we must explicity call 'app.receivedEvent(...);'
    onDeviceReady: function() {
        app.receivedEvent('deviceready');

        app.startDownload();
    },

    startDownload: function () {
        
        window.requestFileSystem(LocalFileSystem.PERSISTENT, 0, function(fileSystem) {
            fileSystem.root.getFile(app.fileName, { create: true }, function (newFile) {
                app.downloadFile(app.uriString, newFile);
            });
        });
    },
    
    stopDownload: function () {
        app.downloadPromise && app.downloadPromise.cancel();
    },
    
    getFileInfo: function () {
        window.requestFileSystem(LocalFileSystem.PERSISTENT, 0, function (fileSystem) {
            fileSystem.root.getFile(app.fileName, { create: true }, function (fileEntry) {
                fileEntry.file(function (meta) {
                    document.getElementById('lblFileInfo').innerHTML =
                        "Modified: " + meta.lastModifiedDate + "<br/>" +
                        "size: " + meta.size;
                });
            }, function(error) {
                document.getElementById('lblFileInfo').innerHTML = "error: " + error;
            });
        });
    },

        // Update DOM on a Received Event
    receivedEvent: function(id) {
        var parentElement = document.getElementById(id);
        var listeningElement = parentElement.querySelector('.listening');
        var receivedElement = parentElement.querySelector('.received');

        listeningElement.setAttribute('style', 'display:none;');
        receivedElement.setAttribute('style', 'display:block;');

        console.log('Received Event: ' + id);
    }
};