FROM library/openjdk:12
ARG WEBSERVICE_VERSION=2.1

COPY target/Webservice-${WEBSERVICE_VERSION}-jar-with-dependencies.jar /app/Webservice.jar
WORKDIR /app

EXPOSE 8080
CMD ["java", "-jar", "/app/Webservice.jar"]