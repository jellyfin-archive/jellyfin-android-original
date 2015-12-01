function getWindowLocationSearch(win){var search=(win||window).location.search;if(!search){var index=window.location.href.indexOf('?');if(index!=-1){search=window.location.href.substring(index);}}
return search||'';}
function getParameterByName(name,url){name=name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");var regexS="[\\?&]"+name+"=([^&#]*)";var regex=new RegExp(regexS,"i");var results=regex.exec(url||getWindowLocationSearch());if(results==null)
return"";else
return decodeURIComponent(results[1].replace(/\+/g," "));}
function replaceQueryString(url,param,value){var re=new RegExp("([?|&])"+param+"=.*?(&|$)","i");if(url.match(re))
return url.replace(re,'$1'+param+"="+value+'$2');else if(value){if(url.indexOf('?')==-1){return url+'?'+param+"="+value;}
return url+'&'+param+"="+value;}
return url;}
function parseISO8601Date(s,options){options=options||{};var re=/(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(\.\d+)?(Z|([+-])(\d{2}):(\d{2}))?/;var d=s.match(re);if(!d){throw"Couldn't parse ISO 8601 date string '"+s+"'";}
var a=[1,2,3,4,5,6,10,11];for(var i in a){d[a[i]]=parseInt(d[a[i]],10);}
d[7]=parseFloat(d[7]);var ms=Date.UTC(d[1],d[2]-1,d[3],d[4],d[5],d[6]);if(d[7]>0){ms+=Math.round(d[7]*1000);}
if(d[8]!="Z"&&d[10]){var offset=d[10]*60*60*1000;if(d[11]){offset+=d[11]*60*1000;}
if(d[9]=="-"){ms-=offset;}else{ms+=offset;}}else if(!options.toLocal){ms+=new Date().getTimezoneOffset()*60000;}
return new Date(ms);}