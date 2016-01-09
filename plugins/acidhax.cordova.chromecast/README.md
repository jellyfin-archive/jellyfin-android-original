cordova-chromecast
==================

Chromecast in Cordova

##Installation
For now, add the plugin from this repository, we'll publish soon with more progress.

```
cordova plugin add https://github.com/GetVideostream/cordova-chromecast.git
```

If you have NodeJS installed, the dependencies should be automatically copied.

- `http://nodejs.org/`

If not you will need to import the following projects as Library Projects in order for this plugin to work:

- `adt-bundle\sdk\extras\google\google_play_services\libproject\google-play-services_lib`
- `adt-bundle\sdk\extras\android\support\v7\appcompat`
- `adt-bundle\sdk\extras\android\support\v7\mediarouter`

##Usage

This project attempts to implement the official Google Cast SDK for Chrome... in Cordova. We've made a lot of progress in making this possible, check out the offical docs for examples: https://developers.google.com/cast/docs/chrome_sender

When you call `chrome.cast.requestSession()` an ugly popup will be displayed to select a Chromecast. If you're not cool with this - you can call: `chrome.cast.getRouteListElement()` which will return a `<ul>` tag that contains the Chromecasts in a list. All you have to do is style that bad boy and you're off to the races!


##Status

The project is now pretty much feature complete - the only things that probably break will be missing parameters. We haven't done any checking for optional paramaters. When using it, make sure your constructors and function calls have every parameter specified in the API.
