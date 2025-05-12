# Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copia o wrapper e dá permissão de execução
COPY mvnw .
RUN chmod +x mvnw

# Copia o código
COPY .mvn .mvn
COPY pom.xml .
COPY src src/

# Executa o build
RUN ./mvnw clean package -DskipTests

# Execução
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]