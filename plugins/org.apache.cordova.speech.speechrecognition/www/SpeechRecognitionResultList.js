// A collection of responses (used in continuous mode)
var SpeechRecognitionResultList = function() {
    this._result = [];
    this.length = 0;
};

SpeechRecognitionResultList.prototype.item = function(item) {
    return this._result[item];
};

module.exports = SpeechRecognitionResultList;
