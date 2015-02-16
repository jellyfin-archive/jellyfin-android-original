(function (globalScope) {

    globalScope.AjaxApi = {

        param: function(params) {
            return jQuery.param(params);
        },

        ajax: function(request) {

			var deferred = jQuery.Deferred();
			
			var req = {
			 method: request.type,
			 url: request.url,
			 headers: request.headers,
			 dataType: request.dataType,
			 timeout: request.timeout
			};
			
			var $http = globalScope.$http;
			
			$http(req).success(function(data, status, headers, config) {
				
				deferred.resolveWith(null, [data]);
				
			}).error(function(data, status, headers, config) {
				
				deferred.rejectWith(null, []);
			});
			
			return deferred.promise();
        }

    };

})(this);