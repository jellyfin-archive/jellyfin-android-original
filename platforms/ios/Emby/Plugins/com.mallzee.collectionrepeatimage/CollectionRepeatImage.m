/********* CDVCollectionRepeatImage.m Cordova Plugin Implementation *******/

#import "CollectionRepeatImage.h"
#import "UIImage+ResizeMagick.h"

@implementation CollectionRepeatImage

// Setup the index and download manager
- (void)pluginInitialize {
    indexes = [NSMutableDictionary new];
    manager = [SDWebImageManager sharedManager];
    SDWebImageManager.sharedManager.delegate = self;
}

// When we get the image. Scale it to the size that we asked for
-(UIImage *)imageManager:(SDWebImageManager *)imageManager transformDownloadedImage:(UIImage *)image withURL:(NSURL *)imageURL
{
    return [image resizedImageByMagick:[options objectForKey:@"scale"]];
}

- (void)getImage:(CDVInvokedUrlCommand*)command
{

    options = [command.arguments objectAtIndex:0];
    NSString *index = [options objectForKey:@"index"];

    // Setup the cache key so it considereds the scale asked for as well as the URL
    __weak __typeof(self) weakSelf = self;
    [manager setCacheKeyFilter:^(NSURL *url) {
        // Crazy referencing to stop seg faults when threaded
        __strong __typeof(self) strongSelf = weakSelf;
        return [NSString stringWithFormat:@"%@-%@", [url absoluteString], [strongSelf->options objectForKey:@"scale"]];
    }];

    // Set off a download job
    NSOperation *job = [manager downloadImageWithURL:[options objectForKey:@"data"] options:[[options objectForKey:@"downloadOptions"] integerValue] progress:^(NSInteger receivedSize, NSInteger expectedSize)
    {
        // TODO: create a callback so we can have a process update in web land
    }
    completed:^(UIImage *image, NSError *error, SDImageCacheType cacheType, BOOL finished, NSURL *imageURL)
    {
        if (image)
        {
        	// If we have an image. Switch it to a base64 image and send it back to web land to be injected
            NSString *base64Image = [UIImageJPEGRepresentation(image, [[options objectForKey:@"quality"] floatValue]) base64EncodedStringWithOptions:NSDataBase64Encoding64CharacterLineLength];

            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:base64Image];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        } else {
            [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        }
    }];

    // If we've specified an index. Record the job incase we need to cancel it
    if (index) {
        [indexes setValue:job forKey:index];
    }
}

// Cancel a job at the specified index
- (void)cancel:(CDVInvokedUrlCommand *)command
{
    // Cancel the job if the index has changed
    NSString *index = [command.arguments objectAtIndex:0];
    if (index) {
        NSOperation *job = [indexes objectForKey:index];
        if (job) {
            [job cancel];
        }
    }
}

// Cancel all the current job. Handy when you're leaving the view and want to stop everything.
- (void)cancelAll:(CDVInvokedUrlCommand *)command
{
    // Cancel all the current jobs
    [manager cancelAll];
}

@end