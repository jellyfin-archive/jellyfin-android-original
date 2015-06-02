//
//  iSpeechSDK.h
//  iSpeechSDK
//
//  Copyright (c) 2012 iSpeech, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

// Methods marked with `CONFIGURATION_METHOD` can be set globally, for all objects, by calling the methods on [[iSpeechSDK sharedSDK] configuration]. This mimics the Appearance API in iOS 5.
#define CONFIGURATION_METHOD 

#import "ISSpeechSynthesis.h"
#import "ISSpeechRecognition.h"

// Protocol used by objects that act as the proxy for the Configuration API. For details on each property here, look at ISSpeechSynthesis and ISSpeechRecognition.
@protocol ISConfiguration <NSObject>

@property (nonatomic, copy) NSString *voice;
@property (nonatomic, assign) NSInteger speed;
@property (nonatomic, assign) NSInteger bitrate;

@property (nonatomic, copy) NSString *locale;
@property (nonatomic, copy) NSString *model;

@property (nonatomic, assign) NSUInteger freeformType;

@property (nonatomic, assign) BOOL silenceDetectionEnabled;
@property (nonatomic, assign) BOOL adaptiveBitrateEnabled;

@end

/**
 * The error domain for errors returned by the SDK.
 */
extern NSString *const iSpeechErrorDomain;

/**
 * Possible error codes returned by the SDK.
 * 
 * Some of these should not be returned by the SDK (ones like `kISpeechErrorCodeInvalidFileFormat` and `kISpeechErrorCodeInvalidContentType`) because you don't have control over them. However, they are included in the off chance that something does go wrong with the server and they are returned. Codes that shouldn't be returned are marked with an asterisk (`*`).
 *
 * When you get an error during speech recognition, tell the user that something went wrong. If you get `kISpeechErrorCodeNoInputAvailable`, `kISpeechErrorCodeNoInternetConnection`, or `kISpeechErrorCodeLostInput` the error messages on those NSError instances have been localized, and are presentable to the user. 
 */
enum _ISpeechErrorCode {
	kISpeechErrorCodeInvalidAPIKey = 1,					// You provided an invalid API key.
	kISpeechErrorCodeUnableToConvert = 2,				// The server was unable to convert your text to speech.
	kISpeechErrorCodeNotEnoughCredits = 3,				// Your API key doesn't have the necessary credits required to complete this transaction.
	kISpeechErrorCodeNoActionSpecified = 4,				// *
	kISpeechErrorCodeInvalidText = 5,					// Usually, this error occurs when no text is sent to the server, or, for example, Japanese characters are sent to the English voice.
	kISpeechErrorCodeTooManyWords = 6,					// You tried to convert too many words to speech.
	kISpeechErrorCodeInvalidTextEntry = 7,				// *
	kISpeechErrorCodeInvalidVoice = 8,					// You specified a voice that either doesn't exist, or that you don't have access to.
	kISpeechErrorCodeInvalidFileFormat = 12,			// *
	kISpeechErrorCodeInvalidSpeed = 13,					// *
	kISpeechErrorCodeInvalidDictionary = 14,			// *
	kISpeechErrorCodeInvalidBitrate = 15,				// You specified a bitrate that isn't one of the allowed values. See -[ISSpeechSynthesis bitrate] for details on valid values.
	kISpeechErrorCodeInvalidFrequency = 16,				// *
	kISpeechErrorCodeInvalidAliasList = 17,				// *
	kISpeechErrorCodeAliasMissing = 18,					// *
	kISpeechErrorCodeInvalidContentType = 19,			// *
	kISpeechErrorCodeAliasListTooComplex = 20,			// *
	kISpeechErrorCodeCouldNotRecognize = 21,			// If the audio isn't clear enough, or corrupted, this error will get returned. It's usually good UX to prompt the user to try again.
	kISpeechErrorCodeOptionNotEnabled = 30,				// Option not enabled for your account. Please contact iSpeech sales at +1 (917) 338-7723 or at sales@ispeech.org to modify your license.
	kISpeechErrorCodeNoAPIAccess = 997,					// *
	kISpeechErrorCodeUnsupportedOutputType = 998,		// *
	kISpeechErrorCodeInvalidRequest = 999,				// *
	kISpeechErrorCodeTrialPeriodExceeded = 100,			// This evaluation account has exceeded its trial period. Please contact iSpeech sales at +1 (917) 338-7723 or at sales@ispeech.org to upgrade your license.
	kISpeechErrorCodeAPIKeyDisabled = 101,				// Your key has been disabled. Please contact iSpeech sales at +1 (917) 338-7723 or at sales@ispeech.org to modify your license.
	kISpeechErrorCodeInvalidRequestMethod = 1000,		// *
	
	// Error code 300 was "UserCancelled", but that has been wrapped into the SDK and is no longer used.
	kISpeechErrorCodeNoInputAvailable = 301,			// You wanted to do speech recognition, but there's no mic available.
	kISpeechErrorCodeNoInternetConnection = 302,		// There's no connection to the cloud to do speech synthesis or speech recognition.
	kISpeechErrorCodeSDKIsBusy = 303,					// The SDK is busy doing either recognition or synthesis.
	kISpeechErrorCodeSDKInterrupted = 304,				// The SDK was in the middle of doing something, and then got an audio session interruption
	kISpeechErrorCodeCouldNotActiveAudioSession = 305,	// Unable to activate the audio session. Can happen when another audio session has higher precedence than ours does.
	kISpeechErrorCodeCouldNotStartAudioQueue = 306,		// Unable to start an audio queue. Can happen when another audio queue has higher precedence than ours does.
	kISpeechErrorCodeServerDied = 307,					// Server Died error. mediaserverd has died, and we need to clear out all our audio objects and start fresh.
	kISpeechErrorCodeLostInput = 308,					// There was audio input, and speech recognition was happening, and then the audio input went away for some reason.
	kISpeechErrorCodeBadHost = 309,						// The SSL Certificate chain was invalid, probably a result of some redirect away from iSpeech's servers. An example of this happening is when connected to a WiFi network that requires authentication before sending network requests.
	
	kISpeechErrorCodeUnknownError = 399
};

typedef NSUInteger iSpeechErrorCode;

@class iSpeechSDK;

/**
 * iSpeechSDKDelegate has optional methods to be notified when things happen on the SDK. Currently only notifies when an audio session interruption begins and ends.
 */
@protocol iSpeechSDKDelegate <NSObject>

@optional

/**
 * The audio session has been interrupted. See [Responding to Audio Session Interruptions](https://developer.apple.com/library/ios/#documentation/Audio/Conceptual/AudioSessionProgrammingGuide/Cookbook/Cookbook.html#//apple_ref/doc/uid/TP40007875-CH6-SW7) in the [Audio Session Programming Guide](https://developer.apple.com/library/ios/#documentation/Audio/Conceptual/AudioSessionProgrammingGuide/Introduction/Introduction.html).
 * 
 * @param sdk The shared instance of the SDK.
 */
- (void)iSpeechSDKDidBeginInterruption:(iSpeechSDK *)sdk;

/**
 * The interupption on the audio session has ended. See [Responding to Audio Session Interruptions](https://developer.apple.com/library/ios/#documentation/Audio/Conceptual/AudioSessionProgrammingGuide/Cookbook/Cookbook.html#//apple_ref/doc/uid/TP40007875-CH6-SW7) in the [Audio Session Programming Guide](https://developer.apple.com/library/ios/#documentation/Audio/Conceptual/AudioSessionProgrammingGuide/Introduction/Introduction.html).
 * 
 * @param sdk The shared instance of the SDK.
 */
- (void)iSpeechSDKDidEndInterruption:(iSpeechSDK *)sdk;

@end

/**
 * The shared SDK class. Configuration of the SDK happens on this object, as well as getting the configuration object to set global configuration for ISSpeechSynthesis and ISSpeechRecognition objects.
 */
@interface iSpeechSDK : NSObject

/** @name Configuring the SDK Instance */

/**
 * Whether the SDK should use the Mobile Development Server (`YES`) or the Mobile Production Server (`NO`). This is set to `NO` by default.
 */
@property (nonatomic, assign) BOOL usesDevServer;

/**
 * Whether the SDK should vibrate on the Start Recording and Stop Recording prompts.
 *
 * These are off by default (`NO`) and will need to be turned on by setting this to `YES`.
 */
@property (nonatomic, assign) BOOL vibrateOnPrompts;

/**
 * Whether the SDK should play Success and Fail prompts on a successful or unsuccesful recognition.
 *
 * These are off by default (`NO`) and will need to be turned on by setting this to `YES`.
 */
@property (nonatomic, assign) BOOL playsSuccessAndFailPrompts;

/**
 * Allows you to tell the SDK whether or not it should deactivate the audio session once it's finished its stuff. If you're doing your own audio stuff in the app (such as playing music, an audiobook, etc.), you'd use this to make sure that your audio doesn't go away once the SDK finishes its speech synthesis or speech recognition. 
 */
@property (nonatomic, assign) BOOL shouldDeactivateAudioSessionWhenFinished;

/**
 * Any extra server params you want to send to the server.
 *
 * Use only if directed.
 */
@property (nonatomic, copy) NSString *extraServerParams;

/**
 * Sets the APIKey to send to the server.
 * 
 * The best place to set this is once in your `-applicationDidFinishLaunching:` method on your app delegate. Once set, you shoudn't have a reason to change it.
 */
@property (nonatomic, copy) NSString *APIKey;

/** @name Setting and Getting the Delegate */

/**
 * Set the delegate to be notified of audio session interruptions.
 * 
 * The delegate must adopt the `<iSpeechSDKDelegate>` protocol.
 */
@property (nonatomic, unsafe_unretained) id <iSpeechSDKDelegate> delegate;

/** @name SDK Properties */

/**
 * Returns whether the SDK is currently busy doing something, such as performing speech recognition or speech synthesis.
 */
@property (nonatomic, assign, readonly) BOOL isBusy;

/**
 * Returns the version number of the SDK. Useful for debugging purposes and bug reports.
 */
@property (nonatomic, copy, readonly) NSString *version;

/** @name Getting the SDK Instance */

/**
 * The single instance of the iSpeechSDK class.
 * 
 * @return Returns the shared instance of the SDK.
 */
+ (iSpeechSDK *)sharedSDK;

/** @name Getting the Configuration Instance */

/**
 * Method to get the configuration object to set properties globally for all objects. For example, if you wanted to set the voice for all speech recognition requests, you'd call `[[[iSpeechSDK sharedSDK] configuration] setVoice:VOICE_HERE]` and all subsequent speech recognition requests would use that voice.
 *
 * @return Returns the configuration proxy.
 */
- (id <ISConfiguration>)configuration;

/** @name Resetting the SDK */

/**
 * If you get a lot of 303 errors, even though you know for a fact that the SDK isn't doing anything, call this method to reset the SDK's internals.
 * 
 * Configuration properties set, including your API key, and anything sent to `[[iSpeechSDK sharedSDK] configuration]` will not be affected by this call. The delegate for any active speech synthesis or speech recognition will get a `kISpeechErrorCodeServerDied` error code.
 *
 * @warning This is a temporary fix and will be removed for the final 1.0 relase of the SDK.
 */
- (void)resetSDK;

// The following methods are provided in the event that you initialize the audio session before the SDK has a chance to. If you do, you MUST call these methods in your interruption listener, otherwise the SDK WILL break.

/** @name Interruption Handling */

/**
 * Tells the SDK that an interruption has begun. If you initialize the audio session before the SDK, you must call this method to ensure that the SDK does not break.
 */
- (void)beginInterruption;

/**
 * Tells the SDK that an interruption has ended. If you initialize the audio session before the SDK, you must call this method to ensure that the SDK does not break.
 */
- (void)endInterruption;

@end
