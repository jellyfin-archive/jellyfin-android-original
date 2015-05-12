var exec = require('cordova/exec');

module.exports = {
    
    get: function(success, fail) {
        cordova.exec(success, fail, 'UniqueDeviceID', 'get', []);
    }

};