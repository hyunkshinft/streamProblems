FROM openjdk:8u191-jdk-alpine

RUN apk update
RUN apk add postgresql-client maven

ADD docker-entrypoint.sh /
RUN chmod u+x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
