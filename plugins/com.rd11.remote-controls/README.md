RemoteControls
==========

PhoneGap / Cordova iOS plugin that allows you to use the Remote Controls as well as add metadatas in RemoteControlsInfoCenter

Require
-------
- PhoneGap / Cordova 3.x

## Installation

Add the plugin much like any other:

`cordova plugin add com.rd11.remote-controls`

#### Modify the MainViewController.m with these functions:

```
- (void)viewDidLoad
{
    [super viewDidLoad];
    [[UIApplication sharedApplication] beginReceivingRemoteControlEvents];
    // Do any additional setup after loading the view from its nib.
    [[RemoteControls remoteControls] setWebView:self.webView];
}

- (void)viewDidUnload
{
    [super viewDidUnload];
    // Release any retained subviews of the main view.
    // e.g. self.myOutlet = nil;
    // Turn off remote control event delivery
    [[UIApplication sharedApplication] endReceivingRemoteControlEvents];
}

//add this function
- (void)remoteControlReceivedWithEvent:(UIEvent *)receivedEvent {
       [[RemoteControls remoteControls] receiveRemoteEvent:receivedEvent];
   }
```

## Supported Platforms
- iOS

## Methods
- window.remoteControls.updateMetas

## Events
- "remote-event"

### Example
```javascript
function onDeviceReady() {
  artist = "Daft Punk";
  title = "One More Time";
  album = "Discovery";
  image = "path_within_documents_storage OR url_starting_with_http_or_https";
  duration = my_media.getDuration();
  elapsedTime = my_media.getElapsedTime();

  var params = [artist, title, album, image, duration, elapsedTime];
  window.remoteControls.updateMetas(function(success){
      console.log(success);
  }, function(fail){
      console.log(fail);
  }, params);
}

//listen for the event
document.addEventListener("remote-event", function(event) {
//do something
})

```

## RemoteControls License

The MIT License

Copyright (c) 2013 Seth Hillinger (http://github.com/shi11)

## Metadata License

The MIT License

Copyright (c) 2013 François LASSERRE (http://github.com/choiz)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
