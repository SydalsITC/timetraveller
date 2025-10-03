#!/bin/bash

IMGTAG=${1:-nightly}

# definitions for images
BUILDRIMG=javabuilder
BUILDRTAG=1.0
BUILDRFIL=Dockerfile.builder

WEBAPPIMG=sydalsitc/timetraveller
WEBAPPTAG=${IMGTAG}
WEBAPPFIL=Dockerfile.webapp

# time or offset for libfaketime
FAKETIME=+7d

# function for echoing information about the next step
sectionText() {
  echo "==== $1 ================"
}

sectionText "1) build image with JDK"
docker build -t $BUILDRIMG:$BUILDRTAG -f $BUILDRFIL .

sectionText "2)compile java code"
docker run --rm -it -v ./src:/code:Z $BUILDRIMG:$BUILDRTAG javac Time.java
RC=$?
if [ $RC -ne 0 ] ; then
  echo "!!!! Error compiling, stopping script."
  exit $RC
fi

sectionText "3) build webapp image"
docker build -t $WEBAPPIMG:$WEBAPPTAG -f $WEBAPPFIL .

sectionText "4) running webapp; test on http://localhost:8080/ ; press Ctrl-C to stop"
docker run  --rm -it \
       -p 8080:8080  \
       -e FAKETIME=$FAKETIME \
       -e LD_PRELOAD=/usr/lib/x86_64-linux-gnu/faketime/libfaketime.so.1 \
       $WEBAPPIMG:$WEBAPPTAG

sectionText "5) Cleaning up"
docker run --rm -it -v ./src:/code:Z $BUILDRIMG:$BUILDRTAG bash -c "rm *.class"

