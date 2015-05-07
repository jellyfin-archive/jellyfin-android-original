#import "CDVBackgroundColor.h"
#import <Cordova/CDV.h>

@implementation CDVBackgroundColor

- (id)settingForKey:(NSString*)key
{
    return [self.commandDelegate.settings objectForKey:[key lowercaseString]];
}

- (UIColor *)colorFromHexString:(NSString*)hexString
{
    unsigned int rgbValue = 0;
    NSScanner* scanner = [NSScanner scannerWithString:hexString];
    [scanner scanHexInt:&rgbValue];

    return [UIColor colorWithRed:((float)((rgbValue & 0xFF0000) >> 16))/255.0
                           green:((float)((rgbValue & 0xFF00) >> 8))/255.0
                            blue:((float)(rgbValue & 0xFF))/255.0
                           alpha:((float)((rgbValue & 0xFF000000) >> 24))/255.0];
}


- (void) pluginInitialize
{
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(pageDidLoad) name:CDVPageDidLoadNotification object:self.webView];
}


- (void) pageDidLoad
{
    NSString* setting  = @"BackgroundColor";

    if ([self settingForKey:setting]) {
        UIColor *theColor = [self colorFromHexString:[self settingForKey:setting]];

        self.webView.backgroundColor = theColor;
    }
}

@end
