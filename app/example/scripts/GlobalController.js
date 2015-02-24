angular
  .module('example')
  .controller('GlobalController', function ($scope, supersonic) {

      var self = $scope;

      self.capabilities = function () {

          return {

          };
      };

      self.connectionManager = function () {

          var deferred = DeferredBuilder.Deferred();

          if (self.connectionManagerInstance) {
              deferred.resolveWith(null, [self.connectionManagerInstance]);
          }
          else {
              supersonic.device.ready.then(function () {
                  var credentialProvider = new MediaBrowser.CredentialProvider();
                  var deviceName = device.model;
                  var deviceId = device.uuid;

                  self.connectionManagerInstance = new MediaBrowser.ConnectionManager(Logger, credentialProvider, App.name(), App.version(), deviceName, deviceId, self.capabilities());

                  deferred.resolveWith(null, [self.connectionManagerInstance]);
              });
          }

          return deferred.promise();
      };

      self.processConnectRequest = function (connectionManager, message) {

          connectionManager.connect().done(function (result) {
              self.sendConnectionResult(result, message);
          });
      };

      self.processConnectToServerRequest = function (connectionManager, message) {

          connectionManager.connectToServer(message.serverInfo).done(function (result) {
              self.sendConnectionResult(result, message);
          });
      };

      self.processConnectToAddressRequest = function (connectionManager, message) {

          connectionManager.connectToAddress(message.address).done(function (result) {
              self.sendConnectionResult(result, message);
          });
      };

      self.processGetAvailableServersRequest = function (connectionManager, message) {

          connectionManager.getAvailableServers().done(function (result) {
              supersonic.data.channel('connectionmanager').publish({
                  requestId: message.requestId,
                  result: result,
                  response: true
              });
          });
      };

      self.processUserRequest = function (connectionManager, message) {

          connectionManager.user().done(function (result) {
              supersonic.data.channel('connectionmanager').publish({
                  requestId: message.requestId,
                  result: result,
                  response: true
              });
          });
      };

      self.processLogoutRequest = function (connectionManager, message) {

          connectionManager.logout().done(function (result) {

              var afterPopAll = function () {
                  //supersonic.ui.initialView.show();
                  App.hideTabs();
                  steroids.view.displayLoading();

                  var view = new supersonic.ui.View("example#initial-view");
                  supersonic.ui.layers.push(view);
                  //supersonic.ui.initialView.show();

                  // Hack for now since connection manager event isn't firing
                  supersonic.data.channel('connectionmanagerevents').publish({
                      type: 'localusersignedout',
                      data: {}
                  });

              };

              supersonic.ui.layers.popAll().then(afterPopAll, afterPopAll);
          });
      };

      self.processLoginToConnectRequest = function (connectionManager, message) {

          connectionManager.loginToConnect(message.username, message.password).done(function (result) {
              supersonic.data.channel('connectionmanager').publish({
                  requestId: message.requestId,
                  result: result,
                  response: true
              });

          }).fail(function () {
              supersonic.data.channel('connectionmanager').publish({
                  requestId: message.requestId,
                  response: true,
                  error: true
              });
          });
      };

      self.processIsLoggedIntoConnectRequest = function (connectionManager, message) {

          var messageResult = {
              isLoggedIntoConnect: connectionManager.isLoggedIntoConnect()
          };

          supersonic.data.channel('connectionmanager').publish({
              requestId: message.requestId,
              result: messageResult,
              response: true
          });
      };

      self.processLoginToServerRequest = function (connectionManager, message) {

          var apiClient = connectionManager.getOrCreateApiClient(message.serverId);

          apiClient.authenticateUserByName(message.username, message.password).done(function (result) {
              supersonic.data.channel('connectionmanager').publish({
                  requestId: message.requestId,
                  result: result,
                  response: true
              });

          }).fail(function () {
              supersonic.data.channel('connectionmanager').publish({
                  requestId: message.requestId,
                  response: true,
                  error: true
              });
          });
      };

      function getApiClientInfo(apiClient) {

          return {
              deviceName: apiClient.deviceName(),
              deviceId: apiClient.deviceId(),
              serverInfo: apiClient.serverInfo(),
              serverAddress: apiClient.serverAddress()
          };
      }

      self.processApiClientRequest = function (connectionManager, message) {

          var apiClient = connectionManager.getOrCreateApiClient(message.serverId);

          var result = getApiClientInfo(apiClient);

          supersonic.data.channel('connectionmanager').publish({
              requestId: message.requestId,
              result: result,
              response: true
          });
      };

      self.sendConnectionResult = function (result, originalMessage) {

          if (result.ApiClient) {
              result.ApiClient = getApiClientInfo(result.ApiClient);
          }

          supersonic.data.channel('connectionmanager').publish({
              requestId: originalMessage.requestId,
              result: result,
              response: true
          });
      };

      function bindToChannel() {

          steroids.logger.log('GlobalController subscribing to connectionmanager channel');
          supersonic.data.channel('connectionmanager').subscribe(function (message) {

              if (message.response) {
                  return;
              }

              if (!message.type) {
                  steroids.logger.log('Invalid connection manager request: ' + JSON.stringify(message));
                  return;
              }

              steroids.logger.log('Received connection manager request: ' + message.type);

              self.connectionManager().done(function (connectionManager) {

                  if (message.type == "connect") {
                      self.processConnectRequest(connectionManager, message);
                  } else if (message.type == "connecttoserver") {
                      self.processConnectToServerRequest(connectionManager, message);
                  } else if (message.type == "connecttoaddress") {
                      self.processConnectToAddressRequest(connectionManager, message);
                  } else if (message.type == "getavailableservers") {
                      self.processGetAvailableServersRequest(connectionManager, message);
                  } else if (message.type == "logintoconnect") {
                      self.processLoginToConnectRequest(connectionManager, message);
                  } else if (message.type == "logintoserver") {
                      self.processLoginToServerRequest(connectionManager, message);
                  } else if (message.type == "apiclient") {
                      self.processApiClientRequest(connectionManager, message);
                  } else if (message.type == "isloggedintoconnect") {
                      self.processIsLoggedIntoConnectRequest(connectionManager, message);
                  } else if (message.type == "user") {
                      self.processUserRequest(connectionManager, message);
                  } else if (message.type == "logout") {
                      self.processLogoutRequest(connectionManager, message);
                  }
              });

          });
      }

      bindToChannel();

      function bindEvent(connectionManager, eventName) {

          Events.on(connectionManager, eventName, function (e, data) {

              steroids.logger.log('Publishing connection manager event: ' + eventName);

              supersonic.data.channel('connectionmanagerevents').publish({
                  type: eventName,
                  data: data
              });
          });
      }

      function bindEvents(connectionManager) {

          var events = ['localusersignedin', 'localusersignedout', 'connectusersignedin', 'connectusersignedout'];

          for (var i = 0, length = events.length; i < length; i++) {

              var eventName = events[i];
              bindEvent(connectionManager, eventName);
          }
      }

      self.connectionManager().done(function (connectionManager) {

          bindEvents(connectionManager);
      });

  });
