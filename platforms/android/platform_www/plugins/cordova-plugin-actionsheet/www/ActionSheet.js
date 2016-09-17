cordova.define("cordova-plugin-actionsheet.ActionSheet", function(require, exports, module) {
function ActionSheet() {
}

ActionSheet.prototype.show = function (options, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "ActionSheet", "show", [options]);
};

ActionSheet.prototype.hide = function (options, successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "ActionSheet", "hide", [options]);
};

ActionSheet.prototype.ANDROID_THEMES = {
  THEME_TRADITIONAL          : 1, // default
  THEME_HOLO_DARK            : 2,
  THEME_HOLO_LIGHT           : 3,
  THEME_DEVICE_DEFAULT_DARK  : 4,
  THEME_DEVICE_DEFAULT_LIGHT : 5
};

ActionSheet.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }

  window.plugins.actionsheet = new ActionSheet();

  return window.plugins.actionsheet;
};

cordova.addConstructor(ActionSheet.install);
});
