//
//  ISSpeechRecognitionResult.h
//  iSpeechSDK
//
//  Copyright (c) 2012 iSpeech, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

/**
 * This class contains information about a successful recognition.
 */
@interface ISSpeechRecognitionResult : NSObject

/**
 * The transcribed text returned from a recognition.
 */
@property (nonatomic, copy, readonly) NSString *text;

/**
 * How confident the speech recognizer was. Scale from 0.0 to 1.0.
 */
@property (nonatomic, assign, readonly) float confidence;

@end
