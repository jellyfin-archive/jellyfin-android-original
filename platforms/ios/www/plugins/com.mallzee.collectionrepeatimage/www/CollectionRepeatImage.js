cordova.define("com.mallzee.collectionrepeatimage.CollectionRepeatImage", function(require, exports, module) { var exec = require('cordova/exec');

var CollectionRepeatImage = function () {

};

CollectionRepeatImage.getImage = function(options, success, error) {
    exec(success, error, "CollectionRepeatImage", "getImage", [options]);
};

CollectionRepeatImage.cancel = function(index, success, error) {
	exec(success, error, "CollectionRepeatImage", "cancel", [index]);
};

CollectionRepeatImage.cancelAll = function(args, success, error) {
	exec(null, null, "CollectionRepeatImage", "cancelAll", []);
};

module.exports = CollectionRepeatImage;
});
