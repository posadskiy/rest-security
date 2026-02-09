FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN apk add --no-cache maven && mvn dependency:go-offline -B || true
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/lib /app/lib
COPY --from=build /app/target/gateway-1.0.0.jar /app/app.jar
ENTRYPOINT ["java", "-cp", "/app/app.jar:/app/lib/*", "gateway.Main"]
