//
//  ConnectSDKCordovaObjects.h
//  Connect SDK
//
//  Copyright (c) 2014 LG Electronics.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

#import "ConnectSDKCordova.h"
#import "ConnectSDKCordovaDispatcher.h"
#import <ConnectSDK/ConnectSDK.h>

@interface JSObjectWrapper : NSObject
@property (nonatomic, strong) ConnectSDKCordova* plugin;
@property (nonatomic, strong) NSString* objectId;
@property (nonatomic, strong) NSString* callbackId;

- (void) sendEvent:(NSString*)event withObject:(id)obj;
- (void) cleanup;
@end

@interface ConnectableDeviceWrapper : JSObjectWrapper
@property (nonatomic, strong) id device;
@property (nonatomic, strong) JSCommandDispatcher* dispatcher;
@end

@interface WebAppSessionWrapper : JSObjectWrapper<WebAppSessionDelegate>
@property (nonatomic, strong) WebAppSession* session;

- (instancetype) initWithPlugin:(ConnectSDKCordova*)plugin session:(WebAppSession*)session;

- (void) webAppSession:(WebAppSession *)webAppSession didReceiveMessage:(id)message;
- (void) webAppSessionDidDisconnect:(WebAppSession *)webAppSession;
@end

@interface MediaControlWrapper : JSObjectWrapper
@property (nonatomic, strong) id<MediaControl> mediaControl;

- (instancetype) initWithPlugin:(ConnectSDKCordova*)plugin mediaControl:(id<MediaControl>)mediaControl;
@end

@interface PlaylistControlWrapper : JSObjectWrapper

@property (nonatomic, strong) id<PlayListControl> playlistControl;

- (instancetype)initWithPlugin:(ConnectSDKCordova*)plugin
               playlistControl:(id<PlayListControl>)playlistControl;

@end
