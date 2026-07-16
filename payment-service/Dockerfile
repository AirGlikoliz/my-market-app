FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
COPY my-market-app ./my-market-app
COPY payment-service ./payment-service

RUN ./gradlew :payment-service:build --parallel

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/payment-service/build/libs/payment-service.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
