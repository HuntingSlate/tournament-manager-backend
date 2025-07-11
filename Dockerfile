FROM eclipse-temurin:17-jdk-focal AS build

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw && sed -i 's/\r$//' mvnw

RUN ./mvnw dependency:go-offline -B
COPY src ./src

RUN ./mvnw clean install -DskipTests

FROM eclipse-temurin:17-jre-focal

WORKDIR /app

COPY --from=build /app/target/tournament-manager-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]