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

            supersonic.data.channel('connectionmanager').subscribe(function (message) {

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

        self.sendConnectionManagerRequest = function (request) {

            steroids.logger.log('Sending connection manager request: ' + JSON.stringify(request));
            supersonic.data.channel('connectionmanager').publish(request);
        };

        self.connect = function () {

            var deferred = DeferredBuilder.Deferred();

            var requestId = self.newRequestId();

            self.listenForConnectionResult(requestId, deferred);

            self.sendConnectionManagerRequest({
                requestId: requestId,
                type: "connect"
            });

            return deferred.promise();
        };

        self.connectToServer = function (serverInfo) {

            var deferred = DeferredBuilder.Deferred();

            var requestId = self.newRequestId();

            self.listenForConnectionResult(requestId, deferred);

            self.sendConnectionManagerRequest({
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

            self.sendConnectionManagerRequest({
                requestId: requestId,
                type: "connecttoaddress",
                address: address
            });

            return deferred.promise();
        };

        self.user = function () {

            var deferred = DeferredBuilder.Deferred();

            var requestId = self.newRequestId();

            self.listenForResult(requestId, deferred);

            self.sendConnectionManagerRequest({
                requestId: requestId,
                type: "user"
            });

            return deferred.promise();
        };

        self.logout = function () {

            var requestId = self.newRequestId();

            self.sendConnectionManagerRequest({
                requestId: requestId,
                type: "logout"
            });
        };

        self.getAvailableServers = function () {

            var deferred = DeferredBuilder.Deferred();

            var requestId = self.newRequestId();

            self.listenForResult(requestId, deferred);

            self.sendConnectionManagerRequest({
                requestId: requestId,
                type: "getavailableservers"
            });

            return deferred.promise();
        };

        self.loginToConnect = function (username, password) {

            var deferred = DeferredBuilder.Deferred();

            var requestId = self.newRequestId();

            self.listenForResult(requestId, deferred);

            self.sendConnectionManagerRequest({
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

            self.sendConnectionManagerRequest({
                requestId: requestId,
                type: "isloggedintoconnect"
            });

            return deferred.promise();
        };

        self.loginToServer = function (serverId, username, password) {

            var deferred = DeferredBuilder.Deferred();

            var requestId = self.newRequestId();

            self.listenForResult(requestId, deferred);

            self.sendConnectionManagerRequest({
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

            self.sendConnectionManagerRequest({
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

        self.handleSignedInResult = function (result, serverId) {

            serverId = serverId || result.Servers[0].Id;

            var loadHome = function () {
                supersonic.ui.initialView.dismiss();
                App.loadView('home', null, 'Home', serverId);
            };

            supersonic.ui.layers.popAll().then(loadHome, loadHome);
        };

        self.handleAuthenticationResult = function (result) {

            self.handleSignedInResult(result, result.User.ServerId);
        };

        self.getTabs = function (viewType, parentId, name, serverId) {

            viewType = (viewType || 'folders').toLowerCase();

            var tabs = [];
            var addServerId = true;
            var addParentId = true;

            if (viewType == 'home') {
                tabs.push({
                    title: "Home",
                    location: "example#home"
                });
                tabs.push({
                    title: "Favorites",
                    location: "example#favorites"
                });
                addParentId = false;
            }
            else if (viewType == 'movies') {
                tabs.push({
                    title: "Suggested",
                    location: "example#movies-suggested"
                });
                tabs.push({
                    title: "Movies",
                    location: "example#items?mode=movies"
                });
                tabs.push({
                    title: "Trailers",
                    location: "example#items?mode=trailers"
                });
                tabs.push({
                    title: "Collections",
                    location: "example#items?mode=collections"
                });
                tabs.push({
                    title: "Genres",
                    location: "example#items?mode=genres"
                });
            }
            else if (viewType == 'tvshows') {
                tabs.push({
                    title: "Suggested",
                    location: "example#tv-suggested"
                });
                tabs.push({
                    title: "Upcoming",
                    location: "example#items?mode=upcoming"
                });
                tabs.push({
                    title: "Shows",
                    location: "example#items?mode=series"
                });
                tabs.push({
                    title: "Genres",
                    location: "example#items?mode=genres"
                });
            }
            else if (viewType == 'music') {
                tabs.push({
                    title: "Suggested",
                    location: "example#music-suggested"
                });
                tabs.push({
                    title: "Album",
                    location: "example#items?mode=musicalbums"
                });
                tabs.push({
                    title: "Album Artists",
                    location: "example#items?mode=albumartists"
                });
                tabs.push({
                    title: "Artists",
                    location: "example#items?mode=artists"
                });
                tabs.push({
                    title: "Genres",
                    location: "example#items?mode=musicgenres"
                });
            }
            else {
                tabs.push({
                    title: name,
                    location: "example#items?mode=" + viewType
                });
            }

            if (addServerId || addParentId) {
                for (var i = 0, length = tabs.length; i < length; i++) {

                    var loc = tabs[i].location;
                    if (loc.indexOf('?') == -1) {
                        loc += '?';
                    } else {
                        loc += '&';
                    }

                    if (addServerId) {
                        loc += 'serverid=' + serverId;
                    }

                    if (addParentId) {
                        if (addServerId) {
                            loc += '&';
                        }
                        loc += 'parentid=' + parentId;
                    }

                    tabs[i].location = loc;
                }
            }

            return tabs;
        };

        self.loadView = function (viewType, parentId, name, serverId) {

            store.setItem('viewName', (viewType == 'home' ? '' : name));
            var tabs = self.getTabs(viewType, parentId, name, serverId);

            supersonic.ui.tabs.replace(tabs);

            if (tabs.length) {
                supersonic.ui.tabs.show();
            } else {
                supersonic.ui.tabs.hide();
            }
            Header.load();
        };

        self.hideTabs = function () {

            supersonic.ui.tabs.hide();

        };

        return self;
    }();

})(this);