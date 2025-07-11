FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jdk-alpine
COPY --from=build /target/EngrenaBot-*.jar app.jar
EXPOSE 8585
ENTRYPOINT ["java","-jar","/app.jar"]
