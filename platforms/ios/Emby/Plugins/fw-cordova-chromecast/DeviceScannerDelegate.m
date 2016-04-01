//
//  DeviceDelegate.m
//  HelloCordova
//
//  Created by Franz Wilding on 18.11.15.
//
//

#import <Foundation/Foundation.h>
#import "DeviceScannerDelegate.h"
#import <GoogleCast/GoogleCast.h>

@interface DeviceScannerDelegate() <GCKDeviceScannerListener> {
}
@end

@implementation DeviceScannerDelegate

- (void)startScanningForAppId:(NSString*) appId {
    
    // Establish filter criteria.
    GCKFilterCriteria *filterCriteria = [GCKFilterCriteria criteriaForAvailableApplicationWithID:appId];
    
    // Initialize device scanner.
    self.deviceScanner = [[GCKDeviceScanner alloc] initWithFilterCriteria:filterCriteria];
    //self.deviceScanner = [[GCKDeviceScanner alloc] init];
    [self.deviceScanner addListener:self];
    [self.deviceScanner startScan];
}

- (GCKDevice*)findDevice:(NSString*)deviceId {
    NSPredicate *predicate = [NSPredicate predicateWithFormat:@"deviceID == %@", deviceId];
    NSArray *filteredArray = [self.deviceScanner.devices filteredArrayUsingPredicate:predicate];
    if(filteredArray.count > 0) {
        return filteredArray.firstObject;
    } else {
        return nil;
    }
}

// [START device-scanner-listener]
#pragma mark - GCKDeviceScannerListener
- (void)deviceDidComeOnline:(GCKDevice *)device {
    NSDictionary *data = [[NSDictionary alloc] initWithObjectsAndKeys:
                          device.deviceID, @"id",
                          device.friendlyName, @"friendlyName",
                          device.ipAddress, @"ipAddress",
                          [[NSNumber alloc] initWithUnsignedInt:device.servicePort], @"servicePort", nil];
    [self sendResponse:data from:@"deviceDidComeOnline" andKeepItAlive:true];
}

- (void)deviceDidGoOffline:(GCKDevice *)device {
    NSDictionary *data = [[NSDictionary alloc] initWithObjectsAndKeys:
                          device.deviceID, @"id",
                          device.friendlyName, @"friendlyName",
                          device.ipAddress, @"ipAddress",
                          [[NSNumber alloc] initWithUnsignedInt:device.servicePort], @"servicePort", nil];
    [self sendResponse:data from:@"deviceDidGoOffline" andKeepItAlive:true];
}
// [END device-scanner-listener]

@end