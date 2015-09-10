//
//  NSLoggerCocoaLumberjackConnectorPlugin.m
//

#import "NSLoggerCocoaLumberjackConnectorPlugin.h"

#import "DDTTYLogger.h"
#import "DDNSLoggerLogger.h"

int ddLogLevel = LOG_LEVEL_VERBOSE;

@implementation NSLoggerCocoaLumberjackConnectorPlugin

#pragma mark Initialization

- (void)pluginInitialize {
        
    // add Xcode console logger if not running in the App Store
    if (![self isAppStoreEnvironment]) {
        [DDLog addLogger:[DDTTYLogger sharedInstance]];
        [DDLog addLogger:[DDNSLoggerLogger sharedInstance]];
    }
    
    DDLogInfo(@"NSLoggerCocoaLumberjackConnectorPlugin Plugin initialized");
}

- (BOOL)isAppStoreEnvironment {
    // cribbed from HockeyApp SDK
    BOOL appStoreEnvironment=NO;
#if !TARGET_IPHONE_SIMULATOR
    // check if we are really in an app store environment
    if (![[NSBundle mainBundle] pathForResource:@"embedded" ofType:@"mobileprovision"]) {
      appStoreEnvironment = YES;
    }
#endif
    return appStoreEnvironment;
}

@end
