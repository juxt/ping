FROM java:8
MAINTAINER JUXT <info@juxt.pro>

ADD target/ping-service.jar /srv/ping-service.jar
ADD target /target

EXPOSE 3080

CMD ["java", "-jar", "/srv/ping-service.jar"]
