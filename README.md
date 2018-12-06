# Webservice
Microservice to provide a REST-API to fetch Campus Dual lectures, view mealplans, ...
## deployment
### requirements
- Docker (Linux Kernel 3.10 oder Hyper-V Virtualization under Windows)[https://docs.docker.com/install/]
- recommendation: docker-compose[https://docs.docker.com/compose/install/]
- running DBService(https://github.com/Plan4BA/DBService)
- running CachingService(https://github.com/Plan4BA/CachingService)
- running LoginService(https://github.com/Plan4BA/LoginService)
### docker run 

``
docker run -d --rm --name webservice -p 8080:8080 -e DBSERVICE_ENDPOINT=http://dbservice:8080 -e CACHINGSERVICE_ENDPOINT=http://cachingservice:8080 -e LOGINSERVICE_ENDPOINT=http://loginservice:8080 webservice 
``
### docker compose

```yaml
version: 3
services:
    webservice:
        container_name: webservice
        ports:
            - '8080:8080'
        environment:
            - 'DBSERVICE_ENDPOINT=http://dbservice:8080'
            - 'CACHINGSERVICE_ENDPOINT=http://cachingservice:8080'
            - 'LOGINSERVICE_ENDPOINT=http://loginservice:8080'
        image: webservice
```
## development
### requirements
- Docker & Docker compose(see deployment)
- Java(OpenJDK > 11)
- Maven 3
### build
build the executable JAR-Files
```bash
mvn clean package
```
building docker container:
```bash
docker build -t webservice .
```
### environment variables
- DBSERVICE_ENDPOINT
    - HTTP-URL from which the DBService is accessible
- CACHINGSERVICE_ENDPOINT
    - HTTP-URL from which the CachingService is accessible
- LOGINSERVICE_ENDPOINT
    - HTTP-URL from which the LoginService is accessible
- CALDAVTOKEN_INTERVAL
    - validity interval for a Caldav-Token in milliseconds
    - default value : 365 days
- REFRESHTOKEN_INTERVAL
    - validity interval for a Refresh-Token in milliseconds
    - default value : 365 days
- SHORTTOKEN_INTERVAL
    - validity interval for a Short-Token(short term token for simple api calls) in milliseconds
    - default value : 10 minutes



