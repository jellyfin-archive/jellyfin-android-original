// Copyright 2013 Google Inc.

#import "GCKCastChannel.h"

@class GCKMediaInformation;
@class GCKMediaStatus;
@class GCKMediaTextTrackStyle;

@protocol GCKMediaControlChannelDelegate;

/**
 * @file GCKMediaControlChannel.h
 * GCKMediaControlChannelResumeState enum.
 */

/**
 * The receiver application ID for the Default Media Receiver.
 *
 * Any operations which apply to a currently active stream (play, pause, seek, stop, etc) require
 * a valid (that is, non-nil) media status, or they will return |kGCKInvalidRequestID| and will
 * not send the request. A media status is requested automatically when the channel connects, is
 * included with a successful load completed respose, and can also be updated at any time.
 * The media status can also become nil at any time; this will happen if the channel is
 * temporarily disconnected, for example.
 *
 * In general when using this channel, media status changes should be monitored with
 * |mediaControlChannelDidUpdateStatus:|, and methods which act on streams should be called only
 * while the media status is non-nil.
 */
GCK_EXTERN NSString *const kGCKMediaDefaultReceiverApplicationID;

/**
 * @enum GCKMediaControlChannelResumeState
 * Enum defining the media control channel resume state.
 */
typedef NS_ENUM(NSInteger, GCKMediaControlChannelResumeState) {
  /** A resume state indicating that the player state should be left unchanged. */
  GCKMediaControlChannelResumeStateUnchanged = 0,

  /**
   * A resume state indicating that the player should be playing, regardless of its current
   * state.
   */
  GCKMediaControlChannelResumeStatePlay = 1,

  /**
   * A resume state indicating that the player should be paused, regardless of its current
   * state.
   */
  GCKMediaControlChannelResumeStatePause = 2,
};

/**
 * A CastChannel for media control operations.
 *
 * @ingroup MediaControl
 */
GCK_EXPORT
@interface GCKMediaControlChannel : GCKCastChannel

/**
 * The media status for the currently loaded media, if any; otherwise <code>nil</code>.
 */
@property(nonatomic, strong, readonly) GCKMediaStatus *mediaStatus;

/**
 * The delegate for receiving notifications about changes in the channel's state.
 */
@property(nonatomic, weak) id<GCKMediaControlChannelDelegate> delegate;

/**
 * Designated initializer.
 */
- (instancetype)init;

/**
 * Loads and starts playback of a new media item.
 *
 * @param mediaInfo An object describing the media item to load.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)loadMedia:(GCKMediaInformation *)mediaInfo;

/**
 * Loads and optionally starts playback of a new media item.
 *
 * @param mediaInfo An object describing the media item to load.
 * @param autoplay Whether playback should start immediately.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)loadMedia:(GCKMediaInformation *)mediaInfo autoplay:(BOOL)autoplay;

/**
 * Loads and optionally starts playback of a new media item.
 *
 * @param mediaInfo An object describing the media item to load.
 * @param autoplay Whether playback should start immediately.
 * @param playPosition The initial playback position.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)loadMedia:(GCKMediaInformation *)mediaInfo
              autoplay:(BOOL)autoplay
          playPosition:(NSTimeInterval)playPosition;

/**
 * Loads and optionally starts playback of a new media item.
 *
 * @param mediaInfo An object describing the media item to load.
 * @param autoplay Whether playback should start immediately.
 * @param playPosition The initial playback position.
 * @param customData Custom application-specific data to pass along with the request. Must either
 * be an object that can be serialized to JSON using NSJSONSerialization, or nil.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)loadMedia:(GCKMediaInformation *)mediaInfo
              autoplay:(BOOL)autoplay
          playPosition:(NSTimeInterval)playPosition
            customData:(id)customData;

/**
 * Loads and optionally starts playback of a new media item.
 *
 * @param mediaInfo An object describing the media item to load.
 * @param autoplay Whether playback should start immediately.
 * @param playPosition The initial playback position.
 * @param activeTrackIDs An array of integers (as NSNumbers) specifying the active tracks.
 * May be nil.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)loadMedia:(GCKMediaInformation *)mediaInfo
              autoplay:(BOOL)autoplay
          playPosition:(NSTimeInterval)playPosition
        activeTrackIDs:(NSArray *)activeTrackIDs;

/**
 * Loads and optionally starts playback of a new media item.
 *
 * @param mediaInfo An object describing the media item to load.
 * @param autoplay Whether playback should start immediately.
 * @param playPosition The initial playback position.
 * @param activeTrackIDs An array of integers (as NSNumbers) specifying the active tracks.
 * May be nil.
 * @param customData Custom application-specific data to pass along with the request. Must either
 * be an object that can be serialized to JSON using NSJSONSerialization, or nil.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)loadMedia:(GCKMediaInformation *)mediaInfo
              autoplay:(BOOL)autoplay
          playPosition:(NSTimeInterval)playPosition
        activeTrackIDs:(NSArray *)activeTrackIDs
            customData:(id)customData;

/**
 * Sets the active tracks. Request will fail if there is no current media status.
 *
 * @param activeTrackIDs An array of integers (as NSNumbers) specifying the active tracks.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)setActiveTrackIDs:(NSArray *)activeTrackIDs;

/**
 * Sets the text track style. Request will fail if there is no current media status.
 *
 * @param textTrackStyle The text track style. Style will not be changed if nil.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)setTextTrackStyle:(GCKMediaTextTrackStyle *)textTrackStyle;

/**
 * Pauses playback of the current media item. Request will fail if there is no current media status.
 *
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)pause;

/**
 * Pauses playback of the current media item. Request will fail if there is no current media status.
 *
 * @param customData Custom application-specific data to pass along with the request. Must either
 * be an object that can be serialized to JSON using NSJSONSerialization, or nil.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)pauseWithCustomData:(id)customData;

/**
 * Stops playback of the current media item. Request will fail if there is no current media status.
 *
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)stop;

/**
 * Stops playback of the current media item. Request will fail if there is no current media status.
 *
 * @param customData Custom application-specific data to pass along with the request. Must either
 * be an object that can be serialized to JSON using NSJSONSerialization, or nil.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)stopWithCustomData:(id)customData;

/**
 * Begins (or resumes) playback of the current media item. Playback always begins at the
 * beginning of the stream. Request will fail if there is no current media status.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)play;

/**
 * Begins (or resumes) playback of the current media item. Playback always begins at the
 * beginning of the stream. Request will fail if there is no current media status.
 *
 * @param customData Custom application-specific data to pass along with the request. Must either
 * be an object that can be serialized to JSON using NSJSONSerialization, or nil.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)playWithCustomData:(id)customData;

/**
 * Seeks to a new time within the current media item. Request will fail if there is no current
 * media status.
 *
 * @param position The new time interval from the beginning of the stream.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)seekToTimeInterval:(NSTimeInterval)timeInterval;

/**
 * Seeks to a new position within the current media item. Request will fail if there is no current
 * media status.
 *
 * @param position The new time interval from the beginning of the stream.
 * @param resumeState The action to take after the seek operation has finished.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)seekToTimeInterval:(NSTimeInterval)position
                    resumeState:(GCKMediaControlChannelResumeState)resumeState;

/**
 * Seeks to a new position within the current media item. Request will fail if there is no current
 * media status.
 *
 * @param position The time interval from the beginning of the stream.
 * @param resumeState The action to take after the seek operation has finished.
 * @param customData Custom application-specific data to pass along with the request. Must either
 * be an object that can be serialized to JSON using NSJSONSerialization, or nil.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)seekToTimeInterval:(NSTimeInterval)position
                    resumeState:(GCKMediaControlChannelResumeState)resumeState
                     customData:(id)customData;

/**
 * Sets the stream volume. Request will fail if there is no current media status.
 *
 * @param volume The new volume, in the range [0.0 - 1.0].
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)setStreamVolume:(float)volume;

/**
 * Sets the stream volume. Request will fail if there is no current media status.
 *
 * @param volume The new volume, in the range [0.0 - 1.0].
 * @param customData Custom application-specific data to pass along with the request. Must either
 * be an object that can be serialized to JSON using NSJSONSerialization, or nil.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)setStreamVolume:(float)volume customData:(id)customData;

/**
 * Sets whether the stream is muted. Request will fail if there is no current media status.
 *
 * @param muted Whether the stream should be muted or unmuted.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)setStreamMuted:(BOOL)muted;

/**
 * Sets whether the stream is muted. Request will fail if there is no current media status.
 *
 * @param muted Whether the stream should be muted or unmuted.
 * @param customData Custom application-specific data to pass along with the request. Must either
 * be an object that can be serialized to JSON using NSJSONSerialization, or nil.
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)setStreamMuted:(BOOL)muted customData:(id)customData;


/**
 * Requests updated media status information from the receiver.
 *
 * @return The request ID, or kGCKInvalidRequestID if the message could not be sent.
 */
- (NSInteger)requestStatus;

/**
 * Returns the approximate stream position as calculated from the last received stream
 * information and the elapsed wall-time since that update. If the channel is not connected, or if
 * no media is currently loaded, 0 is returned.
 */
- (NSTimeInterval)approximateStreamPosition;

/**
 * Cancels an in-progress request. Cancelling a request does not prevent it from being executed;
 * it simply indicates that the calling application is no longer interested in the results of the
 * request, so any state associated with the tracking of the request will be cleared.
 *
 * @param The ID of the request to cancel.
 * @return YES if the request was cancelled, or NO if there is no request being tracked with the
 * given ID.
 */
- (BOOL)cancelRequestWithID:(NSInteger)requestID;

@end

/**
 * The delegate for GCKMediaControlChannel notifications.
 */
GCK_EXPORT
@protocol GCKMediaControlChannelDelegate <NSObject>

@optional

/**
 * Called when a request to load media has completed.
 *
 * @param mediaSessionId The unique media session ID that has been assigned to this media item.
 */
- (void)mediaControlChannel:(GCKMediaControlChannel *)mediaControlChannel
    didCompleteLoadWithSessionID:(NSInteger)sessionID;

/**
 * Called when a request to load media has failed.
 */
- (void)mediaControlChannel:(GCKMediaControlChannel *)mediaControlChannel
    didFailToLoadMediaWithError:(NSError *)error;

/**
 * Called when updated player status information is received.
 */
- (void)mediaControlChannelDidUpdateStatus:(GCKMediaControlChannel *)mediaControlChannel;

/**
 * Called when updated media metadata is received.
 */
- (void)mediaControlChannelDidUpdateMetadata:(GCKMediaControlChannel *)mediaControlChannel;

/**
 * Called when a request succeeds.
 *
 * @param requestID The request ID that failed. This is the ID returned when the request was made.
 */
- (void)mediaControlChannel:(GCKMediaControlChannel *)mediaControlChannel
    requestDidCompleteWithID:(NSInteger)requestID;

/**
 * Called when a request is no longer being tracked because another request of the same type has
 * been issued by the application.
 *
 * @param requestID The request ID that has been replaced. This is the ID returned when the request
 * was made.
 */
- (void)mediaControlChannel:(GCKMediaControlChannel *)mediaControlChannel
    didReplaceRequestWithID:(NSInteger)requestID;

/**
 * Called when a request is no longer being tracked because it has been explicitly cancelled.
 *
 * @param requestID The request ID that has been cancelled. This is the ID returned when the request
 * was made.
 */
- (void)mediaControlChannel:(GCKMediaControlChannel *)mediaControlChannel
    didCancelRequestWithID:(NSInteger)requestID;

/**
 * Called when a request fails.
 *
 * @param requestID The request ID that failed. This is the ID returned when the request was made.
 * @param error The error. If any custom data was associated with the error, it will be in the
 * error's userInfo dictionary with the key {@code kGCKErrorCustomDataKey}.
 */
- (void)mediaControlChannel:(GCKMediaControlChannel *)mediaControlChannel
       requestDidFailWithID:(NSInteger)requestID
                      error:(NSError *)error;

@end
