(function (globalScope) {

    globalScope.App = function () {

        var self = {};

        self.name = function () {
            return "Media Browser";
        };

        self.version = function () {
            return "3";
        };

        self.capabilities = function () {

            return {

            };
        };

        self.newRequestId = function () {
            return Math.random() + '';
        };

        self.createApiClient = function (info) {

            var apiClient = new MediaBrowser.ApiClient(Logger, info.serverAddress, self.name(), self.version(), info.deviceName, info.deviceId);

            var serverInfo = info.serverInfo;
            apiClient.serverInfo(serverInfo);
            apiClient.setCurrentUserId(serverInfo.UserId, serverInfo.AccessToken);

            return apiClient;
        };

        self.listenForConnectionResult = function (requestId, deferred) {

            supersonic.data.channel('connectionmanager').subscribe(function(message) {

                if (message.response && message.requestId == requestId) {

                    if (message.result.ApiClient) {
                        message.result.ApiClient = self.createApiClient(message.result.ApiClient);
                    }

                    deferred.resolveWith(null, [message.result]);
                }
            });
        };

        self.listenForResult = function (requestId, deferred, debugAlert) {

            supersonic.data.channel('connectionmanager').subscribe(function (message) {

                if (message.response && message.requestId == requestId) {

                    if (debugAlert) {
                        alert(JSON.stringify(message));
                    }
                    if (message.error) {
                        deferred.rejectWith(null, [message.result]);
                    } else {
                        deferred.resolveWith(null, [message.result]);
                    }
                }
            });
        };

        self.connect = function () {

            var deferred = DeferredBuilder.Deferred();

            var requestId = self.newRequestId();

            self.listenForConnectionResult(requestId, deferred);

            supersonic.data.channel('connectionmanager').publish({
                requestId: requestId,
                type: "connect"
            });

            return deferred.promise();
        };

        self.connectToServer = function (serverInfo) {

            var deferred = DeferredBuilder.Deferred();

            var requestId = self.newRequestId();

            self.listenForConnectionResult(requestId, deferred);

            supersonic.data.channel('connectionmanager').publish({
                requestId: requestId,
                type: "connecttoserver",
                serverInfo: serverInfo
            });

            return deferred.promise();
        };

        self.connectToAddress = function (address) {

            var deferred = DeferredBuilder.Deferred();

            var requestId = self.newRequestId();

            self.listenForConnectionResult(requestId, deferred);

            supersonic.data.channel('connectionmanager').publish({
                requestId: requestId,
                type: "connecttoaddress",
                address: address
            });

            return deferred.promise();
        };

        self.getAvailableServers = function () {

            var deferred = DeferredBuilder.Deferred();

            var requestId = self.newRequestId();

            self.listenForResult(requestId, deferred);

            supersonic.data.channel('connectionmanager').publish({
                requestId: requestId,
                type: "getavailableservers"
            });

            return deferred.promise();
        };

        self.loginToConnect = function (username, password) {

            var deferred = DeferredBuilder.Deferred();

            var requestId = self.newRequestId();

            self.listenForResult(requestId, deferred);

            supersonic.data.channel('connectionmanager').publish({
                requestId: requestId,
                type: "logintoconnect",
                username: username,
                password: password
            });

            return deferred.promise();
        };

        self.isLoggedIntoConnect = function () {

            var deferred = DeferredBuilder.Deferred();

            var requestId = self.newRequestId();

            self.listenForResult(requestId, deferred);

            supersonic.data.channel('connectionmanager').publish({
                requestId: requestId,
                type: "isloggedintoconnect"
            });

            return deferred.promise();
        };

        self.loginToServer = function (serverId, username, password) {

            var deferred = DeferredBuilder.Deferred();

            var requestId = self.newRequestId();

            self.listenForResult(requestId, deferred);

            supersonic.data.channel('connectionmanager').publish({
                requestId: requestId,
                type: "logintoserver",
                username: username,
                password: password,
                serverId: serverId
            });

            return deferred.promise();
        };

        self.listenForApiClientResult = function (requestId, deferred) {

            supersonic.data.channel('connectionmanager').subscribe(function (message) {

                if (message.response && message.requestId == requestId) {

                    if (message.error) {
                        deferred.rejectWith(null, [message.result]);
                    } else {
                        deferred.resolveWith(null, [self.createApiClient(message.result)]);
                    }
                }
            });
        };

        self.getApiClient = function (serverId) {

            var deferred = DeferredBuilder.Deferred();

            var requestId = self.newRequestId();

            self.listenForApiClientResult(requestId, deferred);

            supersonic.data.channel('connectionmanager').publish({
                requestId: requestId,
                type: "apiclient",
                serverId: serverId
            });

            return deferred.promise();
        };

        self.setBackgroundImage = function (url) {
            steroids.view.setBackgroundImage(url);
            self.addClass(document.body, 'clearBody');
        };

        self.clearBackgroundImage = function () {
            steroids.view.setBackgroundImage(null);
            self.removeClass(document.body, 'clearBody');
        };

        self.addClass = function (elem, name) {

            var css = ' ' + (elem.className || '') + ' ';

            if (css.indexOf(' ' + name + ' ') == -1) {
                elem.className = (css + name).trim();
            }
        };

        self.removeClass = function (elem, name) {

            var css = ' ' + (elem.className || '') + ' ';
            elem.className = css.replace(' ' + name + ' ', '').trim();
        };

        self.navigateToServerSelection = function () {

            supersonic.ui.layers.push(new supersonic.ui.View("example#selectserver"), {
                animation: 'fade'
            });
        };

        self.navigateToConnectSignIn = function () {

            supersonic.ui.layers.push(new supersonic.ui.View("example#connectsignin"), {
                animation: 'fade'
            });
        };

        self.handleServerSignInResult = function (result, animate) {

            var server = result.Servers[0];

            console.log('handleServerSignInResult');
            console.log('ServerId: ' + server.Id);
            supersonic.ui.layers.push(new supersonic.ui.View("example#serversignin?serverid=" + server.Id), {
            });
        };

        self.handleSignedInResult = function (result) {
            supersonic.ui.initialView.dismiss();
            App.loadViewTabs('home');
        };

        self.handleAuthenticationResult = function (result) {
            supersonic.ui.initialView.dismiss();
            App.loadViewTabs('home');

        };

        self.loadViewTabs = function (context) {

            var css = 'tabs-' + context;

            //supersonic.ui.tabs.setStyleClass(css);

            supersonic.ui.tabs.show();
            supersonic.ui.tabs.replace([
                  {
                      title: "Home",
                      location: "example#home"
                  },
                  {
                      title: "Favorites",
                      location: "example#favorites"
                  },
                  {
                      title: "Favorites",
                      location: "example#favorites1"
                  },
                  {
                      title: "Favorites",
                      location: "example#favorites2"
                  },
                  {
                      title: "Favorites",
                      location: "example#favorites3"
                  },
                  {
                      title: "Favorites",
                      location: "example#favorites4"
                  },
                  {
                      title: "Favorites",
                      location: "example#favorites5"
                  }
            ]);
        };

        return self;
    }();

})(this);