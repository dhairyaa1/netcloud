FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/tenant_resources-1.0.jar tenant_ws.jar
EXPOSE 9020
ENTRYPOINT ["java", "-jar", "tenant_ws.jar"]