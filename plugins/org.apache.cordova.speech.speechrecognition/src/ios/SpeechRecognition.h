#import <Cordova/CDV.h>
#import "ISpeechSDK.h"

@interface SpeechRecognition : CDVPlugin <ISSpeechRecognitionDelegate>

@property (nonatomic,strong) CDVInvokedUrlCommand * command;
@property (nonatomic,strong) CDVPluginResult* pluginResult;

- (void) init:(CDVInvokedUrlCommand*)command;
- (void) start:(CDVInvokedUrlCommand*)command;

@end

