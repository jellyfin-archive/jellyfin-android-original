(function(){function forceScroll(){var doc=$(document);doc.scrollTop(doc.scrollTop()+1);}
function onOrientationChange(){forceScroll();for(var i=0;i<=500;i+=100){setTimeout(forceScroll,i);}}
window.addEventListener('orientationchange',onOrientationChange);})();