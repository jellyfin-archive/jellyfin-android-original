//
//  CollectionRepeatImage.h
//  Mallzee
//
//  Created by Jamie Sutherland on 24/10/2014.
//
//

#ifndef Mallzee_CollectionRepeatImage_h
#define Mallzee_CollectionRepeatImage_h

#import <Cordova/CDV.h>
#import "UIImageView+WebCache.h"

@interface CollectionRepeatImage : CDVPlugin<SDWebImageManagerDelegate> {
    NSDictionary *options;
    NSMutableDictionary *indexes;
    SDWebImageManager *manager;
}

- (void)getImage:(CDVInvokedUrlCommand*)command;
- (void)cancel: (CDVInvokedUrlCommand*)command;
- (void)cancelAll: (CDVInvokedUrlCommand*)command;

@end

#endif
