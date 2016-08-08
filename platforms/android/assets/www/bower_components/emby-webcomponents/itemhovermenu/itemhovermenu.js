define(["connectionManager","itemHelper","mediaInfo","userdataButtons","playbackManager","globalize","dom","apphost","css!./itemhovermenu","emby-button"],function(e,t,a,i,r,n,s,o){function d(e){var t=e.target;y&&(clearTimeout(y),y=null),t=t.classList.contains("cardOverlayTarget")?t:t.querySelector(".cardOverlayTarget"),t&&c(t)}function c(e){return e.classList.contains("hide")?void 0:e.animate?void requestAnimationFrame(function(){var t=[{transform:"none",offset:0},{transform:"translateY(100%)",offset:1}],a={duration:140,iterations:1,fill:"forwards",easing:"ease-out"};e.animate(t,a).onfinish=function(){e.classList.add("hide")}}):void e.classList.add("hide")}function l(e){e.classList.contains("hide")&&(e.classList.remove("hide"),e.animate&&requestAnimationFrame(function(){var t=[{transform:"translateY(100%)",offset:0},{transform:"none",offset:1}],a={duration:180,iterations:1,fill:"forwards",easing:"ease-out"};e.animate(t,a)}))}function m(e,s,d,c){var l="";l+='<div class="cardOverlayInner">';var m=c.className.toLowerCase(),u=-1!=m.indexOf("mini"),v=u||-1!=m.indexOf("small"),f=-1!=m.indexOf("portrait"),g=v||u||f?null:s.SeriesName,y=t.getDisplayName(s);l+="<div>";var p,b=26;g&&s.ParentLogoItemId?(p=e.getScaledImageUrl(s.ParentLogoItemId,{maxHeight:b,type:"logo",tag:s.ParentLogoImageTag}),l+='<img src="'+p+'" style="max-height:'+b+'px;max-width:100%;" />'):s.ImageTags.Logo?(p=e.getScaledImageUrl(s.Id,{maxHeight:b,type:"logo",tag:s.ImageTags.Logo}),l+='<img src="'+p+'" style="max-height:'+b+'px;max-width:100%;" />'):l+=g||y,l+="</div>",g?(l+="<p>",l+=y,l+="</p>"):v||u||(l+='<div class="cardOverlayMediaInfo">',l+=a.getPrimaryMediaInfoHtml(s,{endsAt:!1}),l+="</div>"),l+='<div class="cardOverlayButtons">';var h=0;r.canPlay(s)&&(l+='<button is="emby-button" class="itemAction autoSize fab cardOverlayFab mini" data-action="playmenu"><i class="md-icon cardOverlayFab-md-icon">&#xE037;</i></button>',h++),s.LocalTrailerCount&&(l+='<button title="'+n.translate("sharedcomponents#Trailer")+'" is="emby-button" class="itemAction autoSize fab cardOverlayFab mini" data-action="playtrailer"><i class="md-icon cardOverlayFab-md-icon">&#xE04B;</i></button>',h++);var L="dots-horiz"==o.moreIcon?"&#xE5D3;":"&#xE5D4;";return l+='<button is="emby-button" class="itemAction autoSize fab cardOverlayFab mini" data-action="menu" data-playoptions="false"><i class="md-icon cardOverlayFab-md-icon">'+L+"</i></button>",h++,l+=i.getIconsHtml({item:s,style:"fab-mini",cssClass:"cardOverlayFab",iconCssClass:"cardOverlayFab-md-icon"}),l+="</div>",l+="</div>"}function u(t){var a=t.querySelector(".cardOverlayTarget");if(!a){a=document.createElement("div"),a.classList.add("hide"),a.classList.add("cardOverlayTarget");var i=t.querySelector("div.cardContent")||t.querySelector(".cardScalable")||t.querySelector(".cardBox");i||(i=t),i.classList.add("withHoverMenu"),i.appendChild(a)}var r=s.parentWithAttribute(t,"data-id");if(r){var n=r.getAttribute("data-id"),o=r.getAttribute("data-type");if("Timer"!=o){var d=r.getAttribute("data-serverid"),c=e.getApiClient(d),u=c.getItem(c.getCurrentUserId(),n),v=c.getCurrentUser();Promise.all([u,v]).then(function(e){var t=e[0],i=e[1];a.innerHTML=m(c,t,i,r)}),l(a)}}}function v(e){var t=e.target,a=s.parentWithClass(t,"cardBox");if(a){if(p===!0)return void(p=!1);y&&(clearTimeout(y),y=null),y=setTimeout(function(){u(a)},1e3)}}function f(){p=!0}function g(e){this.parent=e,this.parent.addEventListener("mouseenter",v,!0),this.parent.addEventListener("mouseleave",d,!0),s.parent.addEventListener(this.parent,"touchstart",f,{passive:!0})}var y,p=!1;return g.prototype={constructor:g,destroy:function(){this.parent.removeEventListener("mouseenter",v,!0),this.parent.removeEventListener("mouseleave",d,!0),s.parent.removeEventListener(this.parent,"touchstart",f,{passive:!0})}},g});