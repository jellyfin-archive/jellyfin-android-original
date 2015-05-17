# Connect SDK Cordova/PhoneGap Plugin

Connect SDK is an open source framework that unifies device discovery and connectivity by providing one set of methods that work across multiple television platforms and protocols.

For more information, visit our [website](http://www.connectsdk.com/).

* [General information about Connect SDK](http://www.connectsdk.com/discover/)
* [Platform documentation & FAQs](http://www.connectsdk.com/docs/cordova/)
* [API documentation](http://www.connectsdk.com/apis/cordova/)

## Dependencies

These steps assume you have a basic working knowledge of development for Android, iOS and Cordova/PhoneGap. For these steps to work, you will need the following:

- Cordova/PhoneGap 2.x or 3.x
- Xcode
- Xcode command line tools
- Android Developer Tools (Eclipse)
- Android's tools & platform-tools folders in your PATH

If you are only developing for one platform, feel free to ignore the steps & requirements for the irrelevant platform.

## Installation for Cordova/PhoneGap 3.x

#### 1. Create a new Cordova app (optional)

    cordova create sample_app com.example.sample_app SampleApp
    cd sample_app
    cordova platform add ios
    cordova platform add android

#### 2. Add plugin to app

    cordova plugins add https://github.com/ConnectSDK/Connect-SDK-Cordova-Plugin.git#master

Or for a specific branch, such as sdk_1.3:

    cordova plugins add https://github.com/ConnectSDK/Connect-SDK-Cordova-Plugin.git#sdk_1.3

#### 3. Setup projects

Run the following command to create Xcode and Eclipse projects:

    cordova prepare

The projects will be located in platforms/ios and platforms/android respectively.

Next, follow the README.md from the Connect SDK iOS and Android source repositories:

* https://github.com/ConnectSDK/Connect-SDK-iOS
* https://github.com/ConnectSDK/Connect-SDK-Android

## Installation for Cordova/PhoneGap 2.x

#### 1. Create a new app (optional)
    # iOS
    /path/to/cordova-ios/bin/create /path/to/my_new_cordova_project com.example.cordova_project_name
    
    # Android
    /path/to/cordova-android/bin/create /path/to/my_new_cordova_project com.example.cordova_project_name

#### 2. Add plugin files to your project
1. Download the files located at <https://github.com/ConnectSDK/Connect-SDK-Cordova-Plugin>
2. Copy www/ConnectSDK.js to your project's www/js folder
3. Copy the contents of the src folder to your project. For Android, the files should go in src/com/connectsdk/cordova

#### 3. Modify Cordova/PhoneGap config files
    # add to www/index.html
    <script src="js/ConnectSDK.js" type="text/javascript"></script>
	
    # add plugin to www/config.xml

    ## iOS
    <plugins>
        <plugin name="ConnectSDK" value="ConnectSDKCordova" />
    </plugins>

    ## Android
    <plugins>
        <plugin name="ConnectSDK" value="com.connectsdk.cordova.ConnectSDKCordova" />
    </plugins>

#### 4. Setup projects

Next, follow the README.md from the Connect SDK iOS and Android source repositories:

* https://github.com/ConnectSDK/Connect-SDK-iOS
* https://github.com/ConnectSDK/Connect-SDK-Android

## Contact
* Twitter [@ConnectSDK](https://www.twitter.com/ConnectSDK)
* Ask a question with the "tv" tag on [Stack Overflow](http://stackoverflow.com/tags/tv)
* General Inquiries info@connectsdk.com
* Developer Support support@connectsdk.com
* Partnerships partners@connectsdk.com

## Troubleshooting

* If the plugin doesn't load on iOS, check if the *.m files are in the project's
  "Compile Sources" list in "Build Phases".

* When updating or re-adding the plugin, you may need to manually add the *.m files
  as well as libicu.dylib, libz.dylib, and any other missing libraries.

## License

Copyright (c) 2013-2014 LG Electronics.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

> http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
