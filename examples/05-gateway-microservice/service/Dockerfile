FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY pom.xml .
# Install rest-security from parent build context (multi-stage would need repo)
RUN apk add --no-cache maven
# Expect rest-security artifacts in local .m2 or copy from host
COPY src ./src
ARG REST_SECURITY_VERSION=1.0.0
RUN mvn dependency:resolve -B || true
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/service-1.0.0.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
