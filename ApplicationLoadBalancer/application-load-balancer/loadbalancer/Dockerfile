FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/loadbalancer-1.0.jar application-load-balancer.jar
EXPOSE 9006
ENTRYPOINT ["java", "-jar", "application-load-balancer.jar","--spring.profiles.active=prod"]