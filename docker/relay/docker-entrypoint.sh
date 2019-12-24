#! /bin/sh

cd /app
mvn clean && mvn package
java -cp traffic-shaping-1.0.jar Relay
