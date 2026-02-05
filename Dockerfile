# 使用 Maven 建置（跳過測試）
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# 使用 Java 17 執行
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/raid-system-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
