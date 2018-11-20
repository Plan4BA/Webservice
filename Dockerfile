FROM library/openjdk:10-jre
ARG WEBSERVICE_VERSION=1.3

COPY target/Webservice-${WEBSERVICE_VERSION}-jar-with-dependencies.jar /app/Webservice.jar
WORKDIR /app

EXPOSE 8080
CMD ["java", "-jar", "/app/Webservice.jar"]