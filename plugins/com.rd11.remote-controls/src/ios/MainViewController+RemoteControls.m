//
//  MainViewController+RemoteControls.m
//
//  Created by Julio Cesar Sanchez Hernandez on 4/3/16.
//
//

#import "MainViewController+RemoteControls.h"

@implementation MainViewController (RemoteControls)

- (void)remoteControlReceivedWithEvent:(UIEvent *)receivedEvent {
    [[NSNotificationCenter defaultCenter] postNotificationName:@"receivedEvent" object:receivedEvent];
}

@end
