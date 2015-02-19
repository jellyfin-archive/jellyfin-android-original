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

      self.sendConnectionResult = function (result, originalMessage) {

          if (result.ApiClient) {
              result.ApiClient = {
                  deviceName: result.ApiClient.deviceName(),
                  deviceId: result.ApiClient.deviceId(),
                  serverInfo: result.ApiClient.serverInfo(),
                  serverAddress: result.ApiClient.serverAddress()
              };
          }

          supersonic.data.channel('connectionmanager').publish({
              requestId: originalMessage.requestId,
              result: result,
              response: true
          });
      };

      supersonic.data.channel('connectionmanager').subscribe(function (message) {

          self.connectionManager().done(function (connectionManager) {

              if (message.type == "connect") {
                  self.processConnectRequest(connectionManager, message);
              }
              else if (message.type == "connecttoserver") {
                  self.processConnectToServerRequest(connectionManager, message);
              }
              else if (message.type == "connecttoaddress") {
                  self.processConnectToAddressRequest(connectionManager, message);
              }
              else if (message.type == "getavailableservers") {
                  self.processGetAvailableServersRequest(connectionManager, message);
              }
              else if (message.type == "logintoconnect") {
                  self.processLoginToConnectRequest(connectionManager, message);
              }
              else if (message.type == "logintoserver") {
                  self.processLoginToServerRequest(connectionManager, message);
              }
          });

      });
  });
