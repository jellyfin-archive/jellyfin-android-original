//
//  NSObject_DeviceDelegate.h
//  HelloCordova
//
//  Created by Franz Wilding on 18.11.15.
//
//

#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>

@interface CommandDelegate : NSObject

- (id)initWithCommandDelegate:(id<CDVCommandDelegate>)commandDelegate andCallbackId:(NSString*)callbackId;
- (void)sendResponse:(id)response from:(NSString*)command andKeepItAlive:(bool)keepItAlive;
@property (nonatomic, weak) id<CDVCommandDelegate> commandDelegate;
@property (nonatomic, strong) NSString* callbackId;

@end
