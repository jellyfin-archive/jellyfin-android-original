#import "DeviceTextChannel.h"
#import <GoogleCast/GoogleCast.h>
#import "CommandDelegate.h"

// [START custom-channel-1]
// This custom channel class extends GCKCastChannel.
@interface DeviceTextChannel : GCKCastChannel

@property(nonatomic, strong) CommandDelegate* commandDelegate;

@end
// [END custom-channel-1]