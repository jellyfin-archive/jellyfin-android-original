define(["iron-list","lazyload-image"],function(){function e(e){var n=200;return new Promise(function(t){var r=new XMLHttpRequest;r.open("GET","components/ironcardlist/ironcardlist.template.html",!0),r.onload=function(){var r=this.response;r=r.replace("${maxphysical}",n),r=r.replace("${scrolltarget}",e),t(r)},r.send()})}return{getTemplate:e}});