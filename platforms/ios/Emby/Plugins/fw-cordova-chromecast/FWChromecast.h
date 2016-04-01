//
//  FWChromecast.h
//  HelloCordova
//
//  Created by Franz Wilding on 16.11.15.
//
//

#ifndef FWChromecast_h
#define FWChromecast_h

#import <Cordova/CDV.h>
#import "DeviceScannerDelegate.h"
#import "SelectDeviceDelegate.h"
#import "MediaChannelDelegate.h"

@interface FWChromecast : CDVPlugin

- (void)scanForDevices:(CDVInvokedUrlCommand*)command;
- (void)selectDevice:(CDVInvokedUrlCommand*)command;
- (void)launchApplication:(CDVInvokedUrlCommand*)command;
- (void)joinApplication:(CDVInvokedUrlCommand*)command;
- (void)disconnect;
- (void)startMediaChannel:(CDVInvokedUrlCommand*)command;
- (void)loadMedia:(CDVInvokedUrlCommand*)command;
- (void)playMedia:(CDVInvokedUrlCommand*)command;
- (void)pauseMedia:(CDVInvokedUrlCommand*)command;
- (void)stopMedia:(CDVInvokedUrlCommand*)command;
- (void)muteMedia:(CDVInvokedUrlCommand*)command;
- (void)setVolumeForMedia:(CDVInvokedUrlCommand*)command;
- (void)seekMedia:(CDVInvokedUrlCommand*)command;
@property(nonatomic, strong) DeviceScannerDelegate* deviceScannerDelegate;
@property(nonatomic, strong) SelectDeviceDelegate* selectDeviceDelegate;
@property(nonatomic, strong) MediaChannelDelegate* mediaChannelDelegate;
@property(nonatomic, strong) NSString* receiverAppId;

@end


#endif /* FWChromecast_h */
