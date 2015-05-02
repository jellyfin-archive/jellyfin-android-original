// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

exports.defineManualTests = function(rootEl, addButton) {

  function sendTo(data, addr, port) {
    chrome.sockets.udp.create(function(createInfo) {
      chrome.sockets.udp.bind(createInfo.socketId, '0.0.0.0', 0, function(result) {
        chrome.sockets.udp.send(createInfo.socketId, data, addr, port, function(result) {
          if (result < 0) {
            console.log('send fail: ' + result);
            chrome.sockets.udp.close(createInfo.socketId);
          } else {
            console.log('sendTo: success ' + port);
            chrome.sockets.udp.close(createInfo.socketId);
          }
        });
      });
    });
  }

  function receiveErrorListener(info) {
    console.log('RecvError on socket: ' + info.socketId);
    chrome.sockets.udp.close(info.socketId);
  }

  function receiveListener(info) {
    console.log('Recv from socket: ');
    console.log(info);
    chrome.sockets.udp.close(info.socketId);
  }

  function addReceiveListeners() {
    chrome.sockets.udp.onReceiveError.addListener(receiveErrorListener);
    chrome.sockets.udp.onReceive.addListener(receiveListener);
  }

  function removeReceiveListeners() {
    chrome.sockets.udp.onReceiveError.removeListener(receiveErrorListener);
    chrome.sockets.udp.onReceive.removeListener(receiveListener);
  }

  function bindRecvFromSendTo(data) {
    chrome.sockets.udp.create(function(createInfo) {
      chrome.sockets.udp.bind(createInfo.socketId, '0.0.0.0', 0, function(result) {
        chrome.sockets.udp.getInfo(createInfo.socketId, function(socketInfo) {
          sendTo(data, socketInfo.localAddress, socketInfo.localPort);
        });
      });
    });
  }

  function bindRecvFromSendToWithBufferSize(data, bufferSize) {
    chrome.sockets.udp.create({bufferSize:bufferSize}, function(createInfo) {
      chrome.sockets.udp.bind(createInfo.socketId, '0.0.0.0', 0, function(result) {
        chrome.sockets.udp.getInfo(createInfo.socketId, function(socketInfo) {
          sendTo(data, socketInfo.localAddress, socketInfo.localPort);
        });
      });
    });
  }

  function pauseBindRecvFromSendTo(data) {
    chrome.sockets.udp.create(function(createInfo) {
      chrome.sockets.udp.setPaused(createInfo.socketId, true, function() {
        chrome.sockets.udp.bind(createInfo.socketId, '0.0.0.0', 0, function(result) {
          chrome.sockets.udp.getInfo(createInfo.socketId, function(socketInfo) {
            sendTo(data, socketInfo.localAddress, socketInfo.localPort);
          });
        });
      });
    });
  }

  function bindPauseRecvFromSendTo(data) {
    chrome.sockets.udp.create(function(createInfo) {
      chrome.sockets.udp.bind(createInfo.socketId, '0.0.0.0', 0, function(result) {
        chrome.sockets.udp.setPaused(createInfo.socketId, true, function() {
          chrome.sockets.udp.getInfo(createInfo.socketId, function(socketInfo) {
            sendTo(data, socketInfo.localAddress, socketInfo.localPort);
          });
        });
      });
    });
  }

  function stringToArrayBuffer(string) {
    // UTF-16LE
    var buf = new ArrayBuffer(string.length * 2);
    var bufView = new Uint16Array(buf);
    for (var i = 0, strLen = string.length; i < strLen; i++) {
      bufView[i] = string.charCodeAt(i);
    }
    return buf;
  }

  function joinMulticastGroup(address, port, loopback) {
    chrome.sockets.udp.create(function (socket) {
      var socketId = socket.socketId;
      console.log(socket);
      chrome.sockets.udp.setMulticastTimeToLive(socketId, 12, function (result) {
        if (result != 0) {
          console.log("Set TTL Error: " + result);
        }
        chrome.sockets.udp.setMulticastLoopbackMode(socketId, loopback, function (result) {
          if (result != 0) {
            console.log("Set Multicast LoopbackMode" + result);
          }
          chrome.sockets.udp.bind(socketId, "0.0.0.0", port, function (result) {
            if (result != 0) {
              chrome.sockets.udp.close(socketId);
              console.log("Error on bind(): " + result);
            } else {
              chrome.sockets.udp.joinGroup(socketId, address, function (result) {
                if (result != 0) {
                  chrome.sockets.udp.close(socketId);
                  console.log("Error on joinGroup(): " + result);
                } else {
                  message = stringToArrayBuffer(socketId + ' Joined Group');
                  chrome.sockets.udp.send(socketId, message, address, port, function (result) {
                      if (result < 0) {
                        console.log('send fail: ' + result);
                      } else {
                        console.log('group sendTo: success');
                      }
                  });
                }
              });
            }
          });
        });
      });
    });
  }

  function getSockets() {
    chrome.sockets.udp.getSockets(function(socketsInfo) {
      if (!socketsInfo) return;
      for (var i = 0; i < socketsInfo.length; i++) {
        console.log(socketsInfo[i]);
      }
    });
  }

  function updateSocket() {
    chrome.sockets.udp.create({}, function(createInfo) {
      updatedProperties = {
        persistent: true,
        name: 'testUpdate',
        bufferSize: 2048
      };
      chrome.sockets.udp.update(createInfo.socketId, updatedProperties);

      chrome.sockets.udp.getInfo(createInfo.socketId, function(socketInfo) {
        console.log(socketInfo);
      });
    });
  }

  function setPaused() {
    chrome.sockets.udp.create({}, function(createInfo) {
      chrome.sockets.udp.setPaused(createInfo.socketId, true);
      chrome.sockets.udp.getInfo(createInfo.socketId, function(socketInfo) {
        console.log(socketInfo);
      });
    });
  }

  function closeSockets() {
    chrome.sockets.udp.getSockets(function(socketsInfo) {
      if (!socketsInfo) return;
      for (var i = 0; i < socketsInfo.length; i++) {
        console.log('closing socket: ' + socketsInfo[i].socketId);
        chrome.sockets.udp.close(socketsInfo[i].socketId);
      }
    });
  }

  function sendBroadcast(data) {
    var port = 1667;
    chrome.sockets.udp.create(function(createInfo) {
      chrome.sockets.udp.bind(createInfo.socketId, '0.0.0.0', port, function(result) {
        chrome.sockets.udp.send(createInfo.socketId, data, '255.255.255.255', port, function(result) {
          if (result < 0) {
            console.log('send fail: ' + result);
            chrome.sockets.udp.close(createInfo.socketId);
          } else {
            console.log('sendTo: success ' + port);
            chrome.sockets.udp.close(createInfo.socketId);
          }
        });
      });
    });
  }

  function initPage() {

    var defaultAddr = '127.0.0.1';
    var defaultPort = 12345;
    var multicastAddr = '224.0.0.1';

    var arr = new Uint8Array(256);
    for (var i = 0; i < arr.length; i++) {
      arr[i] = i;
    }

    addButton('send', function() {
      sendTo(arr.buffer, defaultAddr, defaultPort);
    });

    addButton('add receive listeners', function() {
      addReceiveListeners();
    });

    addButton('remove receive listeners', function() {
      removeReceiveListeners();
    });

    addButton('send to a bind socket', function() {
      bindRecvFromSendTo(arr.buffer);
    });

    addButton('send to a tiny buffer bind socket', function() {
      bindRecvFromSendToWithBufferSize(arr.buffer, 8);
    });

    addButton('send to a extra large buffer bind socket', function() {
      bindRecvFromSendToWithBufferSize(arr.buffer, 1048576);
    });

    addButton('get sockets', function() {
      getSockets();
    });

    addButton('update socket', function() {
      updateSocket();
    });

    addButton('set paused', function() {
      setPaused();
    });

    addButton('send to a paused socket', function() {
      pauseBindRecvFromSendTo(arr.buffer);
      bindPauseRecvFromSendTo(arr.buffer);
    });

    addButton('send broadcast message', function() {
      sendBroadcast(arr.buffer);
    });

    addButton('close sockets', function() {
      closeSockets();
    });

    addButton('Join multicast group', function() {
      joinMulticastGroup(multicastAddr, defaultPort, false);
    });

    addButton('Join multicast group with loopback', function() {
      joinMulticastGroup(multicastAddr, defaultPort, true);
    });
  }

  initPage();

};

exports.defineAutoTests = function() {
  'use strict';

  require('cordova-plugin-chrome-apps-test-framework.jasmine_helpers').addJasmineHelpers();

  //constants
  var bindAddr = '0.0.0.0';
  var multicastAddr = '224.0.1.' + Math.floor(Math.random()*256); // 224.0.1.0 to 239.255.255.255
  var multicastAddr2 = '224.0.2.' + Math.floor(Math.random()*256);
  var port = Math.floor(Math.random() * (65535-1024)) + 1024; // random in 1024 -> 65535
  var arr = new Uint8Array(256);
  for (var i = 0; i < arr.length; i++) {
    arr[i] = i;
  }
  var data = arr.buffer;

  // Socket management -- Make sure we clean up sockets after each test, even upon failure
  var sockets = [];

  function createSocket(properties, callback) {
    if (typeof properties == 'function') {
      callback = properties;
      properties = {};
    }
    chrome.sockets.udp.create(properties, function(createInfo) {
      expect(createInfo).toBeTruthy();
      expect(createInfo.socketId).toBeDefined();
      sockets.push(createInfo);
      callback();
    });
  }

  function createSockets(count, callback) {
    if (!count)
      return setTimeout(callback, 0);
    createSocket(createSockets.bind(null, count-1, callback));
  }

  beforeEach(function() {
    var customMatchers = {
      toBeValidUdpReadResultEqualTo: function(util, customEqualityTesters) {
        return {
          compare: function(actual, expected) {
            if (Object.prototype.toString.call(expected).slice(8, -1) !== "ArrayBuffer")
              throw new Error("toBeValidUdpReadResultEqualTo expects an ArrayBuffer");
            var result = { pass: true };
            if (!actual) result.pass = false;
            if (!actual.data) result.pass = false;
            if (Object.prototype.toString.call(actual.data).slice(8, -1) !== "ArrayBuffer") result.pass = false;

            var sent = new Uint8Array(expected);
            var recv = new Uint8Array(actual.data);
            if (recv.length !== sent.length) result.pass = false;

            for (var i = 0; i < recv.length; i++) {
              if (recv[i] !== sent[i]) result.pass = false;
            }
            return result;
          }
        };
      }
    };

    jasmine.addMatchers(customMatchers);
  });

  beforeEach(function(done) {
    createSockets(2, done);
  });

  afterEach(function() {
    sockets.forEach(function(createInfo) {
      chrome.sockets.udp.close(createInfo.socketId);
    });
    sockets = [];
  });

  it('should contain definitions', function() {
    expect(chrome.sockets.udp).toBeDefined();
    expect(chrome.sockets.udp.create).toBeDefined();
    expect(chrome.sockets.udp.update).toBeDefined();
    expect(chrome.sockets.udp.setPaused).toBeDefined();
    expect(chrome.sockets.udp.bind).toBeDefined();
    expect(chrome.sockets.udp.send).toBeDefined();
    expect(chrome.sockets.udp.close).toBeDefined();
    expect(chrome.sockets.udp.getInfo).toBeDefined();
    expect(chrome.sockets.udp.getSockets).toBeDefined();
    expect(chrome.sockets.udp.joinGroup).toBeDefined();
    expect(chrome.sockets.udp.leaveGroup).toBeDefined();
    expect(chrome.sockets.udp.setMulticastTimeToLive).toBeDefined();
    expect(chrome.sockets.udp.setMulticastLoopbackMode).toBeDefined();
    expect(chrome.sockets.udp.getJoinedGroups).toBeDefined();
  });

  describe('UDP', function() {

    it('port is available (sanity test)', function(done) {
      chrome.sockets.udp.bind(sockets[0].socketId, bindAddr, port, function(bindResult) {
        expect(bindResult).toEqual(0);
        done();
      });
    });

    it('bind to port 0 works', function(done) {
      chrome.sockets.udp.bind(sockets[0].socketId, bindAddr, 0, function(bindResult) {
        expect(bindResult).toEqual(0);
        done();
      });
    });

    it('getInfo works', function(done) {
      chrome.sockets.udp.bind(sockets[0].socketId, bindAddr, port, function(bindResult) {
        expect(bindResult).toEqual(0);

        chrome.sockets.udp.getInfo(sockets[0].socketId, function(socketInfo) {
          expect(socketInfo.persistent).toBe(false);
          expect(socketInfo.name).toBeFalsy();
          expect(socketInfo.paused).toBe(false);
          expect(socketInfo.localAddress).toBeTruthy();
          expect(socketInfo.localPort).toBeTruthy();
          done();
        });
      });
    });

    it('update socket', function(done) {
      chrome.sockets.udp.bind(sockets[0].socketId, bindAddr, port, function(bindResult) {
        expect(bindResult).toEqual(0);
        var updatedProperties = {
          persistent: true,
          name: 'testUpdate',
          bufferSize: 2048
        };
        chrome.sockets.udp.update(sockets[0].socketId, updatedProperties, function() {
          chrome.sockets.udp.getInfo(sockets[0].socketId, function(socketInfo) {
            expect(socketInfo.persistent).toEqual(updatedProperties.persistent);
            expect(socketInfo.name).toEqual(updatedProperties.name);
            expect(socketInfo.bufferSize).toEqual(updatedProperties.bufferSize);
            expect(socketInfo.paused).toBe(false);
            expect(socketInfo.localAddress).toBeTruthy();
            expect(socketInfo.localPort).toBeTruthy();
            done();
          });
        });
      });
    });

    it('recv with default buffer size', function(done) {
      var recvListener = function(info) {
        expect(info.socketId).toEqual(sockets[0].socketId);
        expect(info.remotePort).toBeTruthy();
        expect(info.remoteAddress).toBeTruthy();
        expect(info).toBeValidUdpReadResultEqualTo(data);
        chrome.sockets.udp.onReceive.removeListener(recvListener);
        done();
      };

      chrome.sockets.udp.onReceive.addListener(recvListener);
      chrome.sockets.udp.bind(sockets[0].socketId, bindAddr, port, function(bindResult) {
        expect(bindResult).toEqual(0);
        chrome.sockets.udp.getInfo(sockets[0].socketId, function(socketInfo) {
          expect(socketInfo.persistent).toBe(false);
          expect(socketInfo.name).toBeFalsy();
          expect(socketInfo.paused).toBe(false);
          expect(socketInfo.localAddress).toBeTruthy();
          expect(socketInfo.localPort).toBeTruthy();
        });

        chrome.sockets.udp.bind(sockets[1].socketId, bindAddr, 0, function(bindResult) {
          expect(bindResult).toEqual(0);
          chrome.sockets.udp.send(sockets[1].socketId, data, bindAddr, port, function(sendInfo) {
            expect(sendInfo.resultCode).toEqual(0);
            expect(sendInfo.bytesSent).toEqual(256);
            console.log('default sent success');
          });
        });
      });
    });

    it('recv with large buffer size', function(done) {
      var recvListener = function(info) {
        expect(info.socketId).toEqual(sockets[0].socketId);
        expect(info.remotePort).toBeTruthy();
        expect(info.remoteAddress).toBeTruthy();
        expect(info).toBeValidUdpReadResultEqualTo(data);
        chrome.sockets.udp.onReceive.removeListener(recvListener);
        done();
      };

      chrome.sockets.udp.onReceive.addListener(recvListener);
      chrome.sockets.udp.bind(sockets[0].socketId, bindAddr, port, function(bindResult) {
        expect(bindResult).toEqual(0);
        chrome.sockets.udp.getInfo(sockets[0].socketId, function(socketInfo) {
          expect(socketInfo.persistent).toBe(false);
          expect(socketInfo.name).toBeFalsy();
          expect(socketInfo.paused).toBe(false);
          expect(socketInfo.localAddress).toBeTruthy();
          expect(socketInfo.localPort).toBeTruthy();
        });

        var largeBufferProperties = {
          bufferSize: 1048576
        };
        chrome.sockets.udp.update(sockets[1].socketId, largeBufferProperties, function () {
          chrome.sockets.udp.bind(sockets[1].socketId, bindAddr, 0, function(bindResult) {
            expect(bindResult).toEqual(0);
            chrome.sockets.udp.send(sockets[1].socketId, data, bindAddr, port, function(sendInfo) {
              expect(sendInfo.resultCode).toEqual(0);
              expect(sendInfo.bytesSent).toEqual(256);
            });
          });
        });
      });
    });

    it('recv with small buffer size', function(done) {
      var recvListener = function(info) {
        expect(info.socketId).toEqual(sockets[0].socketId);
        expect(info.remotePort).toBeTruthy();
        expect(info.remoteAddress).toBeTruthy();
        chrome.sockets.udp.onReceive.removeListener(recvListener);
        done();
      };

      chrome.sockets.udp.onReceive.addListener(recvListener);
      chrome.sockets.udp.bind(sockets[0].socketId, bindAddr, port, function(bindResult) {
        expect(bindResult).toEqual(0);
        chrome.sockets.udp.getInfo(sockets[0].socketId, function(socketInfo) {
          expect(socketInfo.persistent).toBe(false);
          expect(socketInfo.name).toBeFalsy();
          expect(socketInfo.paused).toBe(false);
          expect(socketInfo.localAddress).toBeTruthy();
          expect(socketInfo.localPort).toBeTruthy();
        });

        var tinyBufferProperties = {
          bufferSize: 8
        };
        chrome.sockets.udp.update(sockets[1].socketId, tinyBufferProperties, function () {
          chrome.sockets.udp.bind(sockets[1].socketId, bindAddr, 0, function(bindResult) {
            expect(bindResult).toEqual(0);
            chrome.sockets.udp.send(sockets[1].socketId, data, bindAddr, port, function(sendInfo) {
              expect(sendInfo.resultCode).toEqual(0);
              expect(sendInfo.bytesSent).toEqual(256);
            });
          });
        });
      });
    });

    it('bind read send with replay', function(done) {
      var recvCounter = 0;
      var recvListener = function(info) {
        recvCounter++;
        if (recvCounter == 1) {
          expect(info.socketId).toEqual(sockets[0].socketId);
          expect(info.remotePort).toBeTruthy();
          expect(info.remoteAddress).toBeTruthy();
          expect(info).toBeValidUdpReadResultEqualTo(data);
          chrome.sockets.udp.send(sockets[0].socketId, data, info.remoteAddress, info.remotePort, function(sendInfo) {
            expect(sendInfo.resultCode).toEqual(0);
            expect(sendInfo.bytesSent).toEqual(256);
          });
        } else {
          expect(info.socketId).toEqual(sockets[1].socketId);
          expect(info.remotePort).toBeTruthy();
          expect(info.remoteAddress).toBeTruthy();
          expect(info).toBeValidUdpReadResultEqualTo(data);
          chrome.sockets.udp.onReceive.removeListener(recvListener);
          done();
        }
      };

      chrome.sockets.udp.onReceive.addListener(recvListener);
      chrome.sockets.udp.bind(sockets[0].socketId, bindAddr, port, function(bindResult) {
        expect(bindResult).toEqual(0);
        chrome.sockets.udp.getInfo(sockets[0].socketId, function(socketInfo) {
          expect(socketInfo.persistent).toBe(false);
          expect(socketInfo.name).toBeFalsy();
          expect(socketInfo.paused).toBe(false);
          expect(socketInfo.localAddress).toBeTruthy();
          expect(socketInfo.localPort).toBeTruthy();
        });

        chrome.sockets.udp.bind(sockets[1].socketId, bindAddr, 0, function(bindResult) {
          expect(bindResult).toEqual(0);
          chrome.sockets.udp.send(sockets[1].socketId, data, bindAddr, port, function(sendInfo) {
            expect(sendInfo.resultCode).toEqual(0);
            expect(sendInfo.bytesSent).toEqual(256);
          });
        });
      });
    });

  });

  describe('Multicast', function() {

    it('simple joinGroup', function(done) {
      chrome.sockets.udp.bind(sockets[0].socketId, bindAddr, port, function(bindResult) {
        expect(bindResult).toEqual(0);
        chrome.sockets.udp.joinGroup(sockets[0].socketId, multicastAddr, function(joinGroupResult) {
          expect(joinGroupResult).toEqual(0);
          done();
        });
      });
    });

    it('set time to live', function(done) {
      chrome.sockets.udp.setMulticastTimeToLive(sockets[0].socketId, 12, function(ttlResult) {
        expect(ttlResult).toEqual(0);
        done();
      });
    });

    it('simple joinGroup and leaveGroup', function(done) {
      chrome.sockets.udp.bind(sockets[0].socketId, bindAddr, port, function(bindResult) {
        expect(bindResult).toEqual(0);
        chrome.sockets.udp.joinGroup(sockets[0].socketId, multicastAddr, function(joinGroupResult) {
          expect(joinGroupResult).toEqual(0);
          chrome.sockets.udp.leaveGroup(sockets[0].socketId, multicastAddr, function(leaveGroupResult) {
            expect(leaveGroupResult).toEqual(0);
            chrome.sockets.udp.getJoinedGroups(sockets[0].socketId, function(joinedGroups) {
              expect(joinedGroups.length).toBe(0);
              done();
            });
          });
        });
      });
    });

    it('joinGroup and sent without loopback', function(done) {
      var recvListener = function(info) {
        expect(info.socketId).toEqual(sockets[0].socketId);
        expect(info.remotePort).toBeTruthy();
        expect(info.remoteAddress).toBeTruthy();
        expect(info).toBeValidUdpReadResultEqualTo(data);
        chrome.sockets.udp.onReceive.removeListener(recvListener);
        done();
      };

      chrome.sockets.udp.onReceive.addListener(recvListener);
      chrome.sockets.udp.bind(sockets[0].socketId, bindAddr, port, function(bindResult) {
        expect(bindResult).toEqual(0);
        chrome.sockets.udp.joinGroup(sockets[0].socketId, multicastAddr, function(joinGroupResult) {
          expect(joinGroupResult).toEqual(0);
          chrome.sockets.udp.bind(sockets[1].socketId, bindAddr, 0, function(bindResult) {
            expect(bindResult).toEqual(0);
            chrome.sockets.udp.send(sockets[1].socketId, data, multicastAddr, port, function(sendInfo) {
              expect(sendInfo.resultCode).toEqual(0);
              expect(sendInfo.bytesSent).toEqual(256);
            });
          });
        });
      });
    });

    it('joinGroup and sent with loopback', function(done) {
      var recvListener = function(info) {
        expect(info.socketId).toEqual(sockets[0].socketId);
        expect(info.remotePort).toBeTruthy();
        expect(info.remoteAddress).toBeTruthy();
        expect(info).toBeValidUdpReadResultEqualTo(data);
        chrome.sockets.udp.onReceive.removeListener(recvListener);
        done();
      };

      chrome.sockets.udp.onReceive.addListener(recvListener);

      chrome.sockets.udp.setMulticastLoopbackMode(sockets[0].socketId, true, function(loopbackResult) {
        expect(loopbackResult).toEqual(0);
        chrome.sockets.udp.bind(sockets[0].socketId, bindAddr, port, function(bindResult) {
          expect(bindResult).toEqual(0);
          chrome.sockets.udp.joinGroup(sockets[0].socketId, multicastAddr, function(joinGroupResult) {
            expect(joinGroupResult).toEqual(0);
            chrome.sockets.udp.send(sockets[0].socketId, data, multicastAddr, port, function(sendInfo) {
              expect(sendInfo.resultCode).toEqual(0);
              expect(sendInfo.bytesSent).toEqual(256);
            });
          });
        });
      });
    });

    it('joinGroup and use as a regular socket', function(done) {
      var recvListener = function(info) {
        expect(info.socketId).toEqual(sockets[0].socketId);
        expect(info.remotePort).toBeTruthy();
        expect(info.remoteAddress).toBeTruthy();
        expect(info).toBeValidUdpReadResultEqualTo(data);
        chrome.sockets.udp.onReceive.removeListener(recvListener);
        done();
      };

      chrome.sockets.udp.onReceive.addListener(recvListener);

      chrome.sockets.udp.bind(sockets[0].socketId, bindAddr, port, function(bindResult) {
        expect(bindResult).toEqual(0);
        chrome.sockets.udp.joinGroup(sockets[0].socketId, multicastAddr, function(joinGroupResult) {
          expect(joinGroupResult).toEqual(0);
          chrome.sockets.udp.bind(sockets[1].socketId, bindAddr, 0, function(bindResult) {
            expect(bindResult).toEqual(0);
            chrome.sockets.udp.send(sockets[1].socketId, data, bindAddr, port, function(sendInfo) {
              expect(sendInfo.resultCode).toEqual(0);
              expect(sendInfo.bytesSent).toEqual(256);
            });
          });
        });
      });
    });

    it('get joined groups', function(done) {
      chrome.sockets.udp.bind(sockets[0].socketId, bindAddr, port, function(bindResult) {
        expect(bindResult).toEqual(0);
        chrome.sockets.udp.joinGroup(sockets[0].socketId, multicastAddr, function(joinGroupResult) {
          expect(joinGroupResult).toEqual(0);
          chrome.sockets.udp.joinGroup(sockets[0].socketId, multicastAddr2, function(joinGroupResult) {
            expect(joinGroupResult).toEqual(0);
            chrome.sockets.udp.getJoinedGroups(sockets[0].socketId, function(joinedGroups) {
              expect(joinedGroups.length).toBe(2);
              expect(joinedGroups).toContain(multicastAddr);
              expect(joinedGroups).toContain(multicastAddr2);
              done();
            });
          });
        });
      });
    });

  });

};
