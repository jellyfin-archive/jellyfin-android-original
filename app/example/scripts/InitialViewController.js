angular
  .module('example')
  .controller('InitialViewController', function ($scope, supersonic) {

      $scope.next = function () {

          showConnectSignIn();
      };

      function showWelcome() {

          $scope.$apply(function () {
              $scope.showWelcome = true;
              $scope.showServerSignIn = false;
              $scope.showServerSelection = false;
              $scope.showConnectSignIn = false;
          });
          hideLoading();
      }

      function startConnectionProcess() {

          steroids.logger.log('Calling App.connect');
          App.connect().done(function (result) {

              steroids.logger.log('App.connect done');
              steroids.logger.log('result.State: ' + result.State);

              switch (result.State) {

                  case MediaBrowser.ConnectionState.ConnectSignIn:
                      showWelcome();
                      break;
                  case MediaBrowser.ConnectionState.SignedIn:
                      App.handleSignedInResult(result);
                      break;
                  case MediaBrowser.ConnectionState.ServerSignIn:
                      showServerSignIn(result);
                      break;
                  case MediaBrowser.ConnectionState.ServerSelection:
                      showServerSelection();
                      break;
                  default:
                      steroids.logger.log('Unhandled ConnectionState');
                      break;
              }
          });
      }

      function startConnectionProcessWithDelay() {

          // https://github.com/AppGyver/steroids/issues/614
          setTimeout(startConnectionProcess, 1000);
      }

      function startLeftDrawer() {

          steroids.logger.log('Creating drawer view');
          supersonic.ui.views.start(new supersonic.ui.View({
              location: "example#drawer",
              id: "leftDrawer"

          })).then(startConnectionProcessWithDelay, startConnectionProcessWithDelay);
      }

      //localStorage.removeItem('servercredentials3');

      function beginView() {

          showLoading();

          App.setBackgroundImage("images/splash.jpg");

          steroids.logger.log('Creating global view');
          supersonic.ui.views.start(new supersonic.ui.View({
              location: "example#global",
              id: "global"

          })).then(startLeftDrawer, startLeftDrawer);
      }

      store.removeItem('viewName');
      Header.load(false);
      beginView();

      function showLoading() {
          steroids.view.displayLoading();
      }

      function hideLoading() {

          steroids.view.removeLoading();

          // not sure if this is a bug but sometimes the header disappears after removing loading
          Header.load(false);
      }

      /** Server Selection */
      function showServerSelection() {

          // Depending on where we're coming from, two different execution methods are needed
          var applyScope = function () {
              $scope.showWelcome = false;
              $scope.showServerSignIn = false;
              $scope.showServerSelection = true;
              $scope.showConnectSignIn = false;
          };
          $scope.$apply(applyScope);
          applyScope();

          loadServers();
          loadConnectInfo();
      }

      function navigateToNewServer() {

          supersonic.ui.modal.show(new supersonic.ui.View("example#newserver"), {
              animate: true
          });
      }

      function loadServers() {

          steroids.logger.log('Calling App.getAvailableServers');
          App.getAvailableServers().done(function (result) {

              $scope.$apply(function () {
                  $scope.servers = result;
              });

              hideLoading();
          });
      }

      $scope.newServer = function () {
          navigateToNewServer();
      };

      supersonic.data.channel('newserverresult').subscribe(function (message) {
          processServerConnectionResult(message);
      });

      $scope.connectToServer = function (server) {

          showLoading();

          steroids.logger.log('Calling App.connectToServer');
          App.connectToServer(server).done(function (result) {

              steroids.logger.log('result.State: ' + result.State);

              processServerConnectionResult(result);
          });
      };

      function processServerConnectionResult(result) {

          hideLoading();

          switch (result.State) {

              case MediaBrowser.ConnectionState.Unavailable:

                  supersonic.ui.dialog.alert("Connection Failure", {
                      message: "We're unable to reach this server. Please ensure it is running and try again."
                  });
                  break;
              case MediaBrowser.ConnectionState.ServerSignIn:
                  showServerSignIn(result);
                  break;
              case MediaBrowser.ConnectionState.SignedIn:
                  App.handleSignedInResult(result);
                  break;
              default:
                  steroids.logger.log('Unhandled ConnectionState');
                  break;
          }
      }

      $scope.signInWithConnect = function () {

          showConnectSignIn();
      };

      /** Server Sign In */
      supersonic.data.channel('manualloginresult').subscribe(function (message) {
          App.handleAuthenticationResult(message);
      });

      function loginToServer(username, password) {

          var serverId = $scope.serverId;

          App.loginToServer(serverId, username, password)
		  .done(function (result) {
		      App.handleAuthenticationResult(result);
		  })
		  .fail(function () {
		      supersonic.ui.dialog.alert("Sign In Failure", {
		          message: "Invalid username or password entered. Please try again."
		      });
		  });
      }

      function showServerSignIn(result) {

          // Depending on where we're coming from, two different execution methods are needed
          var applyScope = function () {
              $scope.showWelcome = false;
              $scope.showServerSignIn = true;
              $scope.showServerSelection = false;
              $scope.showConnectSignIn = false;
          };
          $scope.$apply(applyScope);
          applyScope();

          var server = result.Servers[0];

          console.log('handleServerSignInResult');
          console.log('ServerId: ' + server.Id);
          $scope.serverId = server.Id;

          App.getApiClient(server.Id).done(function (apiClient) {

              apiClient.getPublicUsers().done(function (users) {
                  renderUsers(apiClient, users);
              });

              hideLoading();
          });

          loadConnectInfo();
      }

      function loadConnectInfo() {

          App.isLoggedIntoConnect().done(function (result) {

              $scope.$apply(function () {
                  $scope.isNotLoggedIntoConnect = !result.isLoggedIntoConnect;
              });
          });
      }

      $scope.showManualLogin = function (username) {

          var url = "example#manuallogin?serverid=" + $scope.serverId;

          if (username) {
              url += "&username=" + username;
          }

          supersonic.ui.modal.show(new supersonic.ui.View(url), {
              animate: true
          });
      };

      $scope.loginUser = function (user) {

          if (user.HasPassword) {
              $scope.showManualLogin(user.Name);
          }
          else {
              loginToServer(user.Name);
          }
      };

      $scope.changeServer = function () {
          showServerSelection();
      };

      function renderUsers(apiClient, users) {

          $scope.$apply(function () {

              $scope.users = users.map(function (u) {

                  if (u.PrimaryImageTag) {
                      u.ImageUrl = apiClient.getUserImageUrl(u.Id, {
                          type: 'Primary',
                          tag: u.PrimaryImageTag
                      });
                  }
                  else {
                      u.ImageUrl = "/images/user.png";
                  }

                  return u;
              });
          });
      }

      /** Connect Sign In */
      function showConnectSignIn() {

          // Depending on where we're coming from, two different execution methods are needed
          var applyScope = function () {
              $scope.showWelcome = false;
              $scope.showServerSignIn = false;
              $scope.showServerSelection = false;
              $scope.showConnectSignIn = true;
              $scope.username = '';
              $scope.password = '';
          };
          $scope.$apply(applyScope);
          applyScope();

          hideLoading();
      }

      function loginToConnect(username, password) {

          if (!username) {
              document.getElementsByClassName('txtUsername')[0].focus();
              return;
          }
          if (!password) {
              document.getElementsByClassName('txtPassword')[0].focus();
              return;
          }

          steroids.view.displayLoading();

          steroids.logger.log('Calling App.loginToConnect');
          App.loginToConnect(username, password).done(function () {

              steroids.logger.log('Connect authentication succeeded');

              steroids.logger.log('Calling App.connect');
              App.connect().done(function (result) {

                  steroids.logger.log('result.State: ' + result.State);

                  switch (result.State) {

                      case MediaBrowser.ConnectionState.ServerSelection:
                          showServerSelection();
                          break;
                      case MediaBrowser.ConnectionState.ServerSignIn:
                          App.handleServerSignInResult(result);
                          break;
                      case MediaBrowser.ConnectionState.SignedIn:
                          App.handleSignedInResult(result);
                          break;
                      default:
                          steroids.logger.log('Unhandled ConnectionState');
                          break;
                  }
              });

          }).fail(function () {

              steroids.logger.log('Connect authentication failed');
              steroids.view.removeLoading();

              supersonic.ui.dialog.alert("Sign In Error", {
                  message: "Invalid username or password. Please try again."

              });
          });
      }

      $scope.loginToConnect = function () {

          loginToConnect($scope.username, $scope.password);
      };

      $scope.skipConnect = function () {

          showServerSelection();
      };
  });
