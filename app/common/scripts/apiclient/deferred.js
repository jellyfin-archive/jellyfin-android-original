(function (globalScope, angular) {

    globalScope.DeferredBuilder = {

        Deferred: function () {
            return new DeferContainer($q.defer());
        },

        when: function (promises) {
            var all = $q.all(promises);
            extendPromise(all);
            return all;
        }

    };

    var $q = angular.injector(['ng']).get('$q');

    // Adds jQuery style .done() and .fail() functionality to an Angular Promise object
    function extendPromise(promise) {
      promise.done = function() {
        for (var i = 0; i < arguments.length; i++) {
          promise.then(arguments[i]);
        }
        return promise;
      };

      promise.fail = function() {
        for (var i = 0; i < arguments.length; i++) {
          promise.then(null, arguments[i]);
        }
        return promise;
      };
    }

    // Container for an Angular Deferred object that exposes jQuery style methods
    function DeferContainer(ngDefer) {
      extendPromise(ngDefer.promise);
      this._ngDefer = ngDefer;
    }

    DeferContainer.prototype.promise = function() {
      return this._ngDefer.promise;
    };

    DeferContainer.prototype.resolve = function() {
      return this._ngDefer.resolve.apply(this._ngDefer, arguments);
    };

    DeferContainer.prototype.resolveWith = function(context, array) {
      return this._ngDefer.resolve.apply(this._ngDefer, array);
    };

    DeferContainer.prototype.reject = function() {
      return this._ngDefer.reject.apply(this._ngDefer, arguments);
    };

    DeferContainer.prototype.rejectWith = function(context, array) {
      return this._ngDefer.reject.apply(this._ngDefer, array);
    };


})(window, angular);
