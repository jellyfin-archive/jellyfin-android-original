#!/bin/bash

# reference: https://wiki.videolan.org/VLCKit/#Building_the_framework_for_iOS

echo 'Building VLC for iOS framework'

echo 'Cloning VLC repo...'
rm -fr VLCKit
git clone git://git.videolan.org/vlc-bindings/VLCKit.git -b 2.2.x

echo 'Moving patches into cloned VLC repo...'
cp patches/ios/* VLCKit/MobileVLCKit/patches

cd VLCKit
echo 'Building framework for device...'
./buildMobileVLCKit.sh
echo 'Building framework for simulator...'
./buildMobileVLCKit.sh -s
echo 'Creating embedded framework...'
./buildMobileVLCKit.sh -f

mv build/MobileVLCKit.framework ../lib/ios/MobileVLCKit.framework

echo 'Finished! Find the framework at VLCKit/build/MobileVLCKit.framework'


