# timetraveller
Simple web server that just specifies the system time for time travel tests

## About
For a project where we shift containers through time a simple web server was needed to do basic tests.
It just needs to display the "system time" inside the container. So I decided to write a small Java
based web server for this task. For our current needs it's sufficient to get the seconds since UNIX
epoch which is displayed at /epoch. Other formats may follow.

