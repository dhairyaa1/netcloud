FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/control-plane-1.0.jar control-plane.jar
EXPOSE 9004
ENTRYPOINT ["java", "-jar", "control-plane.jar","--spring.profiles.active=prod"]