/*
@date  : 11.05.2015
@author: Pavel Durov
@note  :  In order to use ActionSheetProxy on Universal Windows Platform -
		  Make sure that you are referencing base.js and ui.js 
 */
function ActionSheet() { /* CTOR...*/ }


ActionSheet.prototype.show = function (options, successCallback, errorCallback) {
	
    ActionSheet.prototype.successCallBack = successCallback;
    ActionSheet.prototype.errorCallback = errorCallback;

    if (cordova.platformId == "windows") {
        ActionSheet.prototype._injectWinJsFlyoutHTML();
    }

    if (options) {
        var actionSheetProxyFlyoutDiv = document.getElementById("actionSheetProxyFlyoutDiv");
        //Settings popup
        this._clearChildren(actionSheetProxyFlyoutDiv);

        if (options.title) {    //Append Title
            this._addTitle(options.title, actionSheetProxyFlyoutDiv);
        }

        if (!options.destructiveButtonLast && options.addDestructiveButtonWithLabel) {    //Generate Desctructive Button
            this._addDestructiveButton(options.addDestructiveButtonWithLabel, actionSheetProxyFlyoutDiv);
        }

        if (options.buttonLabels && options.buttonLabels instanceof Array) {    //Appending Buttons to main Flyout Div
            this._addbuttons(options.buttonLabels, actionSheetProxyFlyoutDiv);
        }

        if (options.destructiveButtonLast && options.addDestructiveButtonWithLabel) {    //Generate Desctructive Button
            this._addDestructiveButton(options.addDestructiveButtonWithLabel, actionSheetProxyFlyoutDiv);
        }

        if (options.winphoneEnableCancelButton && options.addCancelButtonWithLabel) {

            this._addCancelButton(options.addCancelButtonWithLabel, actionSheetProxyFlyoutDiv);
        }
        
        var fly = document.getElementById("fly-test");
        WinJS.UI.process(fly).done(function () {

            if (fly && fly.winControl) {
                //var bodys = document.getElementsByClassName('bodyClass')[0];
                var anchor = document.getElementById("actionSheetSetPoint");
                if (anchor) {
                    fly.winControl.show(anchor, "bottom");
                }


            } else {
                console.log("winControl is undefined");
            }
        });
    }

    document.addEventListener('backbutton', window.plugins.actionsheet.hide, false)
};


ActionSheet.prototype.hide = function (options, successCallback, errorCallback) {
    console.log("//\\//\\//\\//\\//\\ ActionSheet.prototype.hide ");

    var fly = document.getElementById("fly-test");

    WinJS.UI.process(fly).done(function () {
        if (fly.winControl && fly.winControl.hide) {
            //WinJS.UI.Animation.hidePopup(fly).done( /* Your success and error handlers */);
            fly.winControl.hide();
        }
    });
};



ActionSheet.prototype.ANDROID_THEMES = {
    THEME_TRADITIONAL: 1, // default
    THEME_HOLO_DARK: 2,
    THEME_HOLO_LIGHT: 3,
    THEME_DEVICE_DEFAULT_DARK: 4,
    THEME_DEVICE_DEFAULT_LIGHT: 5
};

ActionSheet.prototype.install = function () {
    if (!window.plugins) {
        window.plugins = {};
    }
    window.plugins.actionsheet = new ActionSheet();

    return window.plugins.actionsheet;
};

cordova.addConstructor(ActionSheet.prototype.install);
cordova.commandProxy.add("ActionSheet", ActionSheet);


//Helpers


ActionSheet.prototype._addTitle = function (label, destinationCtrl) {
    var textCtrl = this._generateTitle(label)
    destinationCtrl.appendChild(textCtrl);
}


ActionSheet.prototype._addDestructiveButton = function (label, destinationCtrl) {
    if (label && destinationCtrl) {
        var destructive_btn = this._generateButton(label, this._getDestructiveButtonStyle());
        destinationCtrl.appendChild(destructive_btn);
    }
}

ActionSheet.prototype._addCancelButton = function (label, destinationCtrl) {
    if (label && destinationCtrl) {
        var cancel_btn = this._generateButton(label, this._getCancelButtonStyle());
        cancel_btn.onclick = function () {
            ActionSheet.prototype.hide();
        }

        destinationCtrl.appendChild(cancel_btn);
    }
}

ActionSheet.prototype._addbuttons = function (lables, destinationCtrl) {
    if (lables && destinationCtrl) {
        for (var i = 0; i < lables.length; i++) {
            var btn = this._generateButton(lables[i], this._getButtonStyle());
	    (function (i) {
            	btn.onclick = function () {
                    if (ActionSheet.prototype.successCallBack) {
                    	ActionSheet.prototype.successCallBack(i + 1);
                    	ActionSheet.prototype.hide();
                    }                
            	}
            })(i);
            destinationCtrl.appendChild(btn);
        }
    }
}

ActionSheet.prototype._clearChildren = function (element) {
    if (element && element.hasChildNodes) {
        while (element.hasChildNodes()) {
            element.removeChild(element.lastChild);
        }
    }
}

ActionSheet.prototype._generateButton = function (content, style) {

    var btn = document.createElement("input");
    btn.setAttribute("type", "button");
    btn.setAttribute("value", content);

    btn.setAttribute("style", style);//;

    return btn;
}


//Style
ActionSheet.prototype._getButtonStyle = function () {
    return "display: table-row; font-size: 20px; background-color :black;" +
            " color: white; width: 98%; position: relative; margin: 1%; background: black; margin-top: 20px; height: 60px;"
}
//The only diffrence beweet regulat button and cancel vuttokn styles is WIDTH!!
ActionSheet.prototype._getCancelButtonStyle = function () {
    return "display: table-row; font-size: 20px; background-color :black;" +
            " color: white; width: 49%; position: relative; margin: 1%; background: black; margin-top: 20px; height: 60px;"
}
//The only diffrence beweet destructive button and regular is its color (orange)!
ActionSheet.prototype._getDestructiveButtonStyle = function () {
    return "display: table-row; font-size: 20px; background-color : black;" +
            " color: orange; width: 98%; position: relative; margin: 1%; background: black; margin-top: 20px; height: 60px;"
}



ActionSheet.prototype._getMainDivStyle = function () {
    return "position:absolute; width:99%; display: table; background-color: #4F4F4F; padding: 15px; visibility:collapse ; text-align : center;";
}

ActionSheet.prototype._getTitleStyle = function () {
    return "color:white; font-size: 30px; ";
}

ActionSheet.prototype._generateTitle = function (textLebel) {
    var spanCtrl = document.createElement("span");
    spanCtrl.setAttribute("style", this._getTitleStyle());
    spanCtrl.innerHTML = textLebel;
    return spanCtrl;
}

ActionSheet.prototype._injectWinJsFlyoutHTML = function () {
    var divSetPoint = document.createElement("div");
    divSetPoint.setAttribute("id", "actionSheetSetPoint");
    divSetPoint.setAttribute("aria-haspopup", "true");
    divSetPoint.setAttribute("style", "visibility:collapse");

    var flyoutDiv = document.createElement("div");
    flyoutDiv.setAttribute("data-win-control", "WinJS.UI.Flyout");
    flyoutDiv.setAttribute("id", "fly-test");
    flyoutDiv.setAttribute("style", this._getMainDivStyle());

    var internalDiv = document.createElement("div");
    internalDiv.setAttribute("id", "actionSheetProxyFlyoutDiv");

    flyoutDiv.appendChild(internalDiv);

    document.body.appendChild(flyoutDiv);
    document.body.appendChild(divSetPoint);
}