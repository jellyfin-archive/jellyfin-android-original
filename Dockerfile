FROM debian:9
ARG SOURCEDIR=/repo
ENV ANDROID_HOME=/usr/lib/android-sdk

RUN dpkg --add-architecture i386 \
 && echo "deb http://deb.debian.org/debian stretch-backports main" >> /etc/apt/sources.list.d/backports.list \
 && apt-get update \
 && apt-get install -y mmv wget unzip android-sdk libgcc1:i386 libstdc++6:i386 lib32z1 \
 && apt-get install -t stretch-backports -y npm

RUN rm -rf ${ANDROID_HOME}/tools \
 && wget http://dl-ssl.google.com/android/repository/tools_r25.2.3-linux.zip -O tools.zip \
 && unzip tools.zip -d ${ANDROID_HOME}/ \
 && rm -f tools.zip

# Required to accept licenses:
# https://stackoverflow.com/questions/38096225/automatically-accept-all-sdk-licences#comment80496274_38381577
RUN echo -e "\nd56f5187479451eabf01fb78af6dfcb131a6481e" >> "${ANDROID_HOME}/licenses/android-sdk-license" \
 && ${ANDROID_HOME}/tools/bin/sdkmanager "platform-tools" "platforms;android-23" "build-tools;23.0.2" "extras;android;m2repository" "extras;google;m2repository"

WORKDIR ${SOURCEDIR}
COPY . .

RUN mkdir -p /dist/apk

WORKDIR /

VOLUME /dist

COPY docker-build-entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
