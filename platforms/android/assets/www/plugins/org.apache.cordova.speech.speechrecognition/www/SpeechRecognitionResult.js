cordova.define("org.apache.cordova.speech.speechrecognition.SpeechRecognitionResult", function(require, exports, module) { // A complete one-shot simple response
var SpeechRecognitionResult = function() {
    this._result = [];
    this.length = 0;
    this.final = false;
};

SpeechRecognitionResult.prototype.item = function(item) {
    return this._result[item];
};

module.exports = SpeechRecognitionResult;

});
