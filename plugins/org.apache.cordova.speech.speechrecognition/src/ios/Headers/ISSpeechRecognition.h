//
//  ISSpeechRecognition.h
//  iSpeechSDK
//
//  Copyright (c) 2012 iSpeech, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AudioToolbox/AudioToolbox.h>

#import "iSpeechSDK.h"

#import "ISSpeechRecognitionLocales.h"
#import "ISSpeechRecognitionResult.h"

/**
 * The type of model to use when trascribing audio. Currently, only SMS and Dictation are available.
 */
enum {
	ISFreeFormTypeSMS = 1,
	ISFreeFormTypeVoicemail = 2,
	ISFreeFormTypeDictation = 3,
	ISFreeFormTypeMessage = 4,
	ISFreeFormTypeInstantMessage = 5,
	ISFreeFormTypeTranscript = 6,
	ISFreeFormTypeMemo = 7,
};

typedef NSUInteger ISFreeFormType;

#if NS_BLOCKS_AVAILABLE

/*^*
 * The callback handler for speech recognition request.
 *
 * @param error An error, if one occured, or `nil`.
 * @param result The result of a successful recognition, or `nil` if there's an error.
 * @param cancelledByUser Whether speech recognition finished because a user cancelled the request.
 */
typedef void(^ISSpeechRecognitionHandler)(NSError *error, ISSpeechRecognitionResult *result, BOOL cancelledByUser);

#endif

@class ISSpeechRecognition;

/**
 * Delegate protocol for `ISSpeechRecognition`.
 * 
 * The only required method is for getting the result from speech recognition.
 */
@protocol ISSpeechRecognitionDelegate <NSObject>

@required

/**
 * Speech recognition successfully completed, and a result was sent back from the server.
 * 
 * If you get no result text back, and a confience level of 0.0, then, most likely, the user didn't speak anything.
 * 
 * @param speechRecognition The speech recognition instance that completed.
 * @param result The result text and confidence level.
 */
- (void)recognition:(ISSpeechRecognition *)speechRecognition didGetRecognitionResult:(ISSpeechRecognitionResult *)result;

@optional

/**
 * Something went wrong, speech recognition failed, and an error was passed back.
 * 
 * @param speechRecognition The speech recognition instance that was cancelled.
 * @param error The acutal error. Errors from the SDK internals will have the error domain of `iSpeechErrorDomain`. You may get some URL connection errors if something happens with the network.
 */
- (void)recognition:(ISSpeechRecognition *)speechRecognition didFailWithError:(NSError *)error;

/**
 * Speech recognition was cancelled by the user.
 * 
 * @param speechRecognition The speech recognition instance that was cancelled.
 */
- (void)recognitionCancelledByUser:(ISSpeechRecognition *)speechRecognition;

/**
 * Recording the user's speech has started.
 * 
 * @param speechRecognition The speech recognition instance that started recording audio.
 */
- (void)recognitionDidBeginRecording:(ISSpeechRecognition *)speechRecognition;

/**
 * Speech recognition has finished recording and is moving on to recognizing the text.
 * 
 * This happens when the timeout is hit for a timed listen, or when the user taps the "Done" button on the dialog.
 * 
 * @param speechRecognition The speech recognition instance that finished recording.
 */
- (void)recognitionDidFinishRecording:(ISSpeechRecognition *)speechRecognition;

@end

/**
 * The interface for doing speech recognition in the SDK.
 */
@interface ISSpeechRecognition : NSObject

/** @name Getting and Setting the Delegate */

/**
 * The delegate of a speech recognition object.
 * 
 * The delegate must adopt the `<ISSpeechRecognitionDelegate>` protocol.
 */
@property (nonatomic, unsafe_unretained) id <ISSpeechRecognitionDelegate> delegate;

/** @name Configuration Properties */

/**
 * Sets the locale to use for speech recognition.
 *
 * Most of the time, the value passed is a ISO country code. To get our supported ISOs, consult "Freeform Dictation Languages" under "Speech Recognition Settings" when viewing details about a specific key.
 */
@property (nonatomic, copy) NSString *locale CONFIGURATION_METHOD;

/**
 * Allows you to set a custom language model for speech recognition.
 */
@property (nonatomic, copy) NSString *model CONFIGURATION_METHOD;

/**
 * The type of model to use when trascribing audio. Defaults to `ISFreeFormTypeDictation`. 
 */
@property (nonatomic, assign) ISFreeFormType freeformType CONFIGURATION_METHOD;

/**
 * Whether silence detection should be used to automatically detect when someone's done talking.
 */
@property (nonatomic, assign) BOOL silenceDetectionEnabled CONFIGURATION_METHOD;

/** @name Detecting Audio Input */

/**
 * Returns whether audio input is available for speech recognition. You can check this before creating an instance of `ISSpeechRecognition`, as well as to dynamically update your UI with what you can do.
 * 
 * @return Returns whether audio input is available.
 */
+ (BOOL)audioInputAvailable;

/** @name Aliases and Commands */

/**
 * Adds a list of items as an alias.
 * 
 * Think of using an alias list as a way of doing a regular expression. For example, if you want to use a regular expression to match "call joe", "call charlie", or "call ben", then it would be `call (joe|charlie|ben)`. Similarly, to do that with an alias list,
 *
 *	[speechRecognitionInstance addAlias:@"PEOPLE" forItems:[NSArray arrayWithObjects:@"joe", @"charlie", @"ben", nil]];
 *	[speechRecognitionInstance addCommand:@"call %PEOPLE%"];
 * 
 * @param alias The string to use for the alias key.
 * @param items The array of items to be substituted for the alias.
 */
- (void)addAlias:(NSString *)alias forItems:(NSArray *)items;

/**
 * Adds a command.
 *
 * If you want to reference an alias list, the format is `%ALIAS_LIST_NAME%`. Replace `ALIAS_LIST_NAME` with the actual name of your alias list.
 * 
 * @param command The command to be added.
 */
- (void)addCommand:(NSString *)command;

/**
 * Add multiple commands from an array.
 * 
 * @param commands The array of commands to be added.
 */
- (void)addCommands:(NSArray *)commands;

/**
 * Clears any command or alias lists on this speech recognition object.
 */
- (void)resetCommandsAndAliases;

/** @name Listening and Recognizing */

/**
 * Start an untimed listen.
 * 
 * An untimed listen means that the SDK will start listening, and will not stop unless you tell it to by calling -[ISSpeechRecognition finishListenAndStartRecognize], or until silence detection kicks in, if you have that enabled.
 * 
 * If you're using a command or alias list, use `-listenAndRecognizeWithTimeout:error:` instead. This will ensure that speech recognition will only last as long as is necessary, thus saving the user's battery life and data plan, ensuring that you get a result back, and providing an better overall experience.
 * 
 * @param err An `NSError` pointer to get an error object out of the method if something goes wrong.
 * @return Returns whatever speech synthesis successfully started. If this returns `NO`, check the error for details on what went wrong.
 * @see listenAndRecognizeWithTimeout:error:
 * @see finishListenAndStartRecognize
 */
- (BOOL)listen:(NSError **)err;

/**
 * If you're running an untimed listen, or if you want to cut a timed listen short, call this method to tell the SDK to stop listening for audio, and finish up transcribing.
 */
- (void)finishListenAndStartRecognize;

/**
 * Starts a timed listen. After a set timeout, the SDK will stop listening for audio and will start to transcribe it.
 * 
 * Useful when using command lists to ensure that the user doesn't talk longer than necessary.
 * 
 * @param timeout The amount of time, in seconds, for the timed listen to last for.
 * @param err An `NSError` pointer to get an error object out of the method if something goes wrong.
 * @return Returns whatever speech synthesis successfully started. If this returns `NO`, check the error for details on what went wrong.
 */
- (BOOL)listenAndRecognizeWithTimeout:(NSTimeInterval)timeout error:(NSError **)err;

#if NS_BLOCKS_AVAILABLE

/**
 * Start an untimed listen.
 *
 * An untimed listen means that the SDK will start listening, and will not stop unless you tell it to by calling -[ISSpeechRecognition finishListenAndStartRecognize], or until silence detection kicks in, if you have that enabled.
 *
 * If you're using a command or alias list, use `-listenAndRecognizeWithTimeout:handler:` instead. This will ensure that speech recognition will only last as long as is necessary, thus saving the user's battery life and data plan, ensuring that you get a result back, and providing an better overall experience.
 *
 * @param handler An `ISSpeechRecognitionHandler` block that will be executed on the main thread when speech recognition completes, or when an error occurs.
 * @see listenAndRecognizeWithTimeout:handler:
 * @see finishListenAndStartRecognize
 */
- (void)listenWithHandler:(ISSpeechRecognitionHandler)handler;

/**
 * Starts a timed listen. After a set timeout, the SDK will stop listening for audio and will start to transcribe it.
 *
 * Useful when using command lists to ensure that the user doesn't talk longer than necessary.
 *
 * @param timeout The amount of time, in seconds, for the timed listen to last for.
 * @param handler An `ISSpeechRecognitionHandler` block that will be executed on the main thread when speech recognition completes, or when an error occurs.
 */
- (void)listenAndRecognizeWithTimeout:(NSTimeInterval)timeout handler:(ISSpeechRecognitionHandler)handler;

#endif

/**
 * Cancels an in progress speech recognition action.
 * 
 * If, for some reason, you need to cancel an in progress speech recognition action, call this method. It's also a good idea to provide feedback to the user as to why you cancelled it.
 */
- (void)cancel;

@end