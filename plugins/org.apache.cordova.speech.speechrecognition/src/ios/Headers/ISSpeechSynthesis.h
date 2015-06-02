//
//  ISSpeechSynthesis.h
//  iSpeechSDK
//
//  Copyright (c) 2012 iSpeech, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "iSpeechSDK.h"
#import "ISSpeechSynthesisVoices.h"

#if NS_BLOCKS_AVAILABLE

/*^*
 * The callback handler for a speech synthesis request.
 *
 * @param error An error for the request, if one occurred, otherwise, `nil`.
 * @param userCancelled Whether speech synthesis finished as a result of user cancellation or not.
 */
typedef void(^ISSpeechSynthesisHandler)(NSError *error, BOOL userCancelled);

#endif

@class ISSpeechSynthesis;

/**
 * Delegate protocol for `ISSpeechSynthesis`.
 * 
 * All methods are optional.
 */
@protocol ISSpeechSynthesisDelegate <NSObject>

@optional

/**
 * The specified speech synthesis instance started speaking. Audio is now playing.
 * 
 * @param speechSynthesis The speech synthesis object that is speaking.
 */
- (void)synthesisDidStartSpeaking:(ISSpeechSynthesis *)speechSynthesis;

/**
 * The specified speech synthesis isntance finished speaking, either on its own or because the user cancelled it.
 * 
 * @param speechSynthesis The speech synthesis object that finished speaking.
 * @param userCancelled Whether the user was responsible for cancelling the speech synthesis, usually by tapping the "Cancel" button on the dialog.
 */
- (void)synthesisDidFinishSpeaking:(ISSpeechSynthesis *)speechSynthesis userCancelled:(BOOL)userCancelled;

/**
 * Something went wrong with the speech synthesis. Usually this is used for errors returned by the server.
 * 
 * @param speechSynthesis The speech synthesis object that the error occurred on.
 * @param error The acutal error. Errors from the SDK internals will have the error domain of `iSpeechErrorDomain`. You may get some URL connection errors if something happens with the network.
 */
- (void)synthesis:(ISSpeechSynthesis *)speechSynthesis didFailWithError:(NSError *)error;

@end

/**
 * The interface for doing speech synthesis in the SDK.
 */
@interface ISSpeechSynthesis : NSObject

/** @name Getting and Setting the Delegate */

/**
 * The delegate of a speech synthesis object.
 * 
 * The delegate must adopt the `<ISSpeechSynthesisDelegate>` protocol.
 */
@property (nonatomic, unsafe_unretained) id <ISSpeechSynthesisDelegate> delegate;

/** @name Configuration Properties */

/**
 * Sets the voice to use for this speech synthesis instance.
 * 
 * Voices are listed in the `ISSpeechSynthesisVoices.h` header file. You are not limited to that list; they are only standard voices. If you specify an invalid voice, the delegate will get an error.
 */
@property (nonatomic, copy) NSString *voice CONFIGURATION_METHOD;

/**
 * Sets the speed to use for speech synthesis.
 * 
 * This should be a number anywhere between -10 and 10, with -10 being the slowest, and 10 being the fastest. If you provide a number larger than 10, the speed will be set to 10. Likewise, if you provide a number smaller than -10, the speed will be set to -10.
 */
@property (nonatomic, assign) NSInteger speed CONFIGURATION_METHOD;

/**
 * The bitrate of the synthesised speech.
 * 
 * The higher the bitrate, the better quality the audio, but the larger the file size of the data being sent, which results in more buffering needed to load all that data. Default value is 48, which is sutable for WiFi, 4G, and 3G. 
 * 
 * Valid values include 8, 16, 24, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 192, 224, 256, and 320, as well as any others listed under "Bit Rates" for an API key's Text-to-Speech Settings.
 */
@property (nonatomic, assign) NSInteger bitrate CONFIGURATION_METHOD;

/** Getting and Setting the Text */

/**
 * The text to speak.
 */
@property (nonatomic, copy) NSString *text;

/** @name Creating an Instance */

/**
 * Create a new `ISSpeechSynthesis` object with the supplied text.
 *
 * @param text The initial text for the speech synthesis object.
 */
- (id)initWithText:(NSString *)text;

/** @name Speaking Text */

/**
 * Speak the text that was specified when creating this instance.
 * 
 * @param err An `NSError` pointer to get an error object out of the method if something goes wrong.
 * @return Whether synthesis successfully started. If this returns `NO`, check the error for details on what went wrong.
 */
- (BOOL)speak:(NSError **)err;

#if NS_BLOCKS_AVAILABLE

/**
 * Speak the text that was specified when creating this instance.
 *
 * @param handler A `ISSpeechSynthesisHandler` block to be executed when speaking finishes, or when an error occurs. This handler will be called on the main thread.
 */
- (void)speakWithHandler:(ISSpeechSynthesisHandler)handler;

#endif

/**
 * Cancels an in-progress speech synthesis action.
 */
- (void)cancel;

@end
