FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/
COPY auth-service/pom.xml auth-service/
COPY url-service/pom.xml url-service/
COPY redirect-service/pom.xml redirect-service/
COPY analytics-service/pom.xml analytics-service/
COPY api-gateway/pom.xml api-gateway/
RUN mvn dependency:go-offline -B -pl api-gateway -am || true
COPY . .
RUN mvn package -pl api-gateway -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/api-gateway/target/api-gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
