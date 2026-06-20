FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/netcloud-edge-service-1.0.jar netcloud-edge-service.jar
EXPOSE 9679
ENTRYPOINT ["java", "-jar", "netcloud-edge-service.jar","--spring.profiles.active=prod"]