//
//  FWChromecast.m
//  HelloCordova
//
//  Created by Franz Wilding on 16.11.15.
//
//

#import <Cordova/CDV.h>
#import <Foundation/Foundation.h>
#import <GoogleCast/GoogleCast.h>
#import "FWChromecast.h"
#import "DeviceScannerDelegate.h"
#import "SelectDeviceDelegate.h"
#import "MediaChannelDelegate.h"

@implementation FWChromecast : CDVPlugin

- (void)scanForDevices:(CDVInvokedUrlCommand*)command
{
    self.receiverAppId = [command.arguments objectAtIndex:0];
    self.deviceScannerDelegate = [[DeviceScannerDelegate alloc] initWithCommandDelegate:self.commandDelegate
                                                                        andCallbackId:command.callbackId];
    [self.deviceScannerDelegate startScanningForAppId:self.receiverAppId];
}

- (void)selectDevice:(CDVInvokedUrlCommand*)command
{
    NSString* deviceId = [command.arguments objectAtIndex:0];
    self.selectDeviceDelegate = [[SelectDeviceDelegate alloc] initWithCommandDelegate:self.commandDelegate
                                                                      andCallbackId:command.callbackId];
    [self.selectDeviceDelegate selectDevice:[self.deviceScannerDelegate findDevice:deviceId]];
}

- (void)launchApplication:(CDVInvokedUrlCommand*)command
{
    if(self.selectDeviceDelegate == nil || self.receiverAppId == nil) {
        CDVPluginResult*pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_INVALID_ACTION messageAsString:@"In order to launch an application you need to select a device first."];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    [self.selectDeviceDelegate launchApplication:self.receiverAppId];
}

- (void)joinApplication:(CDVInvokedUrlCommand*)command
{
    if(self.selectDeviceDelegate == nil || self.receiverAppId == nil) {
        CDVPluginResult*pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_INVALID_ACTION messageAsString:@"In order to launch an application you need to select a device first."];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    [self.selectDeviceDelegate joinApplication:self.receiverAppId];
}

- (void)disconnect
{
    if(self.selectDeviceDelegate != nil) {
        [self.selectDeviceDelegate disconnect];
    }
}

- (void)startMediaChannel:(CDVInvokedUrlCommand*)command
{
    GCKMediaControlChannel *mediaChannel = [[GCKMediaControlChannel alloc] init];
    self.mediaChannelDelegate = [[MediaChannelDelegate alloc] initWithCommandDelegate:self.commandDelegate andCallbackId:command.callbackId];
    mediaChannel.delegate = self.mediaChannelDelegate;
    self.mediaChannelDelegate.channel = mediaChannel;
    [self.selectDeviceDelegate addChannel:mediaChannel];

}


- (void)loadMedia:(CDVInvokedUrlCommand*)command {

    if(self.mediaChannelDelegate == nil) {
        [self startMediaChannel:command];
    }

    NSString *title = [command.arguments objectAtIndex:0];
    NSString *mediaUrl = [command.arguments objectAtIndex:1];
    NSString *contentType = [command.arguments objectAtIndex:2];
    NSString *subtitle = [command.arguments objectAtIndex:3];

    GCKMediaMetadata *metadata = [[GCKMediaMetadata alloc] init];
    [metadata setString:title forKey:kGCKMetadataKeyTitle];
    [metadata setString:subtitle forKey:kGCKMetadataKeySubtitle];

    [self.mediaChannelDelegate loadMedia: [[GCKMediaInformation alloc]
                                           initWithContentID:mediaUrl
                                           streamType:GCKMediaStreamTypeNone
                                           contentType:contentType
                                           metadata:metadata
                                           streamDuration:0
                                           customData:nil]];
}

- (void)playMedia:(CDVInvokedUrlCommand*)command {
    if(self.mediaChannelDelegate == nil) {
        CDVPluginResult*pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_INVALID_ACTION messageAsString:@"In order to play a media item you need to load it first."];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    [self.mediaChannelDelegate play];
}

- (void)pauseMedia:(CDVInvokedUrlCommand*)command {
    if(self.mediaChannelDelegate == nil) {
        CDVPluginResult*pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_INVALID_ACTION messageAsString:@"In order to pause a media item you need to load it first."];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    [self.mediaChannelDelegate pause];
}

- (void)stopMedia:(CDVInvokedUrlCommand*)command {
    if(self.mediaChannelDelegate == nil) {
        CDVPluginResult*pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_INVALID_ACTION messageAsString:@"In order to stop a media item you need to load it first."];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    [self.mediaChannelDelegate stop];
}

- (void)muteMedia:(CDVInvokedUrlCommand*)command {
    bool mute = [[command.arguments objectAtIndex:0] boolValue];
    if(self.mediaChannelDelegate == nil) {
        CDVPluginResult*pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_INVALID_ACTION messageAsString:@"In order to mute a media item you need to load it first."];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    [self.mediaChannelDelegate mute:mute];
}

- (void)setVolumeForMedia:(CDVInvokedUrlCommand*)command {
    float volume = [[command.arguments objectAtIndex:0] floatValue];
    if(self.mediaChannelDelegate == nil) {
        CDVPluginResult*pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_INVALID_ACTION messageAsString:@"In order to set the volume you need to load it first."];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    [self.mediaChannelDelegate setVolume:volume];
}

- (void)seekMedia:(CDVInvokedUrlCommand*)command {
    NSTimeInterval time = [[command.arguments objectAtIndex:0] doubleValue];
    if(self.mediaChannelDelegate == nil) {
        CDVPluginResult*pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_INVALID_ACTION messageAsString:@"In order to seek a media item you need to load it first."];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    [self.mediaChannelDelegate seek:time];
}

@end
