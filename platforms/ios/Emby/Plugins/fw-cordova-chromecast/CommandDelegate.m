//
//  DeviceDelegate.m
//  HelloCordova
//
//  Created by Franz Wilding on 18.11.15.
//
//

#import <Foundation/Foundation.h>
#import "CommandDelegate.h"

@implementation CommandDelegate

- (id)initWithCommandDelegate:(id<CDVCommandDelegate>)commandDelegate andCallbackId:(NSString *)callbackId {
    self.commandDelegate = commandDelegate;
    self.callbackId = callbackId;
    return [super init];
}

- (void)sendResponse:(id)data from:(NSString*)command andKeepItAlive:(bool)keepItAlive {
    NSError *error;
    NSDictionary *response = [[NSDictionary alloc] initWithObjectsAndKeys:
                              command, @"command",
                              data, @"data", nil];
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:response options:0 error:&error];
    CDVPluginResult *pluginResult;
    
    if(error) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:
                        [NSString stringWithFormat:@"An Error occured trying to serialize response: %@", error]];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:
                        [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding]];
    }
    
    [pluginResult setKeepCallbackAsBool:keepItAlive];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
}

@end