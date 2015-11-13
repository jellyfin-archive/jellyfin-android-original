# Collection Repeat Image

Making performant scrolling lists in hybrid apps is very difficult. There are a few decent attempts to fix this problem. [IonicFramework Collection Repeat](http://ionicframework.com/docs/api/directive/collectionRepeat/) being the best we've used so far, [Including our own attempt](https://github.com/mallzee/angular-ui-table-view).

When you do not control the source of the images for these lists you start to get problems when scaling massive images on the main javascript thread. This can lead to crazy jankyness on older phones and newer if the images are massive.

This plugin offloads the image processing to native land to download the image and scale it to the desired size in a background thread. When everything is ready a base64 string of the image is sent back to javascript land to be injected into the img tag.

It's essentially a wrapper around SDWebImage optimised to work with Ionic's collection repeat

```JavaScript
var image = document.getElementById('scaled-image');
var rect = image.getBoundingClientRect();

var options = {
  data: src,
  index: scope.id,
  quality: 0,
  scale: Math.round(rect.width) + 'x' + Math.round(rect.height),
  downloadOptions: window.CollectionRepeatImageOptions.SDWebImageCacheMemoryOnly | window.CollectionRepeatImageOptions.SDWebImageLowPriority
};

window.CollectionRepeatImage.getImage(options, function (data) {
    image.src = 'data:image/jpeg;base64,' + data;
});
```

## Installation

    cordova plugin add https://github.com/mallzee/cordova-collection-repeat-image-plugin.git

## Supported Platforms

- iOS
- Android (Soon)

## API

- window.CollectionRepeatImage.getImage(options, success, failure)
- window.CollectionRepeatImage.cancel(index)
- window.CollectionRepeatImage.cancelAll()

### options

- url: The url for the specified image
- index: The index associated with this image. Used to cancel jobs when items have gone out of view
- quality: [0 - 1] The compression applied to the image.
- scale: The image magic format "[width]x[height]" See [here](https://github.com/mustangostang/UIImage-ResizeMagick) for more variations
- downloadOptions: Bit mask of options to apply to the download. See [Download Options](https://github.com/mallzee/cordova-collection-repeat-image-plugin#download-options)

### Download Options

#### window.CollectionRepeatImageOptions.SDWebImageRetryFailed

 By default, when a URL fail to be downloaded, the URL is blacklisted so the library won't keep trying. This flag disable this blacklisting.

#### window.CollectionRepeatImageOptions.SDWebImageLowPriority
 
 By default, image downloads are started during UI interactions, this flags disable this feature, leading to delayed download on UIScrollView deceleration for instance.

#### window.CollectionRepeatImageOptions.SDWebImageCacheMemoryOnly
 
This flag disables on-disk caching

#### window.CollectionRepeatImageOptions.SDWebImageProgressiveDownload

 This flag enables progressive download, the image is displayed progressively during download as a browser would do. By default, the image is only displayed once completely downloaded.
 
#### window.CollectionRepeatImageOptions.SDWebImageRefreshCached
   
 Even if the image is cached, respect the HTTP response cache control, and refresh the image from remote location if needed. The disk caching will be handled by NSURLCache instead of SDWebImage leading to slight performance degradation. This option helps deal with images changing behind the same request URL, e.g. Facebook graph api profile pics. If a cached image is refreshed, the completion block is called once with the cached image and again with the final image.

 *Use this flag only if you can't make your URLs static with embedded cache busting parameter.*

#### window.CollectionRepeatImageOptions.SDWebImageContinueInBackground

 In iOS 4+, continue the download of the image if the app goes to background. This is achieved by asking the system for extra time in background to let the request finish. If the background task expires the operation will be cancelled.
     
#### window.CollectionRepeatImageOptions.SDWebImageHandleCookies,
 
 Handles cookies stored in NSHTTPCookieStore by setting NSMutableURLRequest.HTTPShouldHandleCookies = YES;


#### window.CollectionRepeatImageOptions.SDWebImageAllowInvalidSSLCertificates
 
 Enable to allow untrusted SSL certificates. Useful for testing purposes. Use with caution in production.

#### window.CollectionRepeatImageOptions.SDWebImageHighPriority

 By default, image are loaded in the order they were queued. This flag move them to the front of the queue and is loaded immediately instead of waiting for the current queue to be loaded (which could take a while).

#### window.CollectionRepeatImageOptions.SDWebImageDelayPlaceholder

 By default, placeholder images are loaded while the image is loading. This flag will delay the loading of the placeholder image until after the image has finished loading.
     
## Sample Angular Directive

Example of the markup inside of a collection repeat.

```HTML
<div class="product-multi" collection-repeat="product in products track by product.id" collection-item-width="'50%'" collection-item-height="'50%'">
    <mlz-img src="{{product.image}}" id="{{$id}}"></mlz-img>
    <div class="price"{{product.cost}}</div>
</div>
```

Example directive to make use of the plugin.

```JavaScript
angular.directive('mlzImg', ['$animate', function ($animate) {
  return {
    restrict: 'E',
    scope: {
      src: '@',
      id: '@'
    },
    template: '<img />',
    link: function (scope, element) {

      var image = element.children(),
          hasLoader = false,
          hasLoaded = false,
          rect;

      function getDimensions() {
        if (!rect) {
          rect = element[0].getBoundingClientRect();
        }
        return rect;
      }

      if (!hasLoader) {
        image.on('load', function () {
          ionic.requestAnimationFrame(function () {
            $animate.addClass(image[0], 'fade-in-not-out');
          });
          hasLoaded = true;
        }).on('error', function () {
          // TODO: Have some default error image
        });
        hasLoader = true;
      }

      function replaceWithScaledImage(src, rect) {
        if (ionic.Platform.isWebView()) {
          var options = {
            data: src,
            index: scope.id,
            quality: 1,
            options: window.CollectionRepeatImageOptions.SDWebImageCacheMemoryOnly,
            scale: (Math.round(rect.width) * 2) + 'x' + (Math.round(rect.height) * 2)// + '#' // Uncomment to crop and scale
          };

          window.CollectionRepeatImage.getImage(options, function (data) {
            image[0].src = 'data:image/jpeg;base64,' + data;
          }, function (error) {
            // TODO: Place broken image stuff in
            console.log('Resize error', error);
          });
        } else {
          image[0].src = src;
        }
      }

      function loadNewImage(src) {
        ionic.requestAnimationFrame(function () {
          image[0].classList.remove('fade-in-not-out');
          replaceWithScaledImage(src, getDimensions());
        });
      }

      scope.$watch('src', function (src, oldSrc) {
        if (src && rect && src !== oldSrc || !hasLoaded) {
          if (ionic.Platform.isWebView()) {
            window.CollectionRepeatImage.cancel(scope.id);
            loadNewImage(src);
          } else {
            loadNewImage(src);
          }
        }
      });
    }
  };
});
```

Cancel all operations on a scope destroy

```JavaScript
$scope.$on('destroy', function () {
  if(ionic.Platform.isWebView()) {
    cordova.plugins.CollectionRepeatImage.cancelAll()
  }
});
```

## Attribution

- [SDWebImage](https://github.com/rs/SDWebImage)
- [UIImage-ResizeMagick](https://github.com/mustangostang/UIImage-ResizeMagick)
