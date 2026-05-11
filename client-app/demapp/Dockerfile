FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/demapp-1.0.jar client-app.jar
EXPOSE 9012
ENTRYPOINT ["java", "-jar", "client-app.jar", "--spring.profiles.active=prod"]