//
//  DeviceDelegate.m
//  HelloCordova
//
//  Created by Franz Wilding on 18.11.15.
//
//

#import <Foundation/Foundation.h>
#import "DeviceTextChannel.h"
#import <GoogleCast/GoogleCast.h>

@implementation DeviceTextChannel

- (void)didReceiveTextMessage:(NSString*)message {
  NSLog(@"received message: %@", message);
}

@end
// [END custom-channel-1]