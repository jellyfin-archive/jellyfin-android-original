
//
//  NSObject_DeviceDelegate.h
//  HelloCordova
//
//  Created by Franz Wilding on 18.11.15.
//
//

#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import "CommandDelegate.h"
#import <GoogleCast/GoogleCast.h>


@interface MediaChannelDelegate : CommandDelegate
- (void)setChannel:(GCKMediaControlChannel*)channel;
- (void)loadMedia:(GCKMediaInformation*)mediaInformation;
- (void)play;
- (void)pause;
- (void)stop;
- (void)mute:(BOOL)mute;
- (void)setVolume:(float)volume;
- (void)seek:(NSTimeInterval)time;

@property(nonatomic, strong) GCKMediaControlChannel *channel;
@end
