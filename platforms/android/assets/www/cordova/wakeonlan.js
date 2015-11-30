(function(globalScope){function getResultCode(result){if(result!=null&&result.resultCode!=null){return result.resultCode;}
return result;}
function closeSocket(socketId){try{chrome.sockets.udp.close(socketId);}catch(err){}}
function stringToArrayBuffer(string){var buf=new ArrayBuffer(string.length*2);var bufView=new Uint16Array(buf);for(var i=0,strLen=string.length;i<strLen;i++){bufView[i]=string.charCodeAt(i);}
return buf;}
globalScope.WakeOnLan={send:function(info){var deferred=DeferredBuilder.Deferred();var chrome=globalScope.chrome;if(!chrome){deferred.resolve();return deferred.promise();}
var port=info.Port||9;deferred.resolve();return deferred.promise();}};})(window);