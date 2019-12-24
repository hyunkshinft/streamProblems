FROM openjdk:8u191-jdk-alpine

WORKDIR /app
RUN apk update
RUN apk add maven

ADD pom.xml /app
ADD src /app/src/
ADD docker-entrypoint.sh /
RUN chmod u+x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
