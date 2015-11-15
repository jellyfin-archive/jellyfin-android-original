//
//  ConnectSDKCordova.m
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
#import "ConnectSDKCordovaObjects.h"

#import "ConnectSDK/AirPlayService.h"
#import "ConnectSDK/CapabilityFilter.h"
#import "ConnectSDK/DeviceServiceDelegate.h"

#pragma mark - Helper types

// Used to associate data between cordova and sdk device
@interface JSDeviceState : NSObject

@property (nonatomic, strong) id device;
@property (nonatomic, strong) NSString *deviceId;
@property (nonatomic, strong) NSString *callbackId;
@property (nonatomic, strong) JSCommandDispatcher* dispatcher;

@end

@implementation JSDeviceState {
}

+ (JSDeviceState *) stateFromDevice:(ConnectableDevice*)device
{
    JSDeviceState *state = [JSDeviceState new];
    state.device = device;
    state.deviceId = [device id];
    state.callbackId = nil;
    
    return state;
}

@end

@interface ConnectSDKCordova ()

/// A @c DeviceServicePairingType value that is passed when displaying a device
/// picker and then automagically set to a selected device.
@property (nonatomic, strong, nullable) NSNumber /*<DeviceServicePairingType>*/ *automaticPairingTypeNumber;

@end

@implementation ConnectSDKCordova {
    DiscoveryManager* _discoveryManager;
    NSMapTable* _deviceStateById; // map device id to device
    NSMapTable* _deviceStateByDevice; // map device to device id
    
    NSMapTable* _objectWrappers;
    
    NSString* _discoveryCallbackId;
    NSString* _showPickerCallbackId;
}

#pragma mark - Setup methods

- (void) pluginInitialize
{
    _deviceStateById = [NSMapTable mapTableWithKeyOptions:NSMapTableStrongMemory valueOptions:NSMapTableStrongMemory];
    _deviceStateByDevice = [NSMapTable mapTableWithKeyOptions:NSMapTableStrongMemory valueOptions:NSMapTableStrongMemory];
    _objectWrappers = [NSMapTable mapTableWithKeyOptions:NSMapTableStrongMemory valueOptions:NSMapTableStrongMemory];
    _discoveryCallbackId = 0;
    _showPickerCallbackId = 0;
}

- (void) setupDiscovery:(NSDictionary*)config
{
    if (!_discoveryManager) {
        _discoveryManager = [DiscoveryManager sharedManager];
    }
    
    if (config) {
        NSString* pairingLevel = config[@"pairingLevel"];
        
        if (pairingLevel != nil) {
            if ([pairingLevel isEqualToString:@""] || [pairingLevel isEqualToString:@"off"]) {
                [_discoveryManager setPairingLevel:ConnectableDevicePairingLevelOff];
            } else if ([pairingLevel isEqualToString:@"on"]) {
                [_discoveryManager setPairingLevel:ConnectableDevicePairingLevelOn];
            }
        }
        
        NSString* airPlayServiceMode = config[@"airPlayServiceMode"];
        if (airPlayServiceMode) {
            if ([airPlayServiceMode isEqualToString:@"webapp"]) {
                [AirPlayService setAirPlayServiceMode:AirPlayServiceModeWebApp];
            } else if ([airPlayServiceMode isEqualToString:@"media"]) {
                [AirPlayService setAirPlayServiceMode:AirPlayServiceModeMedia];
            }
        }
        
        NSArray* filterObjs = config[@"capabilityFilters"];
        if (filterObjs) {
            NSMutableArray* capFilters = [NSMutableArray array];
            
            for (NSArray* filterArray in filterObjs) {
                CapabilityFilter* capFilter = [CapabilityFilter filterWithCapabilities:filterArray];
                [capFilters addObject:capFilter];
            }
            
            [_discoveryManager setCapabilityFilters:capFilters];
        }
    }
}

#pragma mark - Cordova command handlers

- (void) startDiscovery:(CDVInvokedUrlCommand*)command
{
    NSLog(@"starting discovery from cordova");
    
    NSDictionary* config = (NSDictionary *)[command argumentAtIndex:0];
    
    [self setupDiscovery:config];
    
    if (_discoveryCallbackId) {
        [self sendDiscoveryUpdate:nil withData:nil done:YES];
        _discoveryCallbackId = 0;
    }
    
    _discoveryCallbackId = [command callbackId];
    _discoveryManager.delegate = self;
    
    [_discoveryManager registerDefaultServices];
    [_discoveryManager startDiscovery];
    
    [self sendDiscoveryUpdate:@"startdiscovery" withData:nil done:NO];
}

- (void) stopDiscovery:(CDVInvokedUrlCommand*)command
{
    if (_discoveryManager) {
        [_discoveryManager stopDiscovery];
    }
    
    // note: using done:NO to keep callback because discovery could still be restarted by someone else
    [self sendDiscoveryUpdate:@"stopdiscovery" withData:nil done:NO];
}

- (void) setDiscoveryConfig:(CDVInvokedUrlCommand*)command
{
    NSDictionary* config = command.arguments[0];
    [self setupDiscovery:config];
}

- (void) pickDevice:(CDVInvokedUrlCommand*)command
{
    [self setupDiscovery:nil];
 
    _showPickerCallbackId = [command callbackId];
    
    BOOL popup = NO;
    NSDictionary* options = [command argumentAtIndex:0];
    
    if (options) {
        NSString* format = options[@"format"];
        
        if (format && [format isEqualToString:@"full"]) {
            popup = NO;
        } else if (format && [format isEqualToString:@"popup"]) {
            popup = YES;
        }

        NSString *pairingTypeString = options[@"pairingType"];
        self.automaticPairingTypeNumber = pairingTypeString ?
                [self parsePairingType:pairingTypeString] :
                nil;
    }
    
    DevicePicker *picker = [_discoveryManager devicePicker];
    picker.delegate = self;
    
    UIView *view = [self.viewController view];
    
    if (popup) {
        [picker showActionSheet:view];
    } else {
        [picker showPicker:view];
    }
}

- (void) closeDevicePicker:(CDVInvokedUrlCommand*)command
{
    // Not supported
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_INVALID_ACTION];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void) sendCommand:(CDVInvokedUrlCommand*)command
{
    //NSString* requestId = (NSString *)[command argumentAtIndex:0];
    //NSString* interfaceName = (NSString *)[command argumentAtIndex:1];
    //NSString* commandName = (NSString *)[command argumentAtIndex:2];
    //NSDictionary* args = (NSDictionary *)[command argumentAtIndex:3];
    
    NSString* deviceId = (NSString *)[command argumentAtIndex:0];
    
    JSDeviceState* deviceState = [self getDeviceStateById:deviceId];
    
    @synchronized(deviceState) {
        if (!deviceState.dispatcher) {
            [deviceState setDispatcher:[JSCommandDispatcher dispatcherWithPlugin:self device:deviceState.device]];
        }
        
        [deviceState.dispatcher dispatch:command];
    }
}

- (void) cancelCommand:(CDVInvokedUrlCommand*)command
{
    NSString* deviceId = command.arguments[0];
    NSString* commandId = command.arguments[1];
    
    JSDeviceState* deviceState = [self getDeviceStateById:deviceId];
    
    @synchronized(deviceState) {
        if (deviceState.dispatcher) {
            [deviceState.dispatcher cancelCommand:commandId];
        }
    }
    
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void) setCallbackId:(NSString*)callbackId forDeviceState:(JSDeviceState*)deviceState
{
    NSString* oldCallbackId = deviceState.callbackId;
    
    [deviceState setCallbackId:callbackId];
    
    if (oldCallbackId) {
        // cancel old callback
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
        [self.commandDelegate sendPluginResult:result callbackId:oldCallbackId];
    }
}

// Routes device events to this callback without connecting
- (void) setDeviceListener:(CDVInvokedUrlCommand*)command
{
    NSString* deviceId = (NSString *)[command argumentAtIndex:0];
    
    NSLog(@"setting listener for device %@", deviceId);
    
    JSDeviceState* state = [self getDeviceStateById:deviceId];
    
    if (state) {
        [self setCallbackId:command.callbackId forDeviceState:state];
        
        ConnectableDevice* device = [state device];
        device.delegate = self;
    }
}

- (void) connectDevice:(CDVInvokedUrlCommand*)command
{
    NSString* deviceId = (NSString *)[command argumentAtIndex:0];
    
    NSLog(@"connecting to device %@", deviceId);
    
    JSDeviceState* state = [self getDeviceStateById:deviceId];
    
    if (state) {
        [self setCallbackId:command.callbackId forDeviceState:state];
        
        ConnectableDevice* device = [state device];
        device.delegate = self;
        [device connect];
    }
}

- (void) disconnectDevice:(CDVInvokedUrlCommand*)command
{
    NSString* deviceId = (NSString *)[command argumentAtIndex:0];
    
    NSLog(@"disconnecting from device %@", deviceId);
    
    ConnectableDevice* device = [self getDeviceById:deviceId];

    if (device) {
        [device disconnect];
    }
}

- (void) setPairingType:(CDVInvokedUrlCommand*)command
{
    NSString* deviceId = (NSString *)[command argumentAtIndex:0];
    ConnectableDevice* device = [self getDeviceById:deviceId];

    NSString *pairingTypeString = [command argumentAtIndex:1];
    NSNumber *pairingTypeNumber = [self parsePairingType:pairingTypeString];

    if (device && pairingTypeNumber) {
        [self setPairingTypeNumber:pairingTypeNumber toDevice:device];
    }
}

- (void) acquireWrappedObject:(CDVInvokedUrlCommand*)command
{
    NSString* objectId = (NSString *)[command argumentAtIndex:0];
    
    JSObjectWrapper* wrapper = [self getObjectWrapper:objectId];
    if (wrapper) {
        wrapper.callbackId = command.callbackId;
    }
}

- (void) releaseWrappedObject:(CDVInvokedUrlCommand*)command
{
    NSString* objectId = (NSString *)[command argumentAtIndex:0];
    
    JSObjectWrapper* wrapper = [self getObjectWrapper:objectId];
    if (wrapper) {
        [self removeObjectWrapper:wrapper];
    }
}

#pragma mark - Helper methods

- (void) sendDiscoveryUpdate:(NSString*)event withDevice:(ConnectableDevice*)device
{
    [self sendDiscoveryUpdate:event withData:[NSDictionary dictionaryWithObject:[self deviceAsDict:device] forKey:@"device"] done:NO];
}

- (void) sendDiscoveryUpdate:(NSString*)event withData:(NSDictionary*)dict done:(BOOL)done
{
    if (!_discoveryCallbackId) {
        return;
    }
    
    done = FALSE;
    
    CDVPluginResult *result;
    if (event) {
        NSArray* array = dict ? @[event, dict]: @[event];
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:array];
    } else {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    }
    [result setKeepCallbackAsBool:(done ? NO : YES)];
    
    // This is safe to call from any thread
    [self.commandDelegate sendPluginResult:result callbackId:_discoveryCallbackId];
}

- (void) sendDeviceUpdate:(NSString*)event device:(ConnectableDevice*)device withData:(NSDictionary*)dict
{
    JSDeviceState* deviceState = [self getOrCreateDeviceState:device];
    
    if (!device || ![deviceState callbackId]) {
        return;
    }
    
    NSArray* array = dict ? @[event, dict] : @[event];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:array];
    [result setKeepCallbackAsBool:YES];
    
    // This is safe to call from any thread
    [self.commandDelegate sendPluginResult:result callbackId:deviceState.callbackId];
}

- (void) sendServiceUpdate:(NSString*)event device:(ConnectableDevice*)device service:(DeviceService*)service withData:(NSDictionary*)dict
{
    JSDeviceState* deviceState = [self getOrCreateDeviceState:device];
    
    if (!device || ![deviceState callbackId]) {
        return;
    }
    
    NSString* serviceName = @"";
    
    NSArray* array = dict ? @[event, serviceName, dict] : @[event, serviceName];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:array];
    [result setKeepCallbackAsBool:YES];
    
    // This is safe to call from any thread
    [self.commandDelegate sendPluginResult:result callbackId:deviceState.callbackId];
}

static id orNull (id obj)
{
    return obj ? obj : [NSNull null];
}

- (JSDeviceState*) getOrCreateDeviceState:(ConnectableDevice*)device
{
    @synchronized(self) {
        JSDeviceState *deviceState = (JSDeviceState*) [_deviceStateByDevice objectForKey:device];
        if (deviceState == nil) {
            deviceState = [JSDeviceState stateFromDevice:device];
            [_deviceStateByDevice setObject:deviceState forKey:device];
            [_deviceStateById setObject:deviceState forKey:[deviceState deviceId]];
        }
        return deviceState;
    }
}

- (JSDeviceState*) getDeviceStateById:(NSString*)deviceId
{
    @synchronized(self) {
        return [_deviceStateById objectForKey:deviceId];
    }
}

- (ConnectableDevice*) getDeviceById:(NSString*)deviceId
{
    return [[self getDeviceStateById:deviceId] device];
}

- (NSDictionary*) deviceAsDict:(ConnectableDevice*)device
{
    NSMutableArray* services = [NSMutableArray array];
    
    for (DeviceService* service in device.services) {
        NSDictionary* serviceDict = @{
            @"name": service.serviceName
        };
        
        [services addObject:serviceDict];
    }
    
    return @{
        @"deviceId": [[self getOrCreateDeviceState:device] deviceId],
        @"ipAddress": orNull(device.address),
        @"friendlyName": orNull(device.friendlyName),
        @"modelName": orNull(device.modelName),
        @"modelNumber": orNull(device.modelNumber),
        @"capabilities": [device capabilities],
        @"services": services
    };
}

- (NSNumber *)parsePairingType:(NSString *)typeString {
    // the PairingType values from `ConnectSDK.js`
    static NSDictionary *mapping;
    static dispatch_once_t mappingOnce;
    dispatch_once(&mappingOnce, ^{
        mapping = @{
                @"NONE": @(DeviceServicePairingTypeNone),
                @"FIRST_SCREEN": @(DeviceServicePairingTypeFirstScreen),
                @"PIN": @(DeviceServicePairingTypePinCode),
                @"MIXED": @(DeviceServicePairingTypeMixed),
                @"AIRPLAY_MIRRORING": @(DeviceServicePairingTypeAirPlayMirroring),
        };
    });

    NSNumber *typeNumber = mapping[typeString];
    NSAssert(typeNumber, @"Unknown pairing type string: %@", typeString);
    return typeNumber;
}

- (void)setPairingTypeNumber:(NSNumber *)pairingTypeNumber
                    toDevice:(ConnectableDevice *)device {
    DeviceServicePairingType type = (DeviceServicePairingType)
            [pairingTypeNumber unsignedIntegerValue];
    [device setPairingType:type];
}

#pragma mark - DiscoveryManager delegates

- (void) discoveryManager:(DiscoveryManager *)manager didFindDevice:(ConnectableDevice *)device
{
    [self getOrCreateDeviceState:device];
    
    [self sendDiscoveryUpdate:@"devicefound" withDevice:device];
}

- (void) discoveryManager:(DiscoveryManager *)manager didLoseDevice:(ConnectableDevice *)device
{
    JSDeviceState *deviceState = [self getOrCreateDeviceState:device];
    NSString* deviceId = [deviceState deviceId];
    
    [self sendDiscoveryUpdate:@"devicelost" withDevice:device];
    
    // Remove from maps; don't remove if device has event listener
    if (![deviceState callbackId]) {
        @synchronized(self) {
            [_deviceStateById removeObjectForKey:deviceId];
            [_deviceStateByDevice removeObjectForKey:device];
        }
    }
}

- (void) discoveryManager:(DiscoveryManager *)manager didUpdateDevice:(ConnectableDevice *)device
{
    [self sendDiscoveryUpdate:@"deviceupdated" withDevice:device];
}

- (void) discoveryManager:(DiscoveryManager *)manager didFailWithError:(NSError*)error
{
    if (_discoveryCallbackId) {
        NSString *errorString = error ? [error localizedDescription] : @"unknown error";
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorString] callbackId:_discoveryCallbackId];
    }
}

#pragma mark - DevicePicker delegates

- (void) devicePicker:(DevicePicker *)picker didSelectDevice:(ConnectableDevice *)device;
{
    if (_showPickerCallbackId) {
        if (self.automaticPairingTypeNumber) {
            [self setPairingTypeNumber:self.automaticPairingTypeNumber
                              toDevice:device];
        }

        NSDictionary* dict = [self deviceAsDict:device];
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dict] callbackId:_showPickerCallbackId];
        
        _showPickerCallbackId = 0;
    }
}

- (void) devicePicker:(DevicePicker *)picker didCancelWithError:(NSError*)error
{
    if (_showPickerCallbackId) {
        NSString* errorString = [error localizedDescription];
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorString] callbackId:_showPickerCallbackId];
        
        _showPickerCallbackId = 0;
    }
}

# pragma mark - ConnectableDevice delegates

- (void) connectableDeviceReady:(ConnectableDevice *)device
{
    [self sendDeviceUpdate:@"ready" device:device withData:nil];
}

- (void) connectableDeviceDisconnected:(ConnectableDevice *)device withError:(NSError *)error;
{
    [self sendDeviceUpdate:@"disconnect" device:device withData:nil];
}

- (void) connectableDevice:(ConnectableDevice *)device capabilitiesAdded:(NSArray *)added removed:(NSArray *)removed
{
    NSDictionary* data = @{@"added": added, @"removed": removed, @"reset": [NSNumber numberWithBool:FALSE]};
    
    [self sendDeviceUpdate:@"capabilitieschanged" device:device withData:data];
}

- (void) connectableDeviceConnectionSuccess:(ConnectableDevice*)device forService:(DeviceService *)service
{
    [self sendServiceUpdate:@"serviceconnect" device:device service:service withData:nil];
}

- (void) connectableDevice:(ConnectableDevice*)device service:(DeviceService *)service disconnectedWithError:(NSError*)error
{
    NSDictionary* errorObj = error ? @{@"message": [error localizedDescription]} : nil;
    
    [self sendServiceUpdate:@"servicedisconnect" device:device service:service withData:errorObj];
}

- (void) connectableDeviceConnectionRequired:(ConnectableDevice *)device forService:(DeviceService *)service;
{
    [self sendServiceUpdate:@"serviceconnectionrequired" device:device service:service withData:nil];
}

- (void) connectableDevice:(ConnectableDevice *)device service:(DeviceService *)service didFailConnectWithError:(NSError*)error
{
    NSDictionary* errorObj = error ? @{@"message": [error localizedDescription]} : nil;
    
    [self sendServiceUpdate:@"serviceconnectionerror" device:device service:service withData:errorObj];
}

- (void) connectableDevice:(ConnectableDevice *)device service:(DeviceService *)service pairingRequiredOfType:(int)pairingType withData:(id)pairingData
{
    NSDictionary* pairingInfo = nil;
    
    switch (pairingType) {
        case DeviceServicePairingTypeFirstScreen:
            pairingInfo = @{@"pairingType": @"firstScreen"};
            break;
        case DeviceServicePairingTypePinCode:
            pairingInfo = @{@"pairingType": @"pinCode"};
            break;
        case DeviceServicePairingTypeNone:
            pairingInfo = @{@"pairingType": @"none"};
            break;
        case DeviceServicePairingTypeMixed:
            pairingInfo = @{@"pairingType": @"mixed"};
            break;
        case DeviceServicePairingTypeAirPlayMirroring:
            // TODO: provide a way to override automatically showing alert
            [(UIAlertView *)pairingData show];
            
            pairingInfo = @{@"pairingType": @"airPlayMirroring"};
            break;
    }
    
    [self sendServiceUpdate:@"servicepairingrequired" device:device service:service withData:pairingInfo];
}

- (void) connectableDevicePairingSuccess:(ConnectableDevice*)device service:(DeviceService *)service
{
    [self sendDeviceUpdate:@"servicepairingsuccess" device:device withData:nil];
}

- (void) connectableDevice:(ConnectableDevice *)device service:(DeviceService *)service pairingFailedWithError:(NSError*)error
{
    NSDictionary* errorObj = @{@"message": [error localizedDescription]};
    
    [self sendDeviceUpdate:@"servicepairingerror" device:device withData:errorObj];
}

#pragma mark - Internal methods

- (void) addObjectWrapper:(JSObjectWrapper*)wrapper
{
    [_objectWrappers setObject:wrapper forKey:wrapper.objectId];
}

- (void) removeObjectWrapper:(JSObjectWrapper*)wrapper
{
    [_objectWrappers removeObjectForKey:wrapper.objectId];
    [wrapper cleanup];
}

- (JSObjectWrapper*) getObjectWrapper:(NSString*)objectId
{
    return [_objectWrappers objectForKey:objectId];
}

@end
