#import "DeviceTextChannel.h"

// [START custom-channel-1]
// This custom channel class extends GCKCastChannel.
@implementation DeviceTextChannel

- (void)didReceiveTextMessage:(NSString*)message {
  NSLog(@"received message: %@", message);
}

@end
// [END custom-channel-1]