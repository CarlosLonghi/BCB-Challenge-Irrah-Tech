# Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Wrapper e pom.xml primeiro: a camada com as dependências fica em cache enquanto o pom.xml não mudar,
# e mudar só o código não baixa tudo de novo
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src/
RUN ./mvnw -B -q package -DskipTests

# Execução: só o JRE e o jar, com um usuário sem privilégios
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=builder /app/target/*.jar app.jar
USER app

# 8080: API. 8081: Actuator (health e métricas), que não deve ser publicada para fora
EXPOSE 8080 8081

# A imagem alpine tem wget (busybox), mas não tem curl
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
    CMD wget -qO- http://localhost:8081/actuator/health > /dev/null || exit 1

# Heap proporcional ao limite de memória do container, em vez do padrão de 25%
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
