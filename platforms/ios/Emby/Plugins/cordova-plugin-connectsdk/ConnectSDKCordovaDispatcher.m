//
//  ConnectSDKCordovaDispatcher.m
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

#import <Cordova/CDV.h>
#import <ConnectSDK/ConnectSDK.h>
#import <objc/message.h>
#import <ConnectSDK/WebOSTVService.h>
#import "ConnectSDKCordovaDispatcher.h"
#import "ConnectSDKCordovaObjects.h"

static id orNull (id obj)
{
    return obj ? obj : [NSNull null];
}

@interface JSCommand : NSObject
@property (nonatomic, strong) ConnectSDKCordova* plugin;
@property (nonatomic, strong) JSCommandDispatcher* dispatcher;

@property (nonatomic, strong) NSDictionary* args;
@property (nonatomic) BOOL isSubscription;
@property (nonatomic) BOOL cancelled;
@property (nonatomic, strong) ServiceSubscription* subscription;
@property (nonatomic, strong) NSString* commandId;
@property (nonatomic, strong) NSString* callbackId;
@end

#pragma mark - JSCommand

@implementation JSCommand

+ (JSCommand*) command:(CDVInvokedUrlCommand*)cdvCommand dispatcher:(JSCommandDispatcher*)dispatcher plugin:(ConnectSDKCordova*)plugin
{
    JSCommand* command = [JSCommand new];
    command.dispatcher = dispatcher;
    command.plugin = plugin;
    command.callbackId = [cdvCommand callbackId];
    command.args = [cdvCommand argumentAtIndex: 4];
    command.isSubscription = [[cdvCommand argumentAtIndex:5] boolValue];
    
    return command;
}

- (void) cancel
{
    if (self.cancelled) return;
    self.cancelled = YES;
    
    if (self.subscription) {
        [self.subscription unsubscribe];
    }
    
    // Remove from dispatcher
    [self.dispatcher cancelCommand:self.commandId];
}

- (void) complete
{
    if (!self.isSubscription) {
        // Commands are removed once they're sent (except for subscriptions)
        [self cancel];
    }
}

- (void) sendSuccessResult:(CDVPluginResult*)result
{
    if (self.cancelled) return;
    
    if (self.isSubscription) {
        [result setKeepCallbackAsBool:YES];
    }
    
    [_plugin.commandDelegate sendPluginResult:result callbackId:_callbackId];
    [self complete];
}

- (void) sendFailureResult:(CDVPluginResult*)result
{
    if (self.cancelled) return;
    
    [_plugin.commandDelegate sendPluginResult:result callbackId:_callbackId];
    [self complete];

}

- (void) sendErrorMessage:(NSString*)message
{
    if (self.cancelled) return;
    
    NSDictionary* errorObj = @{@"message": message};
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:errorObj];
    [self sendFailureResult:result];
}

- (void) sendSuccess:(NSObject*)obj
{
    NSMutableArray* args = [NSMutableArray arrayWithObject:@"success"];
    
    if (obj) {
        [args addObject:obj];
    }
    
    [self sendSuccessResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:args]];
}

- (void) sendSuccessWithArray:(NSArray*)results
{
    NSMutableArray* args = [NSMutableArray arrayWithObject:@"success"];
    
    if (results) {
        [args addObjectsFromArray:results];
    }
    
    [self sendSuccessResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:args]];
}

- (SuccessBlock) success
{
    return ^(id obj) {
        // FIXME: convert obj using some kind of standard toDictionary call that will need to be implemented on each object
        [self sendSuccess:nil];
    };
}

- (AppLaunchSuccessBlock) appLaunchSuccess
{
    return ^(LaunchSession* session) {
        NSDictionary* result = [session toJSONObject];
        
        [self sendSuccess:result];
    };
}

- (MediaPlayerSuccessBlock) mediaLaunchSuccess
{
    return ^(MediaLaunchObject *mediaLaunchObject) {
        NSMutableDictionary* mediaControlObj = [NSMutableDictionary dictionary]; // TODO media control details
        
        MediaControlWrapper* controlWrapper = [[MediaControlWrapper alloc] initWithPlugin:self.plugin
                                                                             mediaControl:mediaLaunchObject.mediaControl];
        [self.plugin addObjectWrapper:controlWrapper];
        
        mediaControlObj[@"objectId"] = controlWrapper.objectId;

        NSDictionary *playlistControlObj;
        if (mediaLaunchObject.playListControl) {
            PlaylistControlWrapper *playlistControlWrapper = [[PlaylistControlWrapper alloc]
                initWithPlugin:self.plugin
               playlistControl:mediaLaunchObject.playListControl];
            [self.plugin addObjectWrapper:playlistControlWrapper];
            playlistControlObj = @{@"objectId": playlistControlWrapper.objectId};
        }

        NSArray* result = @[
            [mediaLaunchObject.session toJSONObject],
            mediaControlObj,
            orNull(playlistControlObj),
        ];
        
        [self sendSuccessWithArray:result];
    };
}

- (WebAppLaunchSuccessBlock) webAppLaunchSuccess
{
    return ^(WebAppSession* session) {
        NSMutableDictionary* result = [NSMutableDictionary dictionaryWithDictionary:[session toJSONObject]];
        JSObjectWrapper* wrapper = [[WebAppSessionWrapper alloc] initWithPlugin:self.plugin session:session];
        [self.plugin addObjectWrapper:wrapper];
        
        result[@"objectId"] = wrapper.objectId;
        
        [self sendSuccess:result];
    };
}

- (AppListSuccessBlock) appListSuccess
{
    return ^(NSArray* appList) {
        NSMutableArray* results = [NSMutableArray array];
        
        for(AppInfo* info in appList) {
            NSDictionary* appInfoObj = @{@"id": info.id, @"name": info.name};
            
            [results addObject:appInfoObj];
        }
        
        [self sendSuccess:results];
    };
}

- (NSDictionary*) channelInfoAsDict:(ChannelInfo*)info
{
    return @{
        @"id": orNull(info.id),
        @"name": orNull(info.name),
        @"number": orNull(info.number),
        @"majorNumber": [NSNumber numberWithInt:info.majorNumber],
        @"minorNumber": [NSNumber numberWithInt:info.minorNumber]
    };
}

- (CurrentChannelSuccessBlock) channelInfoSuccess
{
    return ^(ChannelInfo* info) {
        [self sendSuccess:[self channelInfoAsDict:info]];
    };
}

- (ChannelListSuccessBlock) channelListSuccess
{
    return ^(NSArray* list) {
        NSMutableArray* results = [NSMutableArray array];
        
        for(ChannelInfo* info in list) {
            [results addObject:[self channelInfoAsDict:info]];
        }
        
        [self sendSuccess:results];
    };
}

- (MediaPlayStateSuccessBlock) playStateSuccess
{
    return ^(MediaControlPlayState state) {
        NSString* stateName = nil;
        
        switch (state) {
            case MediaControlPlayStateUnknown: stateName = @"unknown"; break;
            case MediaControlPlayStateIdle: stateName = @"idle"; break;
            case MediaControlPlayStatePlaying: stateName = @"playing"; break;
            case MediaControlPlayStatePaused: stateName = @"paused"; break;
            case MediaControlPlayStateBuffering: stateName = @"buffering"; break;
            case MediaControlPlayStateFinished: stateName = @"finished"; break;
        }
        
        [self sendSuccess:stateName];
    };
}

- (TextInputStatusInfoSuccessBlock) textInputStatusInfoSuccess
{
    return ^(TextInputStatusInfo* info) {
        NSMutableDictionary* result = [NSMutableDictionary dictionary];
        result[@"isVisible"] = [NSNumber numberWithBool:[info isVisible]];
        result[@"rawData"] = orNull([info rawData]);
        
        // FIXME add other properties
        
        [self sendSuccess:result];
    };
}

- (ExternalInputListSuccessBlock) externalInputListSuccess
{
    return ^(NSArray* list) {
        NSMutableArray* objs = [NSMutableArray array];
        
        for (ExternalInputInfo* info in list) {
            NSDictionary* obj = @{
                @"id": orNull(info.id),
                @"name": orNull(info.name),
                @"iconURL": orNull(info.iconURL.absoluteString)
            };
            
            [objs addObject:obj];
        }
        
        [self sendSuccess:objs];
    };
}

- (void (^)(double)) successWithDouble
{
    return ^(double value){
        [self sendSuccessWithArray:@[ [NSNumber numberWithDouble:value] ]];
    };
}

- (void (^)(float)) successWithFloat
{
    return ^(float value){
        [self sendSuccessWithArray:@[ [NSNumber numberWithFloat:value] ]];
    };
}

- (void (^)(BOOL)) successWithBool
{
    return ^(BOOL value){
        [self sendSuccessWithArray:@[ [NSNumber numberWithBool:value] ]];
    };
}

- (FailureBlock) failure
{
    return ^(NSError* error){
        if (self.cancelled) return;
        
        NSDictionary* errorObj = @{
            @"message": [error localizedDescription]
        };
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:errorObj];
        [self sendFailureResult:result];
    };
}

@end

#pragma mark - JSCommandDispatcher

@implementation JSCommandDispatcher {
    CDVPlugin* plugin;
    ConnectableDevice* device;
    NSMutableDictionary* requests; // NOTE: must be synchronized
}

+ (JSCommandDispatcher*) dispatcherWithPlugin:(CDVPlugin*)plugin device:(ConnectableDevice*)device
{
    JSCommandDispatcher* dispatcher = [JSCommandDispatcher new];
    dispatcher->device = device;
    dispatcher->plugin = plugin;
    dispatcher->requests = [NSMutableDictionary dictionary];
    return dispatcher;
}

- (void) dispatch:(CDVInvokedUrlCommand*)cdvCommand
{
    JSCommand* command = [JSCommand command:cdvCommand dispatcher:self plugin:(ConnectSDKCordova*)plugin];
    NSString* interfaceName = (NSString*)[cdvCommand argumentAtIndex:2];
    NSString* methodName = (NSString*)[cdvCommand argumentAtIndex:3];
    
    command.commandId = [cdvCommand argumentAtIndex:1];
    requests[command.commandId] = command;
    
    NSString* selectorName = [NSString stringWithFormat:@"%@_%@:", interfaceName, methodName, nil];
    
    SEL sel = NSSelectorFromString(selectorName);
    
    if (sel && [self respondsToSelector:sel]) {
        @try {
            NSMethodSignature* sig = [self methodSignatureForSelector:sel];
            id result = nil;
            
            if (sig.methodReturnLength > 0) { // FIXME verify type
                result = ((id(*)(id, SEL, id))objc_msgSend)(self, sel, command);
            } else {
                ((void(*)(id, SEL, id))objc_msgSend)(self, sel, command);
            }
            
            if (result && [result isMemberOfClass:[ServiceSubscription class]]) {
                command.subscription = (ServiceSubscription*)result;
            }
        }
        @catch (NSException* e) {
            NSString* errorString = [NSString stringWithFormat:@"Internal exception %@: %@", [e name], [e reason], nil];

            [command sendErrorMessage:errorString];
            
            NSLog(@"Exception while handling command %@:\n%@", selectorName, [e callStackSymbols]);
        }
    } else {
        NSLog(@"dispatch method %@ not implemented", selectorName);
        NSDictionary* errorObj = @{@"message": @"method not implemented"};
        
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_CLASS_NOT_FOUND_EXCEPTION messageAsDictionary:errorObj];
        [command sendFailureResult:result];
    }
}

- (void) cancelCommand:(NSString*)commandId
{
    @synchronized(self) {
        JSCommand* command = requests[commandId];
        
        if (command) {
            [requests removeObjectForKey:commandId];
        
            [command cancel];
        }
    }
}

#pragma mark - Launcher

- (void) launcher_launchApp:(JSCommand*)command
{
    NSString* appId = command.args[@"appId"];
    NSDictionary* params = command.args[@"params"];

    if (params) {
        AppInfo* appInfo = [AppInfo appInfoForId:appId];
        [device.launcher launchAppWithInfo:appInfo params:params success:command.success failure:command.failure];
    } else {
        [device.launcher launchApp:appId success:command.appLaunchSuccess failure:command.failure];
    }
}

- (void) launcher_closeApp:(JSCommand*)command
{
    NSString* appId = command.args[@"appId"];
    LaunchSession* launchSession = [LaunchSession launchSessionForAppId:appId];
    
    [device.launcher closeApp:launchSession success:command.success failure:command.failure];
}

- (void) launcher_launchAppStore:(JSCommand*)command
{
    NSString* appId = command.args[@"appId"];
    
    [device.launcher launchAppStore:appId success:command.appLaunchSuccess failure:command.failure];
}

- (void) launcher_launchBrowser:(JSCommand*)command
{
    NSString* urlString = command.args[@"url"];
    NSURL* url = nil;
    
    if (urlString) {
        url = [NSURL URLWithString:urlString];
    }
    
    [device.launcher launchBrowser:url success:command.appLaunchSuccess failure:command.failure];
}

- (void) launcher_launchHulu:(JSCommand*)command
{
    NSString* contentId = command.args[@"contentId"];
    [device.launcher launchHulu:contentId success:command.appLaunchSuccess failure:command.failure];
}

- (void) launcher_launchNetflix:(JSCommand*)command
{
    NSString* contentId = command.args[@"contentId"];
    [device.launcher launchNetflix:contentId success:command.appLaunchSuccess failure:command.failure];
}

- (void) launcher_launchYouTube:(JSCommand*)command
{
    NSString* contentId = command.args[@"contentId"];
    [device.launcher launchYouTube:contentId success:command.appLaunchSuccess failure:command.failure];
}

- (void) launcher_getAppList:(JSCommand*)command
{
    [device.launcher getAppListWithSuccess:command.appListSuccess failure:command.failure];
}

#pragma mark - MediaControl

- (void) mediaControl_play:(JSCommand*)command
{
    id<MediaControl> mediaControl = [self getMediaControl:command];
    if (!mediaControl) return;
    
    [mediaControl playWithSuccess:command.success failure:command.failure];
}

- (void) mediaControl_pause:(JSCommand*)command
{
    id<MediaControl> mediaControl = [self getMediaControl:command];
    if (!mediaControl) return;
    
    [mediaControl pauseWithSuccess:command.success failure:command.failure];
}

- (void) mediaControl_stop:(JSCommand*)command
{
    id<MediaControl> mediaControl = [self getMediaControl:command];
    if (!mediaControl) return;
    
    [mediaControl stopWithSuccess:command.success failure:command.failure];
}

- (void) mediaControl_rewind:(JSCommand*)command
{
    id<MediaControl> mediaControl = [self getMediaControl:command];
    if (!mediaControl) return;
    
    [mediaControl rewindWithSuccess:command.success failure:command.failure];
}

- (void) mediaControl_fastForward:(JSCommand*)command
{
    id<MediaControl> mediaControl = [self getMediaControl:command];
    if (!mediaControl) return;
    
    [mediaControl fastForwardWithSuccess:command.success failure:command.failure];
}

- (void) mediaControl_seek:(JSCommand*)command
{
    id<MediaControl> mediaControl = [self getMediaControl:command];
    if (!mediaControl) return;
    
    NSNumber* positionObj = (NSNumber*) command.args[@"position"];
    
    if (positionObj == nil) {
        [command sendErrorMessage:@"invalid position"];
    }
    
    NSTimeInterval position = [positionObj doubleValue];
    
    [mediaControl seek:position success:command.success failure:command.failure];
}

- (void) mediaControl_getDuration:(JSCommand*)command
{
    id<MediaControl> mediaControl = [self getMediaControl:command];
    if (!mediaControl) return;
    
    [mediaControl getDurationWithSuccess:command.successWithDouble failure:command.failure];
}

- (void) mediaControl_getPosition:(JSCommand*)command
{
    id<MediaControl> mediaControl = [self getMediaControl:command];
    if (!mediaControl) return;
    
    [mediaControl getPositionWithSuccess:command.successWithDouble failure:command.failure];
}

- (ServiceSubscription*) mediaControl_subscribePlayState:(JSCommand*)command
{
    id<MediaControl> mediaControl = [self getMediaControl:command];
    if (!mediaControl) return nil;
    
    return [mediaControl subscribePlayStateWithSuccess:command.playStateSuccess failure:command.failure];
}

#pragma mark - PlayListControl

- (void)playlistControl_next:(JSCommand *)command {
    id<PlayListControl> playlistControl = [self playlistControlFromCommand:command];
    if (playlistControl) {
        [playlistControl playNextWithSuccess:command.success
                                     failure:command.failure];
    }
}

- (void)playlistControl_previous:(JSCommand *)command {
    id<PlayListControl> playlistControl = [self playlistControlFromCommand:command];
    if (playlistControl) {
        [playlistControl playPreviousWithSuccess:command.success
                                         failure:command.failure];
    }
}

- (void)playlistControl_jumpToTrack:(JSCommand *)command {
    id<PlayListControl> playlistControl = [self playlistControlFromCommand:command];
    if (playlistControl) {
        NSNumber *indexNumber = command.args[@"index"];
        if (indexNumber) {
            [playlistControl jumpToTrackWithIndex:[indexNumber integerValue]
                                          success:command.success
                                          failure:command.failure];
        } else {
            [command sendErrorMessage:@"index cannot be nil"];
        }
    }
}

#pragma mark - MediaPlayer

- (void) mediaPlayer_displayImage:(JSCommand*)command
{
    [self displayMediaCommon:command type:@"image"];
}

- (void) mediaPlayer_playMedia:(JSCommand*)command
{
    [self displayMediaCommon:command type:@"video"];
}

#pragma mark - VolumeControl

- (void) volumeControl_getVolume:(JSCommand*)command
{
    [device.volumeControl getVolumeWithSuccess:command.successWithFloat failure:command.failure];
}

- (void) volumeControl_getMute:(JSCommand*)command
{
    [device.volumeControl getMuteWithSuccess:command.successWithBool failure:command.failure];
}

- (void) volumeControl_volumeUp:(JSCommand*)command
{
    [device.volumeControl volumeUpWithSuccess:command.success failure:command.failure];
}

- (void) volumeControl_volumeDown:(JSCommand*)command
{
    [device.volumeControl volumeDownWithSuccess:command.success failure:command.failure];
}

- (void) volumeControl_setVolume:(JSCommand*)command
{
    float volume = [(NSNumber*)command.args[@"volume"] floatValue];
    [device.volumeControl setVolume:volume success:command.success failure:command.failure];
}

- (void) volumeControl_setMute:(JSCommand*)command
{
    BOOL mute = [(NSNumber*)command.args[@"mute"] boolValue];
    [device.volumeControl setMute:mute success:command.success failure:command.failure];
}

- (ServiceSubscription*) volumeControl_subscribeVolume:(JSCommand*)command
{
    return [device.volumeControl subscribeVolumeWithSuccess:command.successWithFloat failure:command.failure];
}

- (ServiceSubscription*) volumeControl_subscribeMute:(JSCommand*)command
{
    return [device.volumeControl subscribeMuteWithSuccess:command.successWithBool failure:command.failure];
}

#pragma mark - toastControl

- (void) toastControl_showToast:(JSCommand*)command
{
    NSString* message = command.args[@"message"];
    NSDictionary* options = command.args[@"options"];
    
    NSString* iconData = nil;
    NSString* iconExtension = nil;
    
    if (options) {
        iconData = options[@"iconData"];
        iconExtension = options[@"iconExtension"];
    }
    
    if (iconData) {
        [device.toastControl showToast:message iconData:iconData iconExtension:iconExtension success:command.success failure:command.failure];
    } else {
        [device.toastControl showToast:message success:command.success failure:command.failure];
    }
}

- (void) toastControl_showClickableToast:(JSCommand*)command
{
    NSString* message = command.args[@"message"];
    NSDictionary* options = command.args[@"options"];
    
    NSString* iconData = nil;
    NSString* iconExtension = nil;
    NSURL* url = nil;
    AppInfo* appInfo = nil;
    NSDictionary* appParams = nil;
    
    if (options) {
        NSString* urlString = options[@"url"];
        NSString* appId = options[@"appId"];
        
        iconData = options[@"iconData"];
        iconExtension = options[@"iconExtension"];
        
        url = urlString ? [NSURL URLWithString:urlString] : nil;
        
        if (appId) {
            appInfo = [AppInfo appInfoForId:appId];
        }
        
        appParams = options[@"appParams"];
    }
    
    if (url) {
        if (iconData) {
            [device.toastControl showClickableToast:message URL:url iconData:iconData iconExtension:iconExtension success:command.success failure:command.failure];
        } else {
            [device.toastControl showClickableToast:message URL:url success:command.success failure:command.failure];
        }
    } else if (appInfo) {
        if (iconData) {
            [device.toastControl showClickableToast:message appInfo:appInfo params:appParams iconData:iconData iconExtension:iconExtension success:command.success failure:command.failure];
        } else {
            [device.toastControl showClickableToast:message appInfo:appInfo params:appParams success:command.success failure:command.failure];

        }
    } else {
        [command sendErrorMessage:@"need url or appId to show clickable toast"];
    }
    
}

#pragma mark - TVControl

- (void) TVControl_channelUp:(JSCommand*)command
{
    [device.tvControl channelUpWithSuccess:command.success failure:command.failure];
}

- (void) TVControl_channelDown:(JSCommand*)command
{
    [device.tvControl channelDownWithSuccess:command.success failure:command.failure];
}

- (void) TVControl_getChannelList:(JSCommand*)command
{
    [device.tvControl getChannelListWithSuccess:command.channelListSuccess failure:command.failure];
}

- (void) TVControl_getCurrentChannel:(JSCommand*)command
{
    [device.tvControl getCurrentChannelWithSuccess:command.channelInfoSuccess failure:command.failure];
}

- (ServiceSubscription*) TVControl_subscribeCurrentChannel:(JSCommand*)command
{
    return [device.tvControl subscribeCurrentChannelWithSuccess:command.channelInfoSuccess failure:command.failure];
}

- (void) TVControl_setChannel:(JSCommand*)command
{
    NSDictionary* channelInfoObj = command.args[@"channelInfo"];
    
    // TODO: move parsing
    ChannelInfo* channelInfo = [ChannelInfo new];
    channelInfo.id = channelInfoObj[@"id"];
    channelInfo.number = channelInfoObj[@"number"];
    
    [device.tvControl setChannel:channelInfo success:command.success failure:command.failure];
}

#pragma mark - ExternalInputControl

- (void) externalInputControl_getExternalInputList:(JSCommand*)command
{
    [device.externalInputControl getExternalInputListWithSuccess:command.externalInputListSuccess failure:command.failure];
}

- (void) externalInputControl_setExternalInput:(JSCommand*)command
{
    NSDictionary* infoObj = command.args[@"externalInputInfo"];
    
    // TODO: move parsing
    ExternalInputInfo* externalInputInfo = [ExternalInputInfo new];
    externalInputInfo.id = infoObj[@"id"];
    externalInputInfo.name = infoObj[@"name"];
    
    [device.externalInputControl setExternalInput:externalInputInfo success:command.success failure:command.failure];
}

- (void)externalInputControl_showExternalInputPicker:(JSCommand *)command {
    [device.externalInputControl launchInputPickerWithSuccess:command.appLaunchSuccess
                                                      failure:command.failure];
}

#pragma mark - WebAppLauncher

- (void) webAppLauncher_launchWebApp:(JSCommand*)command
{
    if (command.args[@"params"] && [device hasCapability:kWebAppLauncherLaunchParams])
        [device.webAppLauncher launchWebApp:command.args[@"webAppId"] params:command.args[@"params"] success:command.webAppLaunchSuccess failure:command.failure];
    else
        [device.webAppLauncher launchWebApp:command.args[@"webAppId"] success:command.webAppLaunchSuccess failure:command.failure];
}

- (void) webAppLauncher_joinWebApp:(JSCommand*)command
{
    [device.webAppLauncher joinWebAppWithId:command.args[@"webAppId"] success:command.webAppLaunchSuccess failure:command.failure];
}

- (void)webAppLauncher_pinWebApp:(JSCommand *)command {
    [device.webAppLauncher pinWebApp:command.args[@"webAppId"]
                             success:command.success
                             failure:command.failure];
}

- (void)webAppLauncher_unPinWebApp:(JSCommand *)command {
    [device.webAppLauncher unPinWebApp:command.args[@"webAppId"]
                               success:command.success
                               failure:command.failure];
}

- (void)webAppLauncher_isWebAppPinned:(JSCommand *)command {
    [device.webAppLauncher isWebAppPinned:command.args[@"webAppId"]
                                  success:command.successWithBool
                                  failure:command.failure];
}

- (void)webAppLauncher_subscribeIsWebAppPinned:(JSCommand *)command {
    [device.webAppLauncher subscribeIsWebAppPinned:command.args[@"webAppId"]
                                           success:command.successWithBool
                                           failure:command.failure];
}

#pragma mark - KeyControl

- (void) keyControl_up:(JSCommand*)command
{
    [device.keyControl upWithSuccess:command.success failure:command.failure];
}

- (void) keyControl_down:(JSCommand*)command
{
    [device.keyControl downWithSuccess:command.success failure:command.failure];
}

- (void) keyControl_left:(JSCommand*)command
{
    [device.keyControl leftWithSuccess:command.success failure:command.failure];
}

- (void) keyControl_right:(JSCommand*)command
{
    [device.keyControl rightWithSuccess:command.success failure:command.failure];
}

- (void) keyControl_ok:(JSCommand*)command
{
    [device.keyControl okWithSuccess:command.success failure:command.failure];
}

- (void) keyControl_back:(JSCommand*)command
{
    [device.keyControl backWithSuccess:command.success failure:command.failure];
}

- (void) keyControl_home:(JSCommand*)command
{
    [device.keyControl homeWithSuccess:command.success failure:command.failure];
}

- (void) keyControl_sendKeyCode:(JSCommand*)command
{
    NSNumber* keyCodeObj = command.args[@"keyCode"];
    
    if (keyCodeObj == nil) {
        [command sendErrorMessage:@"invalid key code"];
    }
    
    NSUInteger keyCode = [keyCodeObj unsignedIntegerValue];
    [device.keyControl sendKeyCode:keyCode success:command.success failure:command.failure];
}

#pragma mark - MouseControl

- (void) mouseControl_connectMouse:(JSCommand*)command
{
    [device.mouseControl connectMouseWithSuccess:command.success failure:command.failure];
}

- (void) mouseControl_disconnectMouse:(JSCommand*)command
{
    [device.mouseControl disconnectMouse];
}

- (void) mouseControl_move:(JSCommand*)command
{
    double dx = [(NSNumber*)command.args[@"dx"] doubleValue];
    double dy = [(NSNumber*)command.args[@"dy"] doubleValue];
    [device.mouseControl move:CGVectorMake(dx, dy) success:command.success failure:command.failure];
}

- (void) mouseControl_scroll:(JSCommand*)command
{
    double dx = [(NSNumber*)command.args[@"dx"] doubleValue];
    double dy = [(NSNumber*)command.args[@"dy"] doubleValue];
    [device.mouseControl scroll:CGVectorMake(dx, dy) success:command.success failure:command.failure];
}

- (void) mouseControl_click:(JSCommand*)command
{
    [device.mouseControl clickWithSuccess:command.success failure:command.failure];
}

#pragma mark - TextInputControl

- (ServiceSubscription*) textInputControl_subscribeTextInputStatus:(JSCommand*)command
{
    return [device.textInputControl subscribeTextInputStatusWithSuccess:command.textInputStatusInfoSuccess failure:command.failure];
}

- (void) textInputControl_sendText:(JSCommand*)command
{
    NSString* text = command.args[@"input"];
    [device.textInputControl sendText:text success:command.success failure:command.failure];
}

- (void) textInputControl_sendEnter:(JSCommand*)command
{
    [device.textInputControl sendEnterWithSuccess:command.success failure:command.failure];
}

- (void) textInputControl_sendDelete:(JSCommand*)command
{
    [device.textInputControl sendDeleteWithSuccess:command.success failure:command.failure];
}

#pragma mark - WebAppSession

- (void) webAppSession_connect:(JSCommand*)command
{
    NSString* objectId = command.args[@"objectId"];
    
    WebAppSessionWrapper* wrapper = [command.plugin getObjectWrapper:objectId];
    WebAppSession* session = wrapper.session;
    
    [session setDelegate:wrapper];
    [session connectWithSuccess:command.success failure:command.failure];
}

- (void) webAppSession_disconnect:(JSCommand*)command
{
    NSString* objectId = command.args[@"objectId"];
    
    WebAppSessionWrapper* wrapper = [command.plugin getObjectWrapper:objectId];
    WebAppSession* session = wrapper.session;
    
    [session disconnectFromWebApp];
    
    [command sendSuccess:nil];
}

- (void) webAppSession_sendText:(JSCommand*)command
{
    NSString* objectId = command.args[@"objectId"];
    NSString* text = command.args[@"text"];
    
    WebAppSessionWrapper* wrapper = [command.plugin getObjectWrapper:objectId];
    WebAppSession* session = wrapper.session;

    [session sendText:text success:command.success failure:command.failure];
}

- (void) webAppSession_sendJSON:(JSCommand*)command
{
    NSString* objectId = command.args[@"objectId"];
    NSDictionary* dict = command.args[@"jsonObject"];
    
    WebAppSessionWrapper* wrapper = [command.plugin getObjectWrapper:objectId];
    WebAppSession* session = wrapper.session;
    
    [session sendJSON:dict success:command.success failure:command.failure];
}

#pragma mark - Service methods

- (void) webOSTVService_connectToApp:(JSCommand*)command
{
    WebOSTVService* service = (WebOSTVService*)[device serviceWithName:kConnectSDKWebOSTVServiceId];
    
    NSString* appId = command.args[@"appId"];
    
    if (service) {
        [service connectToApp:appId success:command.webAppLaunchSuccess failure:command.failure];
    } else {
        [command sendErrorMessage:@"WebOSTVService not available on this device"];
    }
}

- (void) webOSTVService_joinApp:(JSCommand*)command
{
    WebOSTVService* service = (WebOSTVService*)[device serviceWithName:kConnectSDKWebOSTVServiceId];
    
    NSString* appId = command.args[@"appId"];
    
    if (service) {
        [service joinApp:appId success:command.webAppLaunchSuccess failure:command.failure];
    } else {
        [command sendErrorMessage:@"WebOSTVService not available on this device"];
    }
}

#pragma mark - Special CORDOVAPLUGIN interface for JS-specific commands

- (void) CORDOVAPLUGIN_closeLaunchSession:(JSCommand*)command
{
    NSDictionary* info = command.args[@"launchSession"];
    NSString* serviceName = info[@"serviceName"];
    
    LaunchSession* session = [[LaunchSession new] initWithJSONObject:info];;
    session.service = [device serviceWithName:serviceName];
    
    [session closeWithSuccess:command.success failure:command.failure];
}

#pragma mark - Dispatcher helper methods

- (id<MediaControl>) getMediaControl:(JSCommand*)command
{
    NSString* objectId = command.args[@"objectId"];
    
    if (objectId) {
        MediaControlWrapper* wrapper = [command.plugin getObjectWrapper:objectId];
    
        if (wrapper) {
            return wrapper.mediaControl;
        } else {
            [command sendErrorMessage:@"invalid MediaControl object"];
            return nil;
        }
    } else {
        return device.mediaControl;
    }
}

- (id<PlayListControl>) playlistControlFromCommand:(JSCommand*)command
{
    NSString* objectId = command.args[@"objectId"];
    if (objectId) {
        PlaylistControlWrapper *wrapper = [command.plugin getObjectWrapper:objectId];
        if (wrapper) {
            return wrapper.playlistControl;
        }

        [command sendErrorMessage:@"invalid PlaylistControl object"];
    } else {
        [command sendErrorMessage:@"PlaylistControl not found for device"];
    }

    return nil;
}

- (AppInfo*) parseAppInfo:(NSObject*)obj
{
    if ([obj isKindOfClass:[NSDictionary class]]) {
        NSDictionary* dict = (NSDictionary*) obj;
        
        AppInfo* appInfo = [AppInfo new];
        [appInfo setId:dict[@"appId"]];
        
        return appInfo;
    } else if ([obj isKindOfClass:[NSString class]]) {
        NSString* string = (NSString*) obj;
        
        AppInfo* appInfo = [AppInfo new];
        [appInfo setId:string];
        
        return appInfo;
    } else {
        return nil;
    }
}

- (void) displayMediaCommon:(JSCommand*)command type:(NSString*)type
{
    MediaInfo *mediaInfo = [[MediaInfo alloc]
        initWithURL:[NSURL URLWithString:command.args[@"url"]]
           mimeType:command.args[@"mimeType"]];
    BOOL shouldLoop = NO;

    NSDictionary* options = command.args[@"options"];
    if (options) {
        mediaInfo.title = options[@"title"];
        mediaInfo.description = options[@"description"];

        ImageInfo *imageInfo = [[ImageInfo alloc]
            initWithURL:[NSURL URLWithString:options[@"iconUrl"]]
                   type:ImageTypeAlbumArt];
        [mediaInfo addImage:imageInfo];

        if (options[@"shouldLoop"] == [NSNumber numberWithBool:YES]) {
            shouldLoop = YES;
        }

        NSDictionary *subtitleDict = options[@"subtitles"];
        if (subtitleDict) {
            mediaInfo.subtitleInfo = [SubtitleInfo infoWithURL:[NSURL URLWithString:subtitleDict[@"url"]]
                                                      andBlock:^(SubtitleInfoBuilder *builder) {
                                                          builder.mimeType = subtitleDict[@"mimeType"];
                                                          builder.language = subtitleDict[@"language"];
                                                          builder.label = subtitleDict[@"label"];
                                                      }];
        }
    }

    if ([type isEqualToString:@"image"]) {
        [device.mediaPlayer displayImageWithMediaInfo:mediaInfo
                                              success:command.mediaLaunchSuccess
                                              failure:command.failure];
    } else {
        [device.mediaPlayer playMediaWithMediaInfo:mediaInfo
                                        shouldLoop:shouldLoop
                                           success:command.mediaLaunchSuccess
                                           failure:command.failure];
    }
}

@end


