FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
COPY my-market-app ./my-market-app
COPY payment-service ./payment-service
COPY auth-server ./auth-server

RUN ./gradlew :my-market-app:build --parallel

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/my-market-app/build/libs/my-market-app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
