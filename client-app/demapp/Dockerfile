FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src src
COPY ../../ApplicationLoadBalancer/application-load-balancer/pom.xml ../../ApplicationLoadBalancer/application-load-balancer/
COPY ../../ApplicationLoadBalancer/autodiscovery-client/pom.xml ../../ApplicationLoadBalancer/autodiscovery-client/
COPY ../../netcloud-core/pom.xml ../../netcloud-core/
COPY ../../netcloud-core/netcloud-commons/pom.xml ../../netcloud-core/netcloud-commons/
COPY ../../netcloud-core/netcloud-core-model/pom.xml ../../netcloud-core/netcloud-core-model/
COPY ../../netcloud-core/netcloud-core-dtos/pom.xml ../../netcloud-core/netcloud-core-dtos/
RUN apk add --no-cache maven
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]