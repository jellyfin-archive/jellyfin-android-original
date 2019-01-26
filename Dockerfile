FROM debian:9
ARG SOURCE_DIR=/repo
ARG ARTIFACT_DIR=/dist
ARG ANDROID_DIR=/usr/lib/android-sdk
ENV SOURCE_DIR=/repo
ENV ARTIFACT_DIR=/dist
ENV ANDROID_DIR=/usr/lib/android-sdk

RUN dpkg --add-architecture i386 \
 && echo "deb http://deb.debian.org/debian stretch-backports main" >> /etc/apt/sources.list.d/backports.list \
 && apt-get update \
 && apt-get install -y mmv wget unzip android-sdk libgcc1:i386 libstdc++6:i386 lib32z1 \
 && apt-get install -t stretch-backports -y npm

RUN rm -rf ${ANDROID_DIR}/tools \
 && wget http://dl-ssl.google.com/android/repository/tools_r25.2.3-linux.zip -O tools.zip \
 && unzip tools.zip -d ${ANDROID_DIR}/ \
 && rm -f tools.zip

# Required to accept licenses:
# https://stackoverflow.com/questions/38096225/automatically-accept-all-sdk-licences/42125740#42125740
# There will be a bit of delay (approx 30 seconds)
RUN yes | ${ANDROID_DIR}/tools/bin/sdkmanager "platform-tools" "platforms;android-23" "build-tools;23.0.2" "extras;android;m2repository" "extras;google;m2repository"

COPY . ${SOURCE_DIR}/

RUN ln -s ${SOURCE_DIR}/build.sh /build.sh

VOLUME ${ARTIFACT_DIR}/

ENTRYPOINT ["/build.sh"]
