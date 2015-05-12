//
//  CDVUniqueDeviceID.m
//
//
//

#import "CDVUniqueDeviceID.h"
#import "UICKeyChainStore.h"

@implementation CDVUniqueDeviceID

-(void)get:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        NSString *uuidUserDefaults = [defaults objectForKey:@"uuid"];
        
        NSString *uuid = [UICKeyChainStore stringForKey:@"uuid"];

        if ( uuid && !uuidUserDefaults) {
            [defaults setObject:uuid forKey:@"uuid"];
            [defaults synchronize];
            
        }  else if ( !uuid && !uuidUserDefaults ) {
            NSString *uuidString = [[NSUUID UUID] UUIDString];
            
            [UICKeyChainStore setString:uuidString forKey:@"uuid"];
            
            [defaults setObject:uuidString forKey:@"uuid"];
            [defaults synchronize];
            
            uuid = [UICKeyChainStore stringForKey:@"uuid"];
            
        } else if ( ![uuid isEqualToString:uuidUserDefaults] ) {
            [UICKeyChainStore setString:uuidUserDefaults forKey:@"uuid"];
            uuid = [UICKeyChainStore stringForKey:@"uuid"];
        }

        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:uuid];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

@end

