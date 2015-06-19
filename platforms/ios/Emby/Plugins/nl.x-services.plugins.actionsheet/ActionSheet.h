#import <Cordova/CDVPlugin.h>

@interface ActionSheet :CDVPlugin<UIActionSheetDelegate>

- (void) show:(CDVInvokedUrlCommand*)command;
- (void) hide:(CDVInvokedUrlCommand*)command;

@end
