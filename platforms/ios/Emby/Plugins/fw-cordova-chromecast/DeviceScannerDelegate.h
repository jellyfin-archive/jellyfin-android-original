//
//  NSObject_DeviceDelegate.h
//  HelloCordova
//
//  Created by Franz Wilding on 18.11.15.
//
//

#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import "CommandDelegate.h"
#import <GoogleCast/GoogleCast.h>


@interface DeviceScannerDelegate : CommandDelegate

- (void)startScanningForAppId:(NSString*) appId;
- (GCKDevice*)findDevice:(NSString*)deviceId;
@property(nonatomic, strong) GCKDeviceScanner* deviceScanner;

@end
