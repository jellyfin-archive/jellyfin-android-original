#import <Cordova/CDV.h>

@interface Launcher : CDVPlugin

- (void)canLaunch:(CDVInvokedUrlCommand*)command;
- (void)launch:(CDVInvokedUrlCommand*)command;

@end