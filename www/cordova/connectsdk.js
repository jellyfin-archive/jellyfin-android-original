(function(){function initSdk(){var manager=ConnectSDK.discoveryManager;manager.setAirPlayServiceMode(ConnectSDK.AirPlayServiceMode.Media);manager.on('devicelistchanged',onDeviceListChanged);manager.startDiscovery();requirejs(['cordova/chromecast','cordova/generaldevice']);}
function onDeviceListChanged(list){}
function getDeviceList(){return ConnectSDK.discoveryManager.getDeviceList();}
window.ConnectSDKHelper={getDeviceList:getDeviceList};Dashboard.ready(initSdk);})();