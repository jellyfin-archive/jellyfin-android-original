#import "BackgroundAudio.h"
#import <AVFoundation/AVFoundation.h>

@implementation BackgroundAudio

NSString* theCallbackId;

// this method is executed when the app loads because of the onload param in plugin.xml
- (void)pluginInitialize {
  AVAudioSession *audioSession = [AVAudioSession sharedInstance];
  NSError *setCategoryError = nil;
  BOOL ok = [audioSession setCategory:AVAudioSessionCategoryPlayback error:&setCategoryError];

  NSLog(@"BackgroundAudio plugin ok? %@", ok ? @"YES" : @"NO");
  if (!ok) {
    NSLog(@"BackgroundAudio plugin error: %@", setCategoryError.description);
  }
}

@end