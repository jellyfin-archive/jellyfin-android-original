(function(){function setImageIntoElement(elem,url){if(elem.tagName!=="IMG"){elem.style.backgroundImage="url('"+url+"')";}else{elem.setAttribute("src",url);}}
var fileSystem;function getFileSystem(){var deferred=DeferredBuilder.Deferred();if(fileSystem){deferred.resolveWith(null,[fileSystem]);}else{requestFileSystem(PERSISTENT,0,function(fs){fileSystem=fs;deferred.resolveWith(null,[fileSystem]);});}
return deferred.promise();}
function indexedDbBlobImageStore(){var self=this;function getCacheKey(url){var index=url.indexOf('://');if(index!=-1){url=url.substring(index+3);index=url.indexOf('/');if(index!=-1){url=url.substring(index+1);}}
return CryptoJS.MD5(url).toString();}
function normalizeReturnUrl(url){if(browserInfo.safari){var index=url.indexOf('/Documents');if(index!=-1){return url.substring(index);}
else{return url.replace('file://','');}}
return url;}
self.getImageUrl=function(originalUrl){if(browserInfo.android&&originalUrl.indexOf('tag=')!=-1){originalUrl+="&accept=webp";}
var deferred=DeferredBuilder.Deferred();var key=getCacheKey(originalUrl);getFileSystem().then(function(fileSystem){var path=fileSystem.root.toURL()+"/emby/cache/"+key;resolveLocalFileSystemURL(path,function(fileEntry){var localUrl=normalizeReturnUrl(fileEntry.toURL());deferred.resolveWith(null,[localUrl]);},function(){var ft=new FileTransfer();ft.download(originalUrl,path,function(entry){var localUrl=normalizeReturnUrl(entry.toURL());deferred.resolveWith(null,[localUrl]);});});});return deferred.promise();};self.setImageInto=function(elem,url){function onFail(){setImageIntoElement(elem,url);}
self.getImageUrl(url).then(function(localUrl){setImageIntoElement(elem,localUrl);},onFail);};var imageIdIndex=1;function setImageWithSdWebImage(elem,url){var rect=elem.getBoundingClientRect();var options={data:url,index:imageIdIndex,quality:0,scale:Math.round(rect.width)+'x'+Math.round(rect.height),downloadOptions:window.CollectionRepeatImageOptions.SDWebImageRetryFailed|window.CollectionRepeatImageOptions.SDWebImageLowPriority|window.CollectionRepeatImageOptions.SDWebImageAllowInvalidSSLCertificates};if(elem.classList.contains('coveredCardImage')){options.scale+='!';}
imageIdIndex++;window.CollectionRepeatImage.getImage(options,function(data){var dataUrl='data:image/jpeg;base64,'+data;elem.style.backgroundImage="url('"+dataUrl+"')";});}
window.ImageStore=self;}
require(['cryptojs-sha1'],function(){new indexedDbBlobImageStore();});})();