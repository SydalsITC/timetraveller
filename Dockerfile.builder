
FROM  ubuntu:24.04

# update basic image and make sure some tools are installed
RUN apt-get -qq update \
 && apt-get -qq -y install gnupg ca-certificates curl nano

# Install OpenJDK Java 21 SDK
RUN DEBIAN_FRONTEND=noninteractive  apt-get -y install openjdk-21-jdk make

RUN mkdir /code
WORKDIR   /code

