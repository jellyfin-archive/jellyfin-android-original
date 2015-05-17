// Copyright 2013 Google Inc.

#import <Foundation/Foundation.h>

#import "GCKDefines.h"

@class GCKMediaInformation;

/**
 * @file GCKMediaStatus.h
 * GCKMediaPlayerState and GCKMediaPlayerIdleReason enums.
 */

/** A flag (bitmask) indicating that a media item can be paused. */
GCK_EXTERN const NSInteger kGCKMediaCommandPause;

/** A flag (bitmask) indicating that a media item supports seeking. */
GCK_EXTERN const NSInteger kGCKMediaCommandSeek;

/** A flag (bitmask) indicating that a media item's audio volume can be changed. */
GCK_EXTERN const NSInteger kGCKMediaCommandSetVolume;

/** A flag (bitmask) indicating that a media item's audio can be muted. */
GCK_EXTERN const NSInteger kGCKMediaCommandToggleMute;

/** A flag (bitmask) indicating that a media item supports skipping forward. */
GCK_EXTERN const NSInteger kGCKMediaCommandSkipForward;

/** A flag (bitmask) indicating that a media item supports skipping backward. */
GCK_EXTERN const NSInteger kGCKMediaCommandSkipBackward;

/**
 * @enum GCKMediaPlayerState
 * Media player states.
 */
typedef NS_ENUM(NSInteger, GCKMediaPlayerState) {
  /** Constant indicating unknown player state. */
  GCKMediaPlayerStateUnknown = 0,
  /** Constant indicating that the media player is idle. */
  GCKMediaPlayerStateIdle = 1,
  /** Constant indicating that the media player is playing. */
  GCKMediaPlayerStatePlaying = 2,
  /** Constant indicating that the media player is paused. */
  GCKMediaPlayerStatePaused = 3,
  /** Constant indicating that the media player is buffering. */
  GCKMediaPlayerStateBuffering = 4,
};

/**
 * @enum GCKMediaPlayerIdleReason
 * Media player idle reasons.
 */
typedef NS_ENUM(NSInteger, GCKMediaPlayerIdleReason) {
  /** Constant indicating that the player currently has no idle reason. */
  GCKMediaPlayerIdleReasonNone = 0,

  /** Constant indicating that the player is idle because playback has finished. */
  GCKMediaPlayerIdleReasonFinished = 1,

  /**
   * Constant indicating that the player is idle because playback has been cancelled in
   * response to a STOP command.
   */
  GCKMediaPlayerIdleReasonCancelled = 2,

  /**
   * Constant indicating that the player is idle because playback has been interrupted by
   * a LOAD command.
   */
  GCKMediaPlayerIdleReasonInterrupted = 3,

  /** Constant indicating that the player is idle because a playback error has occurred. */
  GCKMediaPlayerIdleReasonError = 4,
};

/**
 * A class that holds status information about some media.
 *
 * @ingroup MediaControl
 */
GCK_EXPORT
@interface GCKMediaStatus : NSObject<NSCopying>

/**
 * The media session ID for this item.
 */
@property(nonatomic, readonly) NSInteger mediaSessionID;

/**
 * The current player state.
 */
@property(nonatomic, readonly) GCKMediaPlayerState playerState;

/**
 * The current idle reason. This value is only meaningful if the player state is
 * GCKMediaPlayerStateIdle.
 */
@property(nonatomic, readonly) GCKMediaPlayerIdleReason idleReason;

/**
 * Gets the current stream playback rate. This will be negative if the stream is seeking
 * backwards, 0 if the stream is paused, 1 if the stream is playing normally, and some other
 * postive value if the stream is seeking forwards.
 */
@property(nonatomic, readonly) float playbackRate;

/**
 * The GCKMediaInformation for this item.
 */
@property(nonatomic, strong, readonly) GCKMediaInformation *mediaInformation;

/**
 * The current stream position, as an NSTimeInterval from the start of the stream.
 */
@property(nonatomic, readonly) NSTimeInterval streamPosition;

/**
 * The stream's volume.
 */
@property(nonatomic, readonly) float volume;

/**
 * The stream's mute state.
 */
@property(nonatomic, readonly) BOOL isMuted;

/**
 * The list of active track IDs. Each element of the array is an NSNumber containing an integer
 * track ID.
 */
@property(nonatomic, readonly) NSArray *activeTrackIDs;

/**
 * Any custom data that is associated with the media item.
 */
@property(nonatomic, strong, readonly) id customData;

/**
 * Designated initializer.
 *
 * @param mediaSessionID The media session ID.
 * @param mediaInformation The media information.
 */
- (instancetype)initWithSessionID:(NSInteger)mediaSessionID
                 mediaInformation:(GCKMediaInformation *)mediaInformation;

/**
 * Checks if the stream supports a given control command.
 */
- (BOOL)isMediaCommandSupported:(NSInteger)command;

@end
