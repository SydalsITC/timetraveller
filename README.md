# timetraveller
Simple web server that just specifies the system time for time travel tests

## About
For a project where we shift containers through time a simple web server was needed to do basic tests.
It just needs to display the "system time" inside the container. So I decided to write a small Java
based web server for this task which displays the time inside the container in different human and
machine readable formats.

The image uses _libfaketime_ to modify the system time inside the container. For testing purposes,
it gets installed right away into the webapp image. _If you'd like to move any app or container
in time in a kubernetes/openshift environment, without modification of the image, then feel free
to contact me._

## License
This project is published under the Apache 2.0 License. See LICENSE file or details.

## Build
The system that was given us contained bo development tools and no root access was given, 
so it wasn't possible to install a jdk. But we had the opportunity to use docker and with that
to build us an image that fitted our needs.

The src folder gets mounted into the container and afterwards contains the compiled class files.
```
docker build -t javabuilder:1.0 -f Dockerfile.builder .
docker run --rm -it -v ./src:/code:Z javabuilder:1.0 javac Time.java
```

Next step was to build the webapp container, based on a recent Ubuntu image and the OpenJDK runtime.
```
docker build -t timetravellerapp:1.0 -f Dockerfile.webapp .
```

## Run
A simple test run shows a webserver on http://localhost:8080/ where the app displays the time inside
the container:
```
docker run --rm -it -p 8080:8080 timetravellerapp:1.0
```

There's a docker-compose.yml, too, for easy start and daemonizing:
```
docker-compose up -d
```

## Contexts
The webapp is intended for developing and debugging purposes. Therefor it displays the time
inside the container as well as some environment variables for libfaketime in different,
easy or machine readable format. There's no fancy, shiny look, just timeless content. :-)

* /about - shows some brief information about the webapp.
* /now - displays the time inside the container in human readable format; basically, it's just the result of _Instant.now()_, e.g. 2025-09-20T04:06:22.035107349Z.
* /epoch - the same, as seconds since 1970-01-01.
* /FAKETIME and /LD_PRELOAD show the content of the respective environment variable.
* /diff/<numSeconds> shows the difference between the given number of seconds since 1970-01-01 as
  long int and the respective value inside the container. Means /diff/0 shows the time inside the
  container in seconds since Unix start.
* Under /time you'll find the internal time in different formats, both as epoch and human readable:
  * /time/text : in plain text, key=value format, like for Unix env=value.
  * /time/json : as json record
  * /time/yaml : as yaml document

