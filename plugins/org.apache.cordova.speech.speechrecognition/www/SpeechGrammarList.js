var SpeechGrammarList = function(data) {
  this._list = data;
  this.length = this._list.length;
};
    
SpeechGrammarList.prototype.item = function(item) {
    return this._list[item];
};

SpeechGrammarList.prototype.addFromUri = function(item) {
};

SpeechGrammarList.prototype.addFromString = function(item) {
};

module.exports = SpeechGrammarList;
