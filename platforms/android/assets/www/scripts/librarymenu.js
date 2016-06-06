define(["imageLoader","layoutManager","viewManager"],function(e,n,t){function a(){var n="";n+='<div class="primaryIcons">';var t=browserInfo.safari?"chevron-left":"arrow-back";n+='<button type="button" is="paper-icon-button-light" class="headerButton headerButtonLeft headerBackButton hide"><iron-icon icon="'+t+'"></iron-icon></button>',n+='<button type="button" is="paper-icon-button-light" class="headerButton mainDrawerButton barsMenuButton headerButtonLeft"><iron-icon icon="menu"></iron-icon></button>',n+='<button type="button" is="paper-icon-button-light" class="headerButton headerAppsButton barsMenuButton headerButtonLeft"><iron-icon icon="menu"></iron-icon></button>',n+='<div class="libraryMenuButtonText headerButton">'+Globalize.translate("ButtonHome")+"</div>",n+='<div class="viewMenuSecondary">',n+='<span class="headerSelectedPlayer"></span>',n+='<button is="paper-icon-button-light" class="btnCast headerButton headerButtonRight hide"><iron-icon icon="cast"></iron-icon></button>',AppInfo.enableSearchInTopMenu&&(n+='<button type="button" is="paper-icon-button-light" class=headerButton headerButtonRight headerSearchButton hide" onclick="Search.showSearchPanel();"><iron-icon icon="search"></iron-icon></button>',n+='<div class="viewMenuSearch hide">',n+='<form class="viewMenuSearchForm">',n+='<input type="text" data-role="none" data-type="search" class="headerSearchInput" autocomplete="off" spellcheck="off" />',n+='<button type="button" is="paper-icon-button-light" class="btnCloseSearch"><iron-icon icon="close"></iron-icon></button>',n+="</form>",n+="</div>"),n+='<button is="paper-icon-button-light" class="headerButton headerButtonRight headerVoiceButton hide"><iron-icon icon="mic"></iron-icon></button>',n+='<button is="paper-icon-button-light" class="headerButton headerButtonRight btnNotifications clearButton"><div class="btnNotificationsInner">0</div></button>',n+='<button is="paper-icon-button-light" class="headerButton headerButtonRight headerUserButton"><iron-icon icon="person"></iron-icon></button>',browserInfo.mobile||Dashboard.isConnectMode()||(n+='<button is="paper-icon-button-light" class="headerButton headerButtonRight dashboardEntryHeaderButton" onclick="return LibraryMenu.onSettingsClicked(event);"><iron-icon icon="settings"></iron-icon></button>'),n+="</div>",n+="</div>",n+='<div class="viewMenuBarTabs hiddenScrollX">',n+="</div>";var i=document.createElement("div");i.classList.add("viewMenuBar"),i.innerHTML=n,document.querySelector(".skinHeader").appendChild(i),e.lazyChildren(document.querySelector(".viewMenuBar")),document.dispatchEvent(new CustomEvent("headercreated",{})),b()}function r(){Dashboard.exitOnBack()?Dashboard.exit():history.back()}function o(e){var n=document.querySelector(".viewMenuBar");if(n){var t,i=n.querySelector(".headerUserButton");if(e&&e.name&&e.imageUrl){var a=26,r=e.imageUrl;e.supportsImageParams&&(r+="&height="+Math.round(a*Math.max(window.devicePixelRatio||1,2))),i&&(s(i,r,null),t=!0)}i&&!t&&s(i,null,"person"),e&&c(e.localUser),O=!1}}function s(e,n,t){n?(e.classList.add("headerUserButtonRound"),e.innerHTML='<img src="'+n+'" />'):t?(e.classList.remove("headerUserButtonRound"),e.innerHTML='<iron-icon icon="'+t+'"></iron-icon>'):e.classList.remove("headerUserButtonRound")}function c(e){var n=document.querySelector(".viewMenuBar"),t=n.querySelector(".headerSearchButton"),i=n.querySelector(".btnCast"),a=n.querySelector(".dashboardEntryHeaderButton");e?(i.classList.remove("hide"),t&&t.classList.remove("hide"),a&&(e.Policy.IsAdministrator?a.classList.remove("hide"):a.classList.add("hide")),require(["voice/voice"],function(e){e.isSupported()?n.querySelector(".headerVoiceButton").classList.remove("hide"):n.querySelector(".headerVoiceButton").classList.add("hide")})):(i.classList.add("hide"),n.querySelector(".headerVoiceButton").classList.add("hide"),t&&t.classList.add("hide"),a&&a.classList.add("hide"))}function l(){require(["voice/voice"],function(e){e.startListening()})}function d(e){Dashboard.showUserFlyout(e.target)}function u(){require(["dialogHelper"],function(e){var n=e.createDialog({removeOnClose:!0,modal:!1,autoFocus:!1,entryAnimationDuration:160,exitAnimationDuration:160,enableHistory:!1});n.classList.add("ui-body-a"),n.classList.add("background-theme-a"),n.classList.add("adminAppsMenu");var t="";t+='<div class="adminAppsMenuRow">',t+='<a class="adminAppsButton" href="home.html">',t+='<button is="paper-icon-button-light"><iron-icon icon="home"></iron-icon></button>',t+="<div>"+Globalize.translate("ButtonHome")+"</div>",t+="</a>",t+="</div>",t+='<div class="adminAppsMenuRow">',t+='<a class="adminAppsButton" href="edititemmetadata.html">',t+='<button is="paper-icon-button-light"><iron-icon icon="mode-edit"></iron-icon></button>',t+="<div>"+Globalize.translate("ButtonMetadataManager")+"</div>",t+="</a>",t+='<a class="adminAppsButton" href="reports.html">',t+='<button is="paper-icon-button-light"><iron-icon icon="insert-chart"></iron-icon></button>',t+="<div>"+Globalize.translate("ButtonReports")+"</div>",t+="</a>",t+="</div>",n.innerHTML=t,document.body.appendChild(n),n.addEventListener("click",function(t){var i=k(t.target,"A");i&&e.close(n)}),e.open(n)})}function b(){var e=document.querySelector(".mainDrawerButton");e&&e.addEventListener("click",v);var n=document.querySelector(".headerBackButton");n&&n.addEventListener("click",r);var t=document.querySelector(".headerVoiceButton");t&&t.addEventListener("click",l);var i=document.querySelector(".headerUserButton");i&&i.addEventListener("click",d);var a=document.querySelector(".headerAppsButton");a&&a.addEventListener("click",u);var o=document.querySelector(".viewMenuBar");z(o),o.querySelector(".btnNotifications").addEventListener("click",function(){Dashboard.navigate("notificationlist.html")})}function h(e,n){return LibraryBrowser.getHref(e,n)}function v(){"drawer"==R.selected?y(R):m(R)}function m(e){e=e||document.querySelector(".mainDrawerPanel"),e.openDrawer(),V=(new Date).getTime()}function p(){browserInfo.mobile&&document.body.classList.add("bodyWithPopupOpen")}function y(e){e=e||document.querySelector(".mainDrawerPanel"),e.closeDrawer()}function L(e){var n=e.target;"drawer"!=n.selected?document.body.classList.remove("bodyWithPopupOpen"):p()}function g(e,n){var t="";t+='<div style="height:.5em;"></div>';var i=window.ApiClient?"home.html":"selectserver.html?showuser=1";t+='<a class="lnkMediaFolder sidebarLink" href="'+i+'" onclick="return LibraryMenu.onLinkClicked(event, this);">',t+="<div style=\"background-image:url('css/images/mblogoicon.png');width:28px;height:28px;background-size:contain;background-repeat:no-repeat;background-position:center center;border-radius:1000px;vertical-align:middle;margin:0 1.6em 0 1.5em;display:inline-block;\"></div>",t+=Globalize.translate("ButtonHome"),t+="</a>",t+='<a class="sidebarLink lnkMediaFolder" data-itemid="remote" href="nowplaying.html" onclick="return LibraryMenu.onLinkClicked(event, this);"><iron-icon icon="tablet-android" class="sidebarLinkIcon"></iron-icon><span class="sidebarLinkText">'+Globalize.translate("ButtonRemote")+"</span></a>",t+='<div class="sidebarDivider"></div>',t+='<div class="libraryMenuOptions">',t+="</div>";var a=e.localUser;a&&a.Policy.IsAdministrator&&(t+='<div class="adminMenuOptions">',t+='<div class="sidebarDivider"></div>',t+='<div class="sidebarHeader">',t+=Globalize.translate("HeaderAdmin"),t+="</div>",t+='<a class="sidebarLink lnkMediaFolder lnkManageServer" data-itemid="dashboard" href="#"><iron-icon icon="dashboard" class="sidebarLinkIcon"></iron-icon><span class="sidebarLinkText">'+Globalize.translate("ButtonManageServer")+"</span></a>",t+='<a class="sidebarLink lnkMediaFolder editorViewMenu" data-itemid="editor" onclick="return LibraryMenu.onLinkClicked(event, this);" href="edititemmetadata.html"><iron-icon icon="mode-edit" class="sidebarLinkIcon"></iron-icon><span class="sidebarLinkText">'+Globalize.translate("ButtonMetadataManager")+"</span></a>",browserInfo.mobile||(t+='<a class="sidebarLink lnkMediaFolder" data-itemid="reports" onclick="return LibraryMenu.onLinkClicked(event, this);" href="reports.html"><iron-icon icon="insert-chart" class="sidebarLinkIcon"></iron-icon><span class="sidebarLinkText">'+Globalize.translate("ButtonReports")+"</span></a>"),t+="</div>"),t+='<div class="userMenuOptions">',t+='<div class="sidebarDivider"></div>',e.localUser&&AppInfo.isNativeApp&&browserInfo.android&&(t+='<a class="sidebarLink lnkMediaFolder lnkMySettings" onclick="return LibraryMenu.onLinkClicked(event, this);" href="mypreferencesmenu.html?userId='+e.localUser.Id+'"><iron-icon icon="settings" class="sidebarLinkIcon"></iron-icon><span class="sidebarLinkText">'+Globalize.translate("ButtonSettings")+"</span></a>"),t+='<a class="sidebarLink lnkMediaFolder lnkMySync" data-itemid="mysync" onclick="return LibraryMenu.onLinkClicked(event, this);" href="mysync.html"><iron-icon icon="sync" class="sidebarLinkIcon"></iron-icon><span class="sidebarLinkText">'+Globalize.translate("ButtonSync")+"</span></a>",Dashboard.isConnectMode()&&(t+='<a class="sidebarLink lnkMediaFolder" data-itemid="selectserver" onclick="return LibraryMenu.onLinkClicked(event, this);" href="selectserver.html?showuser=1"><iron-icon icon="wifi" class="sidebarLinkIcon"></iron-icon><span class="sidebarLinkText">'+Globalize.translate("ButtonSelectServer")+"</span></a>"),e.localUser&&(t+='<a class="sidebarLink lnkMediaFolder" data-itemid="logout" onclick="return LibraryMenu.onLogoutClicked(this);" href="#"><iron-icon icon="lock" class="sidebarLinkIcon"></iron-icon><span class="sidebarLinkText">'+Globalize.translate("ButtonSignOut")+"</span></a>"),t+="</div>";var n=R.querySelector(".mainDrawer");n.innerHTML=t;var r=n.querySelector(".lnkManageServer");r&&r.addEventListener("click",D),require(["imageLoader"],function(e){e.fillImages(R.getElementsByClassName("lazy"))})}function f(e){R.querySelector(".adminDrawerLogo")?M():w(e)}function k(e,n){for(;e.tagName!=n;)if(e=e.parentNode,!e)return null;return e}function M(){for(var e=R.querySelectorAll(".sidebarLink"),n=0,i=e.length;i>n;n++){var a=e[n],r=!1,o=a.getAttribute("data-pageids");if(o&&(r=-1!=o.split(",").indexOf(t.currentView().id)),r){a.classList.add("selectedSidebarLink");var s="",c=(a.innerText||a.textContent).trim();s+=c;var l=c;Dashboard.setPageTitle(s,l)}else a.classList.remove("selectedSidebarLink")}}function w(){var e="";e+='<a class="adminDrawerLogo clearLink" href="home.html">',e+='<img src="css/images/logo.png" />',e+="</a>",e+=Dashboard.getToolsMenuHtml(),e=e.split("href=").join('onclick="return LibraryMenu.onLinkClicked(event, this);" href='),R.querySelector(".mainDrawer").innerHTML=e,M()}function B(){var e=this.getElementsByClassName("sectionName")[0],n=e?e.innerHTML:this.innerHTML;LibraryMenu.setTitle(n)}function S(e,n){return e.getUserViews({},n).then(function(e){for(var n=e.Items,t=[],i=0,a=n.length;a>i;i++){var r=n[i];if(t.push(r),"livetv"==r.CollectionType){r.ImageTags={},r.icon="live-tv",r.onclick="LibraryBrowser.showTab('livetv.html', 0);";var o=Object.assign({},r);o.Name=Globalize.translate("ButtonGuide"),o.ImageTags={},o.icon="dvr",o.url="livetv.html?tab=1",o.onclick="LibraryBrowser.showTab('livetv.html', 1);",t.push(o)}}return t})}function T(e,n){var t=document.querySelector(e);t&&(n?t.classList.remove("hide"):t.classList.add("hide"))}function C(e){if(!e)return T(".lnkMySync",!1),void T(".userMenuOptions",!1);e.Policy.EnableSync?T(".lnkMySync",!0):T(".lnkMySync",!1);var n=Dashboard.getCurrentUserId(),t=window.ApiClient,i=document.querySelector(".libraryMenuOptions");i&&S(t,n).then(function(e){var n=e,t="";t+='<div class="sidebarHeader">',t+=Globalize.translate("HeaderMedia"),t+="</div>",t+=n.map(function(e){var n="folder",t="inherit",i=e.Id;"channels"==e.CollectionType?i="channels":"livetv"==e.CollectionType&&(i="livetv"),"photos"==e.CollectionType?(n="photo-library",t="#009688"):"music"==e.CollectionType||"musicvideos"==e.CollectionType?(n="library-music",t="#FB8521"):"books"==e.CollectionType?(n="library-books",t="#1AA1E1"):"playlists"==e.CollectionType?(n="view-list",t="#795548"):"games"==e.CollectionType?(n="games",t="#F44336"):"movies"==e.CollectionType?(n="video-library",t="#CE5043"):"channels"==e.CollectionType||"Channel"==e.Type?(n="videocam",t="#E91E63"):"tvshows"==e.CollectionType?(n="tv",t="#4CAF50"):"livetv"==e.CollectionType&&(n="live-tv",t="#293AAE"),n=e.icon||n;var a=e.onclick?" function(){"+e.onclick+"}":"null";return'<a data-itemid="'+i+'" class="lnkMediaFolder sidebarLink" onclick="return LibraryMenu.onLinkClicked(event, this, '+a+');" href="'+h(e,e.CollectionType)+'"><iron-icon icon="'+n+'" class="sidebarLinkIcon" style="color:'+t+'"></iron-icon><span class="sectionName">'+e.Name+"</span></a>"}).join(""),i.innerHTML=t;for(var a=i,r=a.querySelectorAll(".sidebarLink"),o=0,s=r.length;s>o;o++)r[o].removeEventListener("click",B),r[o].addEventListener("click",B)})}function D(){y(),Dashboard.navigate("dashboard.html")}function q(){return getParameterByName("topParentId")||null}function P(){var e=document,n=e.querySelector(".btnCast");if(n){var t=MediaController.getPlayerInfo();t.isLocalPlayer?(n.querySelector("iron-icon").icon="cast",n.classList.remove("btnActiveCast"),e.querySelector(".headerSelectedPlayer").innerHTML=""):(n.querySelector("iron-icon").icon="cast-connected",n.classList.add("btnActiveCast"),e.querySelector(".headerSelectedPlayer").innerHTML=t.deviceName||t.name)}}function I(e){var n,t,i=e.classList.contains("liveTvPage"),a=e.classList.contains("channelsPage"),r=e.classList.contains("metadataEditorPage"),o=e.classList.contains("reportsPage"),s=e.classList.contains("mySyncPage"),c=i||a||r||o||s||e.classList.contains("allLibraryPage")?"":q()||"",l=document.getElementsByClassName("lnkMediaFolder");for(n=0,t=l.length;t>n;n++){var d=l[n],u=d.getAttribute("data-itemid");a&&"channels"==u?d.classList.add("selectedMediaFolder"):i&&"livetv"==u?d.classList.add("selectedMediaFolder"):r&&"editor"==u?d.classList.add("selectedMediaFolder"):o&&"reports"==u?d.classList.add("selectedMediaFolder"):s&&"mysync"==u?d.classList.add("selectedMediaFolder"):c&&u==c?d.classList.add("selectedMediaFolder"):d.classList.remove("selectedMediaFolder")}}function A(e){var n=e.querySelectorAll(".scopedLibraryViewNav a"),t=e.classList.contains("liveTvPage")||e.classList.contains("channelsPage")||e.classList.contains("metadataEditorPage")||e.classList.contains("reportsPage")||e.classList.contains("mySyncPage")||e.classList.contains("allLibraryPage")?"":q()||"";if(t)for(i=0,length=n.length;length>i;i++){var a=n[i],r=a.href;-1==r.indexOf("#")&&(r=replaceQueryString(r,"topParentId",t),a.href=r)}}function E(e,n){var t=n;"UserConfigurationUpdated"===t.MessageType&&t.Data.Id==Dashboard.getCurrentUserId()}function H(e){var t=document.querySelector(".viewMenuBar");t&&(e.classList.contains("standalonePage")?t.classList.add("hide"):t.classList.remove("hide"),e.classList.contains("type-interior")&&!n.mobile?t.classList.add("headroomDisabled"):t.classList.remove("headroomDisabled")),O&&ConnectionManager.user(window.ApiClient).then(o)}function x(e){var n=e.getAttribute("data-title")||e.getAttribute("data-contextname");if(!n){var t=getParameterByName("titlekey");t&&(n=Globalize.translate(t))}n&&LibraryMenu.setTitle(n)}function F(e){var n=!e.classList.contains("homePage")&&history.length>0,t=document.querySelector(".headerBackButton"),i=AppInfo.enableBackButton;i||(i="true"==e.getAttribute("data-backbutton")),t&&(n&&i?t.classList.remove("hide"):t.classList.add("hide"))}function z(e){AppInfo.enableHeadRoom&&require(["headroom"],function(){var n=new Headroom(e,{tolerance:{down:40,up:0}});n.init()})}function G(e){Events.off(e,"websocketmessage",E),Events.on(e,"websocketmessage",E)}function N(e){var n=!1;e||(e=t.currentView()),e&&e.classList.contains("type-interior")&&(n=!0),n?(R.classList.add("adminDrawerPanel"),R.classList.remove("darkDrawerPanel")):(R.classList.add("darkDrawerPanel"),R.classList.remove("adminDrawerPanel"))}function U(e){var n=e?Promise.resolve(e):ConnectionManager.user(window.ApiClient);n.then(function(e){g(e),document.dispatchEvent(new CustomEvent("libraryMenuCreated",{})),C(e.localUser)})}var R=document.querySelector(".mainDrawerPanel"),O=!0,V=(new Date).getTime();window.LibraryMenu={getTopParentId:q,onLinkClicked:function(e,n,t){return 1!=e.which?!0:((new Date).getTime()-V>200&&setTimeout(function(){y();var e=browserInfo.mobile?350:200;setTimeout(function(){t?t():Dashboard.navigate(n.href)},e)},50),e.stopPropagation(),e.preventDefault(),!1)},onLogoutClicked:function(){if((new Date).getTime()-V>200){y();var e=browserInfo.mobile?350:200;setTimeout(function(){Dashboard.logout()},e)}return!1},onHardwareMenuButtonClick:function(){v()},onSettingsClicked:function(e){return 1!=e.which?!0:(Dashboard.navigate("dashboard.html"),!1)},setTabs:function(e,n,t){var i;return e?(i=document.querySelector(".viewMenuBarTabs"),LibraryMenu.tabType||i.classList.remove("hide"),LibraryMenu.tabType!=e?void require(["paper-tabs"],function(){var a=browserInfo.animate?"":" noink";i.innerHTML='<paper-tabs selected="'+n+'" hidescrollbuttons '+a+">"+t().map(function(e){return'<paper-tab link><a class="clearLink paperTabLink" href="'+e.href+'"><div>'+e.name+"</div></a></paper-tab>"}).join("")+"</paper-tabs>",R.classList.add("withTallToolbar"),LibraryMenu.tabType=e}):(i.querySelector("paper-tabs").selected=n,void(LibraryMenu.tabType=e))):void(LibraryMenu.tabType&&(R.classList.remove("withTallToolbar"),i=document.querySelector(".viewMenuBarTabs"),i.innerHTML="",i.classList.add("hide"),LibraryMenu.tabType=null))},setTitle:function(e){var n=e,i=t.currentView();if(i){var a=i.getAttribute("data-helpurl");a&&(n+='<a href="'+a+'" target="_blank" class="clearLink" style="margin-left:2em;" title="'+Globalize.translate("ButtonHelp")+'"><button is="emby-button" type="button" class="accent" style="margin:0;font-weight:normal;font-size:13px;padding:.25em;display:block;align-items:center;"><iron-icon icon="info"></iron-icon><span>'+Globalize.translate("ButtonHelp")+"</span></button></a>")}var r=document.querySelector(".libraryMenuButtonText");r&&(r.innerHTML=n)},setBackButtonVisible:function(e){var n=document.querySelector(".headerBackButton");n&&(e?n.classList.remove("hide"):n.classList.add("hide"))},setMenuButtonVisible:function(e){var n=document.querySelector(".mainDrawerButton");n&&n.classList.remove(!e&&browserInfo.mobile?"hide":"hide")},setTransparentMenu:function(e){var n=document.querySelector(".viewMenuBar");n&&(e?n.classList.add("semiTransparent"):n.classList.remove("semiTransparent"))}},pageClassOn("pageinit","page",function(){var e=this,n=e.classList.contains("libraryPage");if(n)for(var t=e.querySelectorAll(".libraryViewNav"),i=0,a=t.length;a>i;i++)z(t[i])}),pageClassOn("pagebeforeshow","page",function(){var e=this;e.classList.contains("withTabs")||LibraryMenu.setTabs(null)}),pageClassOn("pageshow","page",function(e){var n=this,t=n.classList.contains("type-interior");t?(f(n),R.forceNarrow=!1):(R.classList.contains("adminDrawerPanel")&&U(),R.forceNarrow=!0),N(n),H(n),A(n),e.detail.isRestored||window.scrollTo(0,0),x(n),F(n),n.classList.contains("libraryPage")?(document.body.classList.add("libraryDocument"),document.body.classList.remove("dashboardDocument"),document.body.classList.remove("hideMainDrawer")):t?(document.body.classList.remove("libraryDocument"),document.body.classList.add("dashboardDocument"),document.body.classList.remove("hideMainDrawer")):(document.body.classList.remove("libraryDocument"),document.body.classList.remove("dashboardDocument"),document.body.classList.add("hideMainDrawer")),I(n)}),window.ApiClient&&G(window.ApiClient),R.addEventListener("iron-select",L);var j,W;require(["paper-icon-button-light","emby-icons"],function(){a(),j=!0;var e=W;e&&o(e),W=null}),Events.on(ConnectionManager,"apiclientcreated",function(e,n){G(n)}),Events.on(ConnectionManager,"localusersignedin",function(e,n){N(),ConnectionManager.user(ConnectionManager.getApiClient(n.ServerId)).then(function(e){U(e),j?o(e):W=e})}),Events.on(ConnectionManager,"localusersignedout",o),Events.on(MediaController,"playerchange",P),N()});