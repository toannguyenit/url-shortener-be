FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/
COPY auth-service/pom.xml auth-service/
COPY url-service/pom.xml url-service/
COPY redirect-service/pom.xml redirect-service/
COPY analytics-service/pom.xml analytics-service/
COPY api-gateway/pom.xml api-gateway/
RUN mvn dependency:go-offline -B -pl redirect-service -am || true
COPY . .
RUN mvn package -pl redirect-service -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/redirect-service/target/redirect-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
