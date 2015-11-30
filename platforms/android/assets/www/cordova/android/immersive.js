(function(){function onSuccess(){Logger.log('Immersive mode succeeded');}
function onError(){Logger.log('Immersive mode failed');}
function updateFromSetting(leaveFullScreen){if(AppSettings.enableFullScreen()){AndroidFullScreen.immersiveMode(onSuccess,onError);}
else if(leaveFullScreen){AndroidFullScreen.showSystemUI(onSuccess,onError);}}
Dashboard.ready(function(){Logger.log('binding fullscreen to MediaController');updateFromSetting(false);$(AppSettings).on('settingupdated',function(e,key){if(key=='enableFullScreen'){updateFromSetting(true);}});});})();