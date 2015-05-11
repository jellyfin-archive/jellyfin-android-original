//
// NowPlaying.h
// Now Playing Cordova Plugin
//
// Created by François LASSERRE on 12/05/13.
// Copyright 2013 François LASSERRE. All rights reserved.
// MIT Licensed
//

#import <Cordova/CDVPlugin.h>
#import <MediaPlayer/MediaPlayer.h>
#import <MediaPlayer/MPNowPlayingInfoCenter.h>
#import <MediaPlayer/MPMediaItem.h>

@interface RemoteControls : CDVPlugin {
}

+(RemoteControls*)remoteControls;

- (void)updateMetas:(CDVInvokedUrlCommand*)command;
- (void)receiveRemoteEvent:(UIEvent *)receivedEvent;

@end
