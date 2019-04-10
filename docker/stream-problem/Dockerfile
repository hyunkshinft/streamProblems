FROM openjdk:8u191-jre-alpine

RUN apk update
RUN apk add postgresql-client

ADD streamProblems-1.0.jar /
ADD docker-entrypoint.sh /
RUN chmod u+x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
