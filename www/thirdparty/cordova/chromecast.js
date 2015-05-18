(function(){
 
 
 function onDeviceFound(){
 
 }
 
 function onDeviceLost(){
 
 }
 
 function initSdk() {
 
 discoveryManager.setPairingLevel(ConnectSDK.PairingLevel.OFF);
 discoveryManager.setAirPlayServiceMode(ConnectSDK.AirPlayServiceMode.Media);
 
 discoveryManager.addListener('devicefound', onDeviceFound);
 
 discoveryManager.addListener('devicelost', onDeviceLost);
 }
 
 
 
 document.addEventListener("deviceready", function () {
                           
                           initSdk();
                           
                           }, false);
 
 
 })();