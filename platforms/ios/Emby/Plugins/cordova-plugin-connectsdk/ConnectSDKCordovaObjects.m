//
//  ConnectSDKCordovaObjects.m
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

#import "ConnectSDKCordovaObjects.h"

@implementation JSObjectWrapper

static int nextObjectId = 0;

- (instancetype) initWithPlugin:(ConnectSDKCordova*)plugin
{
    self = [super init];
    
    if (self) {
        self.plugin = plugin;
        self.objectId = [NSString stringWithFormat:@"object_%d", ++nextObjectId];
        self.callbackId = nil;
    }
    
    return self;
}

// send [eventName, data]
- (void) sendEvent:(NSString*)event withObject:obj
{
    if (self.callbackId) {
        NSArray* payload = obj ? @[event, obj] : @[event];
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:payload];
        [result setKeepCallbackAsBool:YES];
        [self.plugin.commandDelegate sendPluginResult:result callbackId:self.callbackId];
    }
}

- (void) cleanup
{
}

@end

@implementation WebAppSessionWrapper

- (instancetype) initWithPlugin:(ConnectSDKCordova*)plugin session:(WebAppSession*)session
{
    self = [super initWithPlugin:plugin];
    
    if (self) {
        self.session = session;
    }
    
    return self;
}

- (void) webAppSession:(WebAppSession *)webAppSession didReceiveMessage:(id)message
{
    [self sendEvent:@"message" withObject:message];
}

- (void) webAppSessionDidDisconnect:(WebAppSession *)webAppSession
{
    [self sendEvent:@"disconnect" withObject:nil];
}

- (void) cleanup
{
    self.session.delegate = nil;
    self.session = nil;
    
    [super cleanup];
}

@end

@implementation MediaControlWrapper

- (instancetype) initWithPlugin:(ConnectSDKCordova*)plugin mediaControl:(id<MediaControl>)mediaControl
{
    self = [super initWithPlugin:plugin];
    
    if (self) {
        self.mediaControl = mediaControl;
    }

    return self;
}

@end

@implementation PlaylistControlWrapper

- (instancetype)initWithPlugin:(ConnectSDKCordova *)plugin
               playlistControl:(id <PlayListControl>)playlistControl {
    self = [super initWithPlugin:plugin];
    _playlistControl = playlistControl;
    return self;
}

@end
