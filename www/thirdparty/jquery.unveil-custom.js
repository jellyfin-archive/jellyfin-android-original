/**
 * jQuery Unveil
 * A very lightweight jQuery plugin to lazy load images
 * http://luis-almeida.github.com/unveil
 *
 * Licensed under the MIT license.
 * Copyright 2013 Luís Almeida
 * https://github.com/luis-almeida
 */

(function ($) {

    var unveilId = 0;


    function getThreshold() {

        if (window.AppInfo && AppInfo.hasLowImageBandwidth) {
            return 0;
        }

        // Test search before setting to 0
        return 100;
    }

    $.fn.unveil = function () {

        var $w = $(window),
            th = getThreshold(),
            attrib = "data-src",
            images = this,
            loaded;

        unveilId++;
        var eventNamespace = 'unveil' + unveilId;

        this.one("unveil", function () {
            var elem = this;
            var source = elem.getAttribute(attrib);
            if (source) {
                ImageStore.setImageInto(elem, source);
                elem.setAttribute("data-src", '');
            }
        });

        function unveil() {
            var inview = images.filter(function () {
                var $e = $(this);

                if ($e.is(":hidden")) return;
                var wt = $w.scrollTop(),
                    wb = wt + $w.height(),
                    et = $e.offset().top,
                    eb = et + $e.height();

                return eb >= wt - th && et <= wb + th;
            });

            loaded = inview.trigger("unveil");
            images = images.not(loaded);

            if (!images.length) {
                $w.off('scroll.' + eventNamespace);
                $w.off('resize.' + eventNamespace);
            }
        }

        $w.on('scroll.' + eventNamespace, unveil);
        $w.on('resize.' + eventNamespace, unveil);

        unveil();

        return this;

    };

    $.fn.lazyChildren = function () {

        var lazyItems = $(".lazy", this);

        if (lazyItems.length) {
            lazyItems.unveil();
        }

        return this;
    };

    $.fn.lazyImage = function (url) {

        return this.attr('data-src', url).unveil();
    };

})(window.jQuery || window.Zepto);

(function () {

    // IndexedDB
    var indexedDB = window.indexedDB || window.webkitIndexedDB || window.mozIndexedDB || window.OIndexedDB || window.msIndexedDB,
        dbVersion = 1.0;

    function setImageIntoElement(elem, url) {

        if (elem.tagName === "DIV") {

            elem.style.backgroundImage = "url('" + url + "')";

        } else {
            elem.setAttribute("src", url);
        }
    }

    function onDbOpened(imageStore, db) {

        imageStore._db = db;
        window.ImageStore = imageStore;
    }

    function onOpenError() {
        console.log("Error creating/accessing IndexedDB database");
    }

    function openDb(imageStore) {

        // Create/open database
        var db = window.sqlitePlugin.openDatabase({ name: "my.db" });

        db.transaction(function (tx) {

            tx.executeSql('CREATE TABLE IF NOT EXISTS images (id text primary key, data text)');
            tx.executeSql('create index if not exists idx_images on images(id)');

            onDbOpened(imageStore, db);
        });
    }

    function indexedDbImageStore() {

        var self = this;

        self.addImageToDatabase = function (blob, key) {

            var deferred = DeferredBuilder.Deferred();

            console.log("addImageToDatabase");

            self.db().transaction(function (tx) {

                tx.executeSql("INSERT INTO images (id, data) VALUES (?,?)", [key, blob], function (tx, res) {
                    
                    deferred.resolve();
                }, function (e) {
                    deferred.reject();
                });
            });

            return deferred.promise();
        };

        self.db = function () {

            return self._db;
        };

        self.get = function (key) {

            var deferred = DeferredBuilder.Deferred();

            self.db().transaction(function (tx) {

                tx.executeSql("SELECT data from images where id=?", [key], function (tx, res) {

                    if (res.rows.length) {

                        deferred.resolveWith(null, [res.rows.item(0).data]);
                    } else {
                        deferred.reject();
                    }
                }, function (e) {
                    deferred.reject();
                });
            });

            return deferred.promise();
        };

        self.getImageUrl = function (originalUrl) {

            console.log('getImageUrl:' + originalUrl);

            var key = CryptoJS.SHA1(originalUrl).toString();

            var deferred = DeferredBuilder.Deferred();

            self.get(key).done(function (url) {

                deferred.resolveWith(null, [url]);

            }).fail(function () {

                self.downloadImage(originalUrl, key).done(function () {
                    self.get(key).done(function (url) {

                        deferred.resolveWith(null, [url]);

                    }).fail(function () {

                        deferred.reject();
                    });
                }).fail(function () {

                    deferred.reject();
                });
            });

            return deferred.promise();
        };

        self.downloadImage = function (url, key) {

            var deferred = DeferredBuilder.Deferred();

            console.log('downloadImage:' + url);

            // Create XHR
            var xhr = new XMLHttpRequest();

            xhr.open("GET", url, true);
            // Set the responseType to blob
            xhr.responseType = "arraybuffer";

            xhr.addEventListener("load", function () {

                if (xhr.status === 200) {
                    console.log("Image retrieved");

                    try {

                        var arr = new Uint8Array(this.response);

                        // Convert the int array to a binary string
                        // We have to use apply() as we are converting an *array*
                        // and String.fromCharCode() takes one or more single values, not
                        // an array.
                        var raw = String.fromCharCode.apply(null, arr);

                        // This works!!!
                        var b64 = btoa(raw);
                        var dataURL = "data:image/jpeg;base64," + b64;

                        // Put the received blob into IndexedDB
                        self.addImageToDatabase(dataURL, key).done(function () {
                            deferred.resolve();
                        }).fail(function () {
                            deferred.reject();
                        });
                    } catch (err) {
                        console.log("Error adding image to database");
                        deferred.reject();
                    }
                } else {
                    deferred.reject();
                }
            }, false);

            // Send XHR
            xhr.send();
            return deferred.promise();
        };

        self.setImageInto = function (elem, url) {

            function onFail() {
                setImageIntoElement(elem, url);
            }

            self.getImageUrl(url).done(function (localUrl) {
                setImageIntoElement(elem, localUrl);

            }).fail(onFail);
        };

        openDb(self);
    }

    window.IndexedDbImageStore = indexedDbImageStore;

})();

(function () {

    function setImageIntoElement(elem, url) {

        if (elem.tagName === "DIV") {

            elem.style.backgroundImage = "url('" + url + "')";

        } else {
            elem.setAttribute("src", url);
        }
    }

    function simpleImageStore() {

        var self = this;

        self.setImageInto = setImageIntoElement;
    }

    console.log('creating simpleImageStore');
    window.ImageStore = new simpleImageStore();

    document.addEventListener("deviceready", function () {

        new IndexedDbImageStore();

    }, false);

})();