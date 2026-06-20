FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/loadbalancer_ws-1.0.jar loadbalancer_ws.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "loadbalancer_ws.jar","--spring.profiles.active=prod"]