(function(window, undefined)
{
	'use strict';

	var AndroidFullScreen =
	{
		isSupported: function(successFunction, errorFunction)
		{
			cordova.exec(successFunction, errorFunction, 'AndroidFullScreen', 'isSupported', []);
		},

		isImmersiveModeSupported: function(successFunction, errorFunction)
		{
			cordova.exec(successFunction, errorFunction, 'AndroidFullScreen', 'isImmersiveModeSupported', []);
		},

		immersiveWidth: function(successFunction, errorFunction)
		{
			cordova.exec(successFunction, errorFunction, 'AndroidFullScreen', 'immersiveWidth', []);
		},

		immersiveHeight: function(successFunction, errorFunction)
		{
			cordova.exec(successFunction, errorFunction, 'AndroidFullScreen', 'immersiveHeight', []);
		},

		leanMode: function(successFunction, errorFunction)
		{
			cordova.exec(successFunction, errorFunction, 'AndroidFullScreen', 'leanMode', []);
		},

		showSystemUI: function(successFunction, errorFunction)
		{
			cordova.exec(successFunction, errorFunction, 'AndroidFullScreen', 'showSystemUI', []);
		},

		showUnderSystemUI: function(successFunction, errorFunction)
		{
			cordova.exec(successFunction, errorFunction, 'AndroidFullScreen', 'showUnderSystemUI', []);
		},
		
		immersiveMode: function(successFunction, errorFunction)
		{
			cordova.exec(successFunction, errorFunction, 'AndroidFullScreen', 'immersiveMode', []);
		}
	};
	
	cordova.addConstructor(function() 
	{
		window.AndroidFullScreen = AndroidFullScreen;
		return window.AndroidFullScreen;
	});
	
})(window);
