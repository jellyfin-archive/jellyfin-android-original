(function(globalScope){function paperDialogHashHandler(dlg,hash,lockDocumentScroll){function onHashChange(e){var data=e.detail.state||{};var isActive=data.hash=='#'+hash;if(data.direction=='back'){if(dlg){if(!isActive){dlg.close();dlg=null;}}}}
function onDialogClosed(){if(lockDocumentScroll!==false){Dashboard.onPopupClose();}
dlg=null;if(enableHashChange()){window.removeEventListener('navigate',onHashChange);if(window.location.hash=='#'+hash){history.back();}}}
dlg.addEventListener('iron-overlay-closed',onDialogClosed);dlg.open();if(lockDocumentScroll!==false){Dashboard.onPopupOpen();}
if(enableHashChange()){window.location.hash=hash;window.addEventListener('navigate',onHashChange);}}
function enableHashChange(){if(browserInfo.msie){return false;}
if(browserInfo.edge){return false;}
return true;}
function openWithHash(dlg,hash,lockDocumentScroll){new paperDialogHashHandler(dlg,hash,lockDocumentScroll);}
function close(dlg){if(enableHashChange()){if(dlg.opened){history.back();}}else{dlg.close();}}
function createDialog(options){options=options||{};var dlg=document.createElement('paper-dialog');dlg.setAttribute('with-backdrop','with-backdrop');dlg.setAttribute('role','alertdialog');if(!browserInfo.msie&&!browserInfo.mozilla){if(options.modal!==false){dlg.setAttribute('modal','modal');}}
dlg.setAttribute('noAutoFocus','noAutoFocus');if(!browserInfo.mobile){dlg.entryAnimation='scale-up-animation';dlg.exitAnimation='fade-out-animation';}
dlg.classList.add('popupEditor');if(options.size=='small'){dlg.classList.add('small-paper-dialog');}
else if(options.size=='medium'){dlg.classList.add('medium-paper-dialog');}else{dlg.classList.add('fullscreen-paper-dialog');}
var theme=options.theme||'b';dlg.classList.add('ui-body-'+theme);dlg.classList.add('background-theme-'+theme);dlg.classList.add('smoothScrollY');return dlg;}
function positionTo(dlg,elem){var windowHeight=$(window).height();if(windowHeight>=540){var pos=$(elem).offset();pos.top+=elem.offsetHeight/2;pos.left+=elem.offsetWidth/2;pos.top-=24;pos.left-=24;pos.top-=$(dlg).height()/2;pos.left-=$(dlg).width()/2;pos.top-=$(window).scrollTop();pos.left-=$(window).scrollLeft();pos.top=Math.min(pos.top,windowHeight-300);pos.left=Math.min(pos.left,$(window).width()-300);pos.top=Math.max(pos.top,0);pos.left=Math.max(pos.left,0);dlg.style.position='fixed';dlg.style.left=pos.left+'px';dlg.style.top=pos.top+'px';}}
globalScope.PaperDialogHelper={openWithHash:openWithHash,close:close,createDialog:createDialog,positionTo:positionTo};})(this);