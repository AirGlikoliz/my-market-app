FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
COPY my-market-app ./my-market-app
COPY payment-service ./payment-service
COPY auth-server ./auth-server

RUN ./gradlew :auth-server:build --parallel

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/auth-server/build/libs/auth-server.jar app.jar

EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]
