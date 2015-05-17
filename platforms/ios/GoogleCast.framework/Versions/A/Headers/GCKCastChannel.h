// Copyright 2013 Google Inc.

#import <Foundation/Foundation.h>

#import "GCKDefines.h"

@class GCKDeviceManager;

/**
 * A GCKCastChannel is used to send and receive messages that are tagged with a specific
 * namespace. In this way, multiple channels may be multiplexed over a single connection
 * to a Cast device.
 * <p>
 * Subclasses should implement the @link GCKCastChannel#didReceiveTextMessage: @endlink method to
 * process incoming messages, and will typically provide additional methods for sending messages
 * that are specific to a given namespace.
 *
 * @ingroup Messages
 */
GCK_EXPORT
@interface GCKCastChannel : NSObject

/** The channel's namespace. */
@property(nonatomic, copy, readonly) NSString *protocolNamespace;

/** A flag indicating whether this channel is currently connected. */
@property(nonatomic, readonly) BOOL isConnected;

/** The device manager with which this channel is registered, if any. */
@property(nonatomic, weak, readonly) GCKDeviceManager *deviceManager;

/**
 * Designated initializer. Constructs a new GCKCastChannel with the given namespace.
 *
 * @param protocolNamespace The namespace.
 */
- (instancetype)initWithNamespace:(NSString *)protocolNamespace NS_DESIGNATED_INITIALIZER;

/**
 * Called when a text message has been received on this channel. The default implementation is a
 * no-op.
 *
 * @param message The message.
 */
- (void)didReceiveTextMessage:(NSString *)message;

/**
 * Sends a text message on this channel.
 *
 * @param message The message.
 * @return <code>YES</code> on success or <code>NO</code> if the message could not be sent (because
 * the channel is not connected, or because the send buffer is too full at the moment).
 */
- (BOOL)sendTextMessage:(NSString *)message;

/**
 * Generates a request ID for a new message.
 */
- (NSInteger)generateRequestID;

/**
 * A convenience method which wraps generateRequestID in an NSNumber.
 */
- (NSNumber *)generateRequestNumber;

/**
 * Called when this channel has been connected, indicating that messages can now be exchanged with
 * the Cast device over this channel. The default implementation is a no-op.
 */
- (void)didConnect;

/**
 * Called when this channel has been disconnected, indicating that messages can no longer be
 * exchanged with the Cast device over this channel. The default implementation is a no-op.
 */
- (void)didDisconnect;

@end

