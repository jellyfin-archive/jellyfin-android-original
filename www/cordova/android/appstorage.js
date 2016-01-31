define([], function () {

    function preferencesStore() {

        var self = this;

        self.setItem = function (name, value) {

            AndroidSharedPreferences.set(name, value);
        };

        self.getItem = function (name) {

            return AndroidSharedPreferences.get(name);
        };

        self.removeItem = function (name) {

            AndroidSharedPreferences.remove(name);
        };
    }

    return new preferencesStore();
});